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
import trading.strategy.DetectionRegion;
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
@ProvidedServices(@ProvidedService(type = IBuyItemService.class))
@RequiredServices(@RequiredService(name = "clockser", type = IClockService.class, binding = @Binding(scope = RequiredServiceInfo.SCOPE_PLATFORM) ))
@Arguments(@Argument(name = "initial_orders", clazz = Order[].class))
public class SellerBDI implements IBuyItemService, INegotiationAgent {
	@Agent
	protected BDIAgent agent;
	
	@Belief
	protected List<NegotiationReport> reports = new ArrayList<NegotiationReport>();

 
	protected Gui gui;
	
	//Buyer's historical offers
	private ArrayList<Offer> offerHistory;

	//other parameters	
	private int historyPrice;	
	private int currentRound;
	private Offer sellerPreviousOffer;
	
	private StrategyCall strategy_call = new StrategyCall();
	private DetectionRegion detectionRegion;
	private Date currentTime;
	
	//initialize the detection regions division using rows and columns
	private final int numberOfRows = 100;
	private final int numberOfColumns = 100;
	
	//initialize the default max number of rounds to 15
	//private final int numberOfRounds = 5; 
	
	private final double betaValue = 0.8;
	
	
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
				} catch (ComponentTerminatedException cte) {
				}				 
			}
		});
	}

	/**
	 * Called when agent terminates.
	 */
	@AgentKilled
	public void shutdown() {
		if (gui != null) {
			gui.dispose();
		}
	}

	@Goal(recur = true, recurdelay = 10000, unique = true)
	public class SellItem implements INegotiationGoal {
		@GoalParameter
		protected Order order;

		/**
		 * Create a new SellItem.
		 */
		public SellItem(Order order) {
			this.order = order;
		}

		/**
		 * Get the order.
		 * 
		 * @return The order.
		 */
		public Order getOrder() {
			return order;
		}

		@GoalDropCondition(parameters = "order")
		public boolean checkDrop() {
			return order.getState().equals(Order.FAILED);
		}

		@GoalTargetCondition(parameters = "order")
		public boolean checkTarget() {
			return Order.DONE.equals(order.getState());
		}
	}

	@Goal
	public class MakeProposal {
		protected String cfp;
		protected int proposal;

		/**
		 * Create a new MakeProposal.
		 */
		public MakeProposal(String cfp) {
			this.cfp = cfp;
		}

		/**
		 * Get the cfp.
		 * 
		 * @return The cfp.
		 */
		public String getCfp() {
			return cfp;
		}

		/**
		 * Get the proposal.
		 * 
		 * @return The proposal.
		 */
		public int getProposal() {
			return proposal;
		}

		/**
		 * Set the proposal.
		 * 
		 * @param proposal
		 *            The proposal to set.
		 */
		public void setProposal(int proposal) {
			this.proposal = proposal;
		}

	}

	@Goal
	public class ExecuteTask {
		protected String cfp;
		protected int proposal;

		/**
		 * Create a new ExecuteTask.
		 */
		public ExecuteTask(String cfp, int proposal) {
			super();
			this.cfp = cfp;
			this.proposal = proposal;
		}

		/**
		 * Get the cfp.
		 * 
		 * @return The cfp.
		 */
		public String getCfp() {
			return cfp;
		}

		/**
		 * Get the proposal.
		 * 
		 * @return The proposal.
		 */
		public int getProposal() {
			return proposal;
		}
	}

	/**
	 *  get order list
	 */
	@Belief(rawevents = { @RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED) })
	public List<Order> getOrders() {
		List<Order> ret = new ArrayList<Order>();
		Collection<SellItem> goals = agent.getGoals(SellItem.class);
		for (SellItem goal : goals) {
			ret.add(goal.getOrder());
		}
		return ret;
	}

	/**
	 * get orders list by the given name
	 */
	public List<Order> getOrders(String name) {
		List<Order> ret = new ArrayList<Order>();
		Collection<SellItem> goals = agent.getGoals(SellItem.class);
		for (SellItem goal : goals) {
			if (name == null || name.equals(goal.getOrder().getName())) {
				ret.add(goal.getOrder());
			}
		}
		return ret;
	}
	
	/**
	 * Make proposals for the buyer
	 */
	@Plan(trigger=@Trigger(goals=MakeProposal.class))
	protected void makeProposal(MakeProposal goal)
	{
		
		//initialize the time
		final long time = getTime();		
		this.currentTime = new Date(this.getTime());
		
		//set the orders in the goal to the order list
		List<Order> orders = getOrders(goal.getCfp());
		
		//if order is empty then throw an exception
		if(orders.isEmpty()){
			throw new PlanFailureException();
		}
			
		//sort the orders according to the time
		Collections.sort(orders, new Comparator<Order>()
		{
			//method to compare orders
			public int compare(Order o1, Order o2)
			{
				double prio1 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				double prio2 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				return prio1>prio2? 1: prio1<prio2? -1: o1.hashCode()-o2.hashCode();
			}
		});
		
		//get the 1st order as the order
		Order order = orders.get(0);
		
		//get the negotiation strategy
		String neg_strategy = order.getNegotiationStrategy();
		
		//initialize the acceptable price
		int acceptable_price = 0; 
		
		// Use most urgent order for preparing proposal.

		//if an order is available
		if (order != null) {
			
			//Strategy 1- Before Learning strategy
			if (neg_strategy.equals("strategy-1")) {
				
				//System.out.println("\nSeller: strategy - 1 @ make proposal");
				
				/*double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				double price_span = order.getLimit() - order.getStartPrice();
				acceptable_price = (int) (price_span * elapsed_time / time_span) + order.getStartPrice();*/
				
				Offer generatedOffer = strategy_call.callForStrategy1(currentTime,order.getLimit()*1.0,order.getDeadline(), offerHistory,this.sellerPreviousOffer, false, betaValue);
				
				//System.out.println("\nSeller: generated offer = "+generatedOffer.getOfferPrice());
				//System.out.println("int generated offer = "+(int)generatedOffer.getOfferPrice());
				acceptable_price =  (int) generatedOffer.getOfferPrice();
			}

			//test strategy 2
			else if (neg_strategy.equals("strategy-2")) {
				
				//System.out.println("\n+++++++++++++++++++++++++++++++++++\nseller: strategy - 2  @ make proposal \n+++++++++++++++++++++++++++++++++\n ");
				
				//generate the offers using the model strategy
				Offer generatedOffer = strategy_call.callForStrategy2(this.currentTime,this.detectionRegion, order.getStartPrice()*1.0, order.getLimit()* 1.0, order.getDeadline(), offerHistory,currentRound, this.numberOfRows, this.numberOfColumns, this.sellerPreviousOffer, false);
				
				//System.out.println("\nSeller: generated offer = "+generatedOffer.getOfferPrice());
				//System.out.println("int generated offer = "+(int)generatedOffer.getOfferPrice());
				acceptable_price =  (int) generatedOffer.getOfferPrice();
				 
			} 
			
			//Strategy 3- Main strategy
			else if (neg_strategy.equals("strategy-3")) { 
				 
				//System.out.println("\n+++++++++++++++++++++++++++++++++++\nseller: strategy - 3  @ make proposal \n+++++++++++++++++++++++++++++++++\n ");
				
				//generate the offers using the model strategy
				Offer generatedOffer = strategy_call.callForStrategy3(this.currentTime,this.detectionRegion, order.getStartPrice()*1.0, order.getLimit()* 1.0, order.getDeadline(), offerHistory,currentRound, this.numberOfRows, this.numberOfColumns, this.sellerPreviousOffer, false);
				
				//System.out.println("\nSeller: generated offer = "+generatedOffer.getOfferPrice());
				//System.out.println("int generated offer = "+(int)generatedOffer.getOfferPrice());
				acceptable_price =  (int) generatedOffer.getOfferPrice();	
							
			} 			
			
			//System.out.println("Seller: @ round= "+this.currentRound+" , offer = "+ acceptable_price);
			//set the buyer current offer as the previous offer of the buyer
			this.setSellerPreviousOffer(acceptable_price, this.currentTime, this.currentRound);	
			 
			agent.getLogger().info(agent.getAgentName()+" proposed: " + acceptable_price);
			
			// Store proposal data in plan parameters.
			goal.setProposal(acceptable_price);  
			String report = "Made proposal: "+acceptable_price;
			NegotiationReport nr = new NegotiationReport(order, report, getTime());
			reports.add(nr);
			
			//System.out.println("\nSeller: Make proposal = "+ acceptable_price+ " @ round = "+ this.currentRound+"\n");
		}
		this.currentRound++;
		
	}
	
	
	/**
	 * Initialize the detection region of the buyer.
	 */
	public void setDetectionRegion(Order order, Date currentTime){
		
		// Initialize the detection region of the buyer
		double detRegLowerPriceBoundary = order.getLimit() -((order.getStartPrice() - order.getLimit())* 0.5);
		//double detRegLowerPriceBoundary = order.getStartPrice();
		
		//System.out.println("Seller: Lower price = "+ detRegLowerPriceBoundary);
		
		double detRegUpperPriceBoundary = (order.getStartPrice() + order.getLimit())* 0.5;
		//double detRegUpperPriceBoundary = order.getLimit();
		//System.out.println("Seller: Upper price = "+ detRegUpperPriceBoundary); 
		
		Date detRegLowerTimeBoundary = new Date((order.getDeadline().getTime() + currentTime.getTime())/2);
		//Date detRegLowerTimeBoundary = new Date(currentTime.getTime());
		
		//System.out.println("Seller: Lower time = "+ detRegLowerTimeBoundary);
		
		Date detRegUpperTimeBoundary = new Date(order.getDeadline().getTime() + ((order.getDeadline().getTime()-currentTime.getTime())*1/2));
		//System.out.println("Seller: Upper time = "+ detRegUpperTimeBoundary);
		
		// Initialize the detection region
		this.detectionRegion = new DetectionRegion(detRegLowerPriceBoundary, detRegUpperPriceBoundary, detRegLowerTimeBoundary, detRegUpperTimeBoundary, numberOfRows, numberOfColumns);
	}
	
	/**
	 * Execute the task method.
	 */
	
	@Plan(trigger=@Trigger(goals=ExecuteTask.class))
	protected void executeTask(ExecuteTask goal)
	{

		int acceptable_price = 0;  
		
		// Search suitable open orders.
		final long time = this.getTime();
		this.currentTime = new Date(this.getTime());
		List<Order> orders = getOrders(goal.getCfp());		
		
		if(orders.isEmpty())
		{
			throw new PlanFailureException();
		}
			
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

		// Use most urgent order for preparing proposal.

		if (order != null) {

			//Strategy 1- Strategy suggested by the available system - test strategy
			if (neg_strategy.equals("strategy-1")) {
				//System.out.println("Seller: strategy - 1 @ execute task");
				/*double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				double price_span = order.getLimit() - order.getStartPrice();
				acceptable_price = (int) (price_span * elapsed_time / time_span) + order.getStartPrice();*/
				
				Offer generatedOffer = strategy_call.callForStrategy1(currentTime,order.getLimit()*1.0,order.getDeadline(), offerHistory,this.sellerPreviousOffer, false, betaValue);
				
				//System.out.println("\nSeller: generated offer = "+generatedOffer.getOfferPrice());
				//System.out.println("int generated offer = "+(int)generatedOffer.getOfferPrice());
				acceptable_price =  (int) generatedOffer.getOfferPrice();
			}
			
			//test strategy 2
			else if (neg_strategy.equals("strategy-2")) {
				//System.out.println("seller: strategy - 2\n----------------------------");
				
 				//generate the offers using the model strategy
				Offer generatedOffer = strategy_call.callForStrategy2(this.currentTime,this.detectionRegion, order.getStartPrice()*1.0, order.getLimit()* 1.0, order.getDeadline(), offerHistory,currentRound, this.numberOfRows, this.numberOfColumns, this.sellerPreviousOffer, false);
				
				//System.out.println("generated offer = "+generatedOffer.getOfferPrice());
	  			 
				acceptable_price =  (int)generatedOffer.getOfferPrice();			
				// System.out.println(acceptable_price);
			}

			
			//Strategy 3- Main strategy
			else if (neg_strategy.equals("strategy-3")) { 
				
				//System.out.println("seller: strategy - 3\n----------------------------");
				
 				//generate the offers using the model strategy
				Offer generatedOffer = strategy_call.callForStrategy3(this.currentTime,this.detectionRegion, order.getStartPrice()*1.0, order.getLimit()* 1.0, order.getDeadline(), offerHistory,currentRound, this.numberOfRows, this.numberOfColumns, this.sellerPreviousOffer, false);
				
				//System.out.println("generated offer = "+generatedOffer.getOfferPrice());
	  			 
				acceptable_price =  (int)generatedOffer.getOfferPrice();			
				// System.out.println(acceptable_price); 
				 
			}
			 	
			//System.out.println("S: acceptable price= "+ acceptable_price);		


			//set the buyer current offer as the previous offer of the buyer
			//this.setSellerPreviousOffer(acceptable_price, this.currentTime, currentRound);
		
			// Extract order data.
			int price = goal.getProposal();
			//System.out.println("S: goal porposal= "+price);
			
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
	 * Get the current time.
	 */
	protected long getTime() {
		IClockService cs = (IClockService) agent.getServiceContainer().getRequiredService("clockser").get();
		return cs.getTime();
	}

	/**
	 * Ask the seller for a a quote on a item.
	 * 
	 * @param name
	 *            The item name.
	 * @return The price.
	 */
	public IFuture<Integer> callForProposal(String name) {
		final Future<Integer> ret = new Future<Integer>();
		final MakeProposal goal = new MakeProposal(name);
		agent.dispatchTopLevelGoal(goal).addResultListener(new IResultListener<Object>() {
			public void resultAvailable(Object result) {
				ret.setResult(Integer.valueOf(goal.getProposal()));
			}

			public void exceptionOccurred(Exception exception) {
				ret.setException(exception);
			}
		});
		return ret;
	}
	
	
	/**
	 * set the sellers current offer to the previous offer.
	 */
	public void setSellerPreviousOffer(double price, Date time, int round){		
		this.sellerPreviousOffer.setOfferPrice(price);
		this.sellerPreviousOffer.setOfferTime(time);
		this.sellerPreviousOffer.setRoundNumber(round);
	} 


	/**
	 *  Buy an item
	 *  @param name	The item name.
	 *  @param price	The price to pay.
	 *  @return A future indicating if the transaction was successful.
	 */
	public IFuture<Void> acceptProposal(String name, int price) {
		final Future<Void> ret = new Future<Void>();
		ExecuteTask goal = new ExecuteTask(name, price);
		agent.dispatchTopLevelGoal(goal).addResultListener(new IResultListener<Object>() {
			public void resultAvailable(Object result) {
				ret.setResult(null);
			}

			public void exceptionOccurred(Exception exception) {
				ret.setException(exception);
			}
		});
		return ret;
	}

	/**
	 * Set seller's offer history
	 * @param name
	 * @param price
	 */
	public void setacceptablePrice(Order order, String name, int price) {
		if (order.getName().equals(name)) {
			offerHistory.add(new Offer(price, new Date(), (currentRound-1)));
		}
		for (int i = 0; i < offerHistory.size(); i++) {
			//System.out.println("Offer from buyer" + offerHistory.get(i).getOfferPrice());
		}
	}

	/**
	 * Get the agent.
	 * 
	 * @return The agent.
	 */
	public BDIAgent getAgent() {
		return agent;
	}

	/**
	 *  Create a purchase or sell oder.
	 */
	public void createGoal(Order order)
	{ 		
		//main process of creating a goal
		SellItem goal = new SellItem(order);
		agent.dispatchTopLevelGoal(goal);
		

		this.currentTime = new Date(this.getTime());
		this.historyPrice = order.getStartPrice();
		this.sellerPreviousOffer = new Offer( historyPrice,this.currentTime, 0);
		this.offerHistory = new ArrayList<Offer>();
		this.strategy_call = new StrategyCall();
		this.currentRound = 0;
		
		//initialize the detection region of the buyer
		this.setDetectionRegion(order, this.currentTime);
		
		//System.out.println("Seller: create Goal  @ round: "+ (this.currentRound));
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
