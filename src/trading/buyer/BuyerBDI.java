package trading.buyer;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalDropCondition;
import jadex.bdiv3.annotation.GoalParameter;
import jadex.bdiv3.annotation.GoalTargetCondition;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.RawEvent;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.Tuple2;
import jadex.commons.future.CollectionResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IResultListener;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import trading.IBuyItemService;
import trading.INegotiationAgent;
import trading.INegotiationGoal;
import trading.common.Gui;
import trading.common.NegotiationReport;
import trading.common.Order;
import trading.strategy.Offer;
import trading.strategy.StrategyCall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * 
 */
@Agent
@RequiredServices(
{
	@RequiredService(name="buyservice", type=IBuyItemService.class, multiple=true),
	@RequiredService(name="clockser", type=IClockService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM))
})
@Arguments(@Argument(name="initial_orders", clazz=Order[].class))
public class BuyerBDI implements INegotiationAgent
{	
	@Agent
	protected BDIAgent agent;
	
	@Belief
	protected List<NegotiationReport> reports = new ArrayList<NegotiationReport>();
	
	private ArrayList<Offer> offerHistory;
	
	protected Gui gui;
	
	private int historyPrice;
	
	private int currentRound;
	
	private int numberOfRounds;
	
	private StrategyCall strategy_call;
	
	/**
	 *  The agent body.
	 */
	@AgentBody
	public void body()
	{
		Order[] ios = (Order[])agent.getArgument("initial_orders");
		if(ios!=null)
		{
			for(Order o: ios)
			{
				createGoal(o);
			}
		}
		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					gui = new Gui(agent.getExternalAccess());
				}
				catch(ComponentTerminatedException cte)
				{
 				}
			}
		});
	}
	
	/**
	 *  Called when agent terminates.
	 */
	@AgentKilled
	public void shutdown()
	{
		if(gui!=null)
		{
			gui.dispose();
		}
	}
	
	@Goal(recur=true, recurdelay=10000, unique=true)
	public class PurchaseItem implements INegotiationGoal
	{
		@GoalParameter
		protected Order order;

		/**
		 *  Create a new PurchaseItem. 
		 */
		public PurchaseItem(Order order)
		{
			this.order = order;
		}

		/**
		 *  Get the order.
		 *  @return The order.
		 */
		public Order getOrder()
		{
			return order;
		}
		
		@GoalDropCondition(parameters="order")
		public boolean checkDrop()
		{
			return order.getState().equals(Order.FAILED);
		}
		
		@GoalTargetCondition(parameters="order")
		public boolean checkTarget()
		{
			return Order.DONE.equals(order.getState());
		}
	}
	
	/**
	 * 
	 */
	@Belief(rawevents={@RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED), 
		@RawEvent(ChangeEvent.PARAMETERCHANGED)})
	public List<Order> getOrders()
	{
//		System.out.println("getOrders belief called");
		List<Order> ret = new ArrayList<Order>();
		Collection<PurchaseItem> goals = agent.getGoals(PurchaseItem.class);
		for(PurchaseItem goal: goals)
		{
			ret.add(goal.getOrder());
		}
		return ret;
	}
	
	/**
	 *  Get the current time.
	 */
	protected long getTime()
	{
		IClockService cs = (IClockService)agent.getServiceContainer().getRequiredService("clockser").get();
		return cs.getTime();
	}
	
	/**
	 * 
	 */
	@Plan(trigger=@Trigger(goals=PurchaseItem.class))
	protected void purchaseItem(PurchaseItem goal)
	{
		int acceptable_price = 0;
		Order order = goal.getOrder();
		String neg_strategy = order.getNegotiationStrategy();

		//Strategy 1- Strategy suggested by the available system - test strategy
		if (neg_strategy.equals("strategy-1")) {
			System.out.println("Buyer: strategy - 1");
			double time_span = order.getDeadline().getTime() - order.getStartTime();
			double elapsed_time = getTime() - order.getStartTime();
			double price_span = order.getLimit() - order.getStartPrice();
			acceptable_price = (int) (price_span * elapsed_time / time_span) + order.getStartPrice();
		}
		
		//test strategy 2
		else if (neg_strategy.equals("strategy-2")) {
			System.out.println("Buyer: strategy - 2");
			acceptable_price = historyPrice;
			historyPrice = acceptable_price + 3;
		}
		
		//Strategy 3- Main strategy
		else if (neg_strategy.equals("strategy-3")) {
			System.out.println("Buyer: Strategy - 3");
			if (currentRound == 0) 
			{
				acceptable_price = order.getStartPrice();
			}
			else if(currentRound < numberOfRounds) 
				strategy_call.callForStrategy(order.getLimit(),order.getDeadline(),offerHistory, numberOfRounds);
			currentRound++;
		}
		
		System.out.println("Buyer..." + acceptable_price);

		// Find available seller agents.
		IBuyItemService[]	services = agent.getServiceContainer().getRequiredServices("buyservice").get().toArray(new IBuyItemService[0]);
		if(services.length == 0)
		{
            //System.out.println("No seller found, purchase failed.");
			generateNegotiationReport(order, null, acceptable_price);
			throw new PlanFailureException();
		}

		// Initiate a call-for-proposal.
		Future<Collection<Tuple2<IBuyItemService, Integer>>>	cfp	= new Future<Collection<Tuple2<IBuyItemService, Integer>>>();
		final CollectionResultListener<Tuple2<IBuyItemService, Integer>>	crl	= new CollectionResultListener<Tuple2<IBuyItemService, Integer>>(services.length, true,
			new DelegationResultListener<Collection<Tuple2<IBuyItemService, Integer>>>(cfp));
		System.out.println(services.length);
		for(int i=0; i<services.length; i++)
		{
			final IBuyItemService	seller	= services[i];
			seller.callForProposal(order.getName()).addResultListener(new IResultListener<Integer>()
			{
				public void resultAvailable(Integer result)
				{
					crl.resultAvailable(new Tuple2<IBuyItemService, Integer>(seller, result));
					System.out.println("Seller..." +result);
					offerHistory.add(new Offer(result,new Date(),currentRound));
				}
				
				public void exceptionOccurred(Exception exception)
				{
					crl.exceptionOccurred(exception);
				}
			});
		}
		// Sort results by price.
		@SuppressWarnings("unchecked")
		Tuple2<IBuyItemService, Integer>[]	proposals	= cfp.get().toArray(new Tuple2[0]);
		Arrays.sort(proposals, new Comparator<Tuple2<IBuyItemService, Integer>>()
		{
			public int compare(Tuple2<IBuyItemService, Integer> o1, Tuple2<IBuyItemService, Integer> o2)
			{
				return o1.getSecondEntity().compareTo(o2.getSecondEntity());
			}
		});

		// Do we have a winner?
		if(proposals.length>0 && proposals[0].getSecondEntity().intValue()<=acceptable_price)
		{
			proposals[0].getFirstEntity().acceptProposal(order.getName(), proposals[0].getSecondEntity().intValue()).get();
			
			generateNegotiationReport(order, proposals, acceptable_price);
			
			// If contract-net succeeds, store result in order object.
			order.setState(Order.DONE);
			order.setExecutionPrice(proposals[0].getSecondEntity());
			order.setExecutionDate(new Date(getTime()));
		}
		else
		{
			generateNegotiationReport(order, proposals, acceptable_price);
			
			throw new PlanFailureException();
		}
		//System.out.println("result: "+cnp.getParameter("result").getValue());
	}
	
	/**
	*  Generate and add a negotiation report.
	*/
	protected void generateNegotiationReport(Order order, Tuple2<IBuyItemService, Integer>[] proposals, double acceptable_price)
	{
		String report = "Accepable price: "+acceptable_price+", proposals: ";
		if(proposals!=null)
		{
			for(int i=0; i<proposals.length; i++)
			{
				report += proposals[i].getSecondEntity()+"-"+proposals[i].getFirstEntity().toString();
				if(i+1<proposals.length)
					report += ", ";
			}
		}
		else
		{
			report	+= "No seller found, purchase failed.";
		}
		NegotiationReport nr = new NegotiationReport(order, report, getTime());
		//System.out.println("REPORT of agent: "+getAgentName()+" "+report);
		reports.add(nr);
	}

	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public BDIAgent getAgent()
	{
		return agent;
	}
	
	/**
	 *  Create a purchase or sell oder.
	 */
	public void createGoal(Order order)
	{
		System.out.println("create Goal");
		historyPrice = order.getStartPrice();
		
		offerHistory = new ArrayList<Offer>();
		strategy_call = new StrategyCall();
		currentRound = 0;
	    numberOfRounds = 15;
		PurchaseItem goal = new PurchaseItem(order);
		agent.dispatchTopLevelGoal(goal);
	}
	
	/**
	 *  Get all purchase or sell goals.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<INegotiationGoal> getGoals()
	{
		return (Collection)agent.getGoals(PurchaseItem.class);
	}
	
	/**
	 *  Get all reports.
	 */
	public List<NegotiationReport> getReports(Order order)
	{
		List<NegotiationReport> ret = new ArrayList<NegotiationReport>();
		for(NegotiationReport rep: reports)
		{
			if(rep.getOrder().equals(order))
			{
				ret.add(rep);
			}
		}
		return ret;
	}  
}




