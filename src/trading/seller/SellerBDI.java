package trading.seller;

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
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

@Agent
@Service
@ProvidedServices(@ProvidedService(type=IBuyItemService.class))
@RequiredServices(@RequiredService(name="clockser", type=IClockService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)))
@Arguments(@Argument(name="initial_orders", clazz=Order[].class))
public class SellerBDI implements IBuyItemService, INegotiationAgent
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
	public class SellItem implements INegotiationGoal
	{
		@GoalParameter
		protected Order order;

		/**
		 *  Create a new SellItem. 
		 */
		public SellItem(Order order)
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
	
	@Goal
	public class MakeProposal
	{
		protected String cfp;
		protected int proposal;
		
		/**
		 *  Create a new MakeProposal. 
		 */
		public MakeProposal(String cfp)
		{
			this.cfp = cfp;
		}

		/**
		 *  Get the cfp.
		 *  @return The cfp.
		 */
		public String getCfp()
		{
			return cfp;
		}

		/**
		 *  Get the proposal.
		 *  @return The proposal.
		 */
		public int getProposal()
		{
			return proposal;
		}

		/**
		 *  Set the proposal.
		 *  @param proposal The proposal to set.
		 */
		public void setProposal(int proposal)
		{
			this.proposal = proposal;
		}
		
	}
	
	@Goal
	public class ExecuteTask
	{
		protected String cfp;
		protected int proposal;
		
		/**
		 *  Create a new ExecuteTask. 
		 */
		public ExecuteTask(String cfp, int proposal)
		{
			super();
			this.cfp = cfp;
			this.proposal = proposal;
		}

		/**
		 *  Get the cfp.
		 *  @return The cfp.
		 */
		public String getCfp()
		{
			return cfp;
		}

		/**
		 *  Get the proposal.
		 *  @return The proposal.
		 */
		public int getProposal()
		{
			return proposal;
		}
	}

	/**
	 * 
	 */
	@Belief(rawevents={@RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED)})
	public List<Order> getOrders()
	{
		List<Order> ret = new ArrayList<Order>();
		Collection<SellItem> goals = agent.getGoals(SellItem.class);
		for(SellItem goal: goals)
		{
			ret.add(goal.getOrder());
		}
		return ret;
	}
	
	/**
	 * 
	 */
	public List<Order> getOrders(String name)
	{
		List<Order> ret = new ArrayList<Order>();
		Collection<SellItem> goals = agent.getGoals(SellItem.class);
		for(SellItem goal: goals)
		{
			if(name==null || name.equals(goal.getOrder().getName()))
			{
				ret.add(goal.getOrder());
			}
		}
		return ret;
	}
	
	@Plan(trigger=@Trigger(goals=MakeProposal.class))
	protected void makeProposal(MakeProposal goal)
	{
		final long time = getTime();
		List<Order> orders = getOrders(goal.getCfp());
		
		if(orders.isEmpty())
			throw new PlanFailureException();
			
		Collections.sort(orders, new Comparator<Order>()
		{
			public int compare(Order o1, Order o2)
			{
				double prio1 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				double prio2 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				return prio1>prio2? 1: prio1<prio2? -1: o1.hashCode()-o2.hashCode();
			}
		});
		Order order = orders.get(0);
		String neg_strategy = order.getNegotiationStrategy();
		int acceptable_price = 0;
		
		// Use most urgent order for preparing proposal.

		if (order != null) {
			if (neg_strategy.equals("strategy-1")) {
				double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				double price_span = order.getLimit() - order.getStartPrice();
				acceptable_price = (int) (price_span * elapsed_time / time_span) + order.getStartPrice();
			}

			else if (neg_strategy.equals("strategy-2")) {
				acceptable_price = historyPrice;
				historyPrice = acceptable_price - 2;
			}
			
			else if (neg_strategy.equals("strategy-3")) {
				acceptable_price = historyPrice;
				historyPrice = acceptable_price - 2;
			}
			
			else if (neg_strategy.equals("strategy-3")) {
				System.out.println("strategy - 3");
				if (currentRound == 0) acceptable_price = order.getStartPrice();
				else if(currentRound < numberOfRounds) 
					strategy_call.callForStrategy(order.getLimit(),order.getDeadline(),offerHistory, numberOfRounds);
				currentRound++;
			}

			agent.getLogger().info(agent.getAgentName()+" proposed: " + acceptable_price);
			
			// Store proposal data in plan parameters.
			goal.setProposal(acceptable_price);
			
			String report = "Made proposal: "+acceptable_price;
			NegotiationReport nr = new NegotiationReport(order, report, getTime());
			reports.add(nr);
		}
	}
	
	@Plan(trigger=@Trigger(goals=ExecuteTask.class))
	protected void executeTask(ExecuteTask goal)
	{
		// Search suitable open orders.
		final long time = getTime();
		List<Order> orders = getOrders(goal.getCfp());
		
		if(orders.isEmpty())
			throw new PlanFailureException();
			
		Collections.sort(orders, new Comparator<Order>()
		{
			public int compare(Order o1, Order o2)
			{
				double prio1 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				double prio2 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				return prio1>prio2? 1: prio1<prio2? -1: o1.hashCode()-o2.hashCode();
			}
		});
		Order order = orders.get(0);
		String neg_strategy = order.getNegotiationStrategy();
		int acceptable_price = 0;
		
		// Use most urgent order for preparing proposal.

		if (order != null) {
			if (neg_strategy.equals("strategy-1")) {
				double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				double price_span = order.getLimit() - order.getStartPrice();
				acceptable_price = (int) (price_span * elapsed_time / time_span) + order.getStartPrice();
			}

			else if (neg_strategy.equals("strategy-2")) {
				acceptable_price = historyPrice;
				historyPrice = acceptable_price - 2;
			}
			
			System.out.println(acceptable_price);
		
			// Extract order data.
			int price = goal.getProposal();
			System.out.println(price);
			
			if (price >= acceptable_price) {
				order.setState(Order.DONE);
				order.setExecutionPrice(price);
				order.setExecutionDate(new Date(getTime()));

				String report = "Sold for: " + price;
				NegotiationReport nr = new NegotiationReport(order, report, getTime());
				reports.add(nr);
			}
			else
			{    
				throw new PlanFailureException();
			}
		}
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
	 *  Ask the seller for a a quote on a item.
	 *  @param name	The item name.
	 *  @return The price.
	 */
	public IFuture<Integer> callForProposal(String name)
	{
		final Future<Integer>	ret	= new Future<Integer>();
		final MakeProposal goal = new MakeProposal(name);
		agent.dispatchTopLevelGoal(goal).addResultListener(new IResultListener<Object>()
		{
			public void resultAvailable(Object result)
			{
				ret.setResult(Integer.valueOf(goal.getProposal()));
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		});
		return ret;
	}

	/**
	 *  Buy an item
	 *  @param name	The item name.
	 *  @param price	The price to pay.
	 *  @return A future indicating if the transaction was successful.
	 */
	public IFuture<Void> acceptProposal(String name, int price)
	{
		final Future<Void>	ret	= new Future<Void>();
		ExecuteTask goal = new ExecuteTask(name, price);
		agent.dispatchTopLevelGoal(goal).addResultListener(new IResultListener<Object>()
		{
			public void resultAvailable(Object result)
			{
				ret.setResult(null);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		});
		return ret;
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
		SellItem goal = new SellItem(order);
		agent.dispatchTopLevelGoal(goal);
	}
	
	/**
	 *  Get all purchase or sell goals.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<INegotiationGoal> getGoals()
	{
		return (Collection)agent.getGoals(SellItem.class); 
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
