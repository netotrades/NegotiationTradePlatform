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
import trading.common.AgentRequests;
import trading.common.AgentRequests;
import trading.common.Gui;
import trading.common.NegotiationReport;
import trading.common.Order;
import trading.common.OrderDetails;
import trading.strategy.Calculator;
import trading.strategy.DetectionRegion;
import trading.strategy.Offer;
import trading.strategy.StrategyCall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
	private Queue<AgentRequests> buyerRequestQueue = new LinkedList<AgentRequests>();
 
	protected Gui gui;
	
	//Buyer's historical offers
	private ArrayList<Offer> offerHistory; 
	
	private ArrayList<OrderDetails> orderDetailsArrayList = new ArrayList<OrderDetails>();

	//other parameters	
	
	private int historyPrice;	 //start price of the seller
	
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
	
	//private ArrayList<Double> utilityPriceArray = new ArrayList<Double>();
	//private ArrayList<Double> utilityTimeArray = new ArrayList<Double>();
	
	private double averagePriceUtility = 0.0;
	private double averageTimeUtility = 0.0;
	
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
				//System.out.println(agent.getAgentName()+" : create goal @ Body, number of goals = "+ ios.length);
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
		//System.out.println(agent.getAgentName()+" : 1. make proposal, goal = " + goal.cfp+", proposal = "+ goal.proposal);
		
		//initialize the time
		final long time = getTime();		
		this.currentTime = new Date(this.getTime());
		
		//set the orders in the goal to the order list
		List<Order> orders = getOrders(goal.getCfp());
		
		//System.out.println(agent.getAgentName()+ "no of orders for given name = "+ orders.size());
		
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
		
		//System.out.println("order detailArray size = "+ this.orderDetailsArrayList.size());
		
		//if the order has not entered in to the order list of the buyer
		if(this.getIndexOfOrder(order.getName()) == -1){
			//System.out.println(order.getName() + "Order index is added to "+ this.orderDetailsArrayList.size());
			this.orderDetailsArrayList.add(this.orderDetailsArrayList.size(),new OrderDetails(order));
		}
		
		int orderIndex = this.getIndexOfOrder(order.getName());
		int opponentIndex = -1;
		
		//if the order index is a positive or 0
		if(orderIndex >-1){
			
			//find the opponent index in the opponent array list
			opponentIndex = this.orderDetailsArrayList.get(orderIndex).getOpponentIndexInOpponentDetailArrayList(this.buyerRequestQueue.peek().getAgentName());
		}
		//System.out.println(orderIndex+ " - "+ opponentIndex);		
		
		this.offerHistory = this.orderDetailsArrayList.get(orderIndex).getOfferHistoryOfGivenOpponent(this.buyerRequestQueue.peek().getAgentName());
		//System.out.println(this.buyerRequestQueue.peek().getAgentName()+ ": Size of the offerHistory = "+ this.offerHistory.size());
	
		
		//get the negotiation strategy
		String neg_strategy = order.getNegotiationStrategy();
		
		//initialize the acceptable price
		int acceptable_price = 0;
		Offer generatedOffer = new Offer();
		
		//System.out.println("initial current round = "+ this.currentRound);
		
		//set current round of the agent
		if(orderIndex > -1 && opponentIndex > -1){
			this.currentRound = this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentCurrentRound();
		 }else{
			 this.currentRound = 0;
		 }
		//System.out.println("current round = "+ this.currentRound);
		
		//set the previous offer of the seller
		if(orderIndex > -1 && opponentIndex > -1){
			this.setSellerPreviousOffer(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentPreviousOffer());
			
		}
		
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
				
				generatedOffer = strategy_call.callForStrategy1(currentTime,order.getLimit()*1.0,order.getDeadline(), offerHistory,new Offer(this.historyPrice, this.currentTime, 0),this.sellerPreviousOffer, false, betaValue);
				
				//System.out.println("\nSeller: generated offer = "+generatedOffer.getOfferPrice());
				//System.out.println("int generated offer = "+(int)generatedOffer.getOfferPrice());
				acceptable_price =  (int) generatedOffer.getOfferPrice();
			}

			//test strategy 2- Main strategy
			else if (neg_strategy.equals("strategy-2")) {
				
				//System.out.println("\n+++++++++++++++++++++++++++++++++++\nseller: strategy - 2  @ make proposal \n+++++++++++++++++++++++++++++++++\n ");
				
				//generate the offers using the model strategy
				generatedOffer = strategy_call.callForStrategy2(this.currentTime,this.detectionRegion, order.getStartPrice()*1.0, order.getLimit()* 1.0, order.getDeadline(), offerHistory,currentRound, this.numberOfRows, this.numberOfColumns, this.sellerPreviousOffer, false);
				
				//System.out.println("\nSeller: generated offer = "+generatedOffer.getOfferPrice());
				//System.out.println("int generated offer = "+(int)generatedOffer.getOfferPrice());
				acceptable_price =  (int) generatedOffer.getOfferPrice();
				 
			} 
			
			//Strategy 3
			else if (neg_strategy.equals("strategy-3")) { 
				 
				//System.out.println("\n+++++++++++++++++++++++++++++++++++\nseller: strategy - 3  @ make proposal \n+++++++++++++++++++++++++++++++++\n ");
				
				//generate the offers using the model strategy
				generatedOffer = strategy_call.callForStrategy3(this.currentTime,this.detectionRegion, order.getStartPrice()*1.0, order.getLimit()* 1.0, order.getDeadline(), offerHistory,currentRound, this.numberOfRows, this.numberOfColumns, this.sellerPreviousOffer, false);
				
				//System.out.println("\nSeller: generated offer = "+generatedOffer.getOfferPrice());
				//System.out.println("int generated offer = "+(int)generatedOffer.getOfferPrice());
				acceptable_price =  (int) generatedOffer.getOfferPrice();	
							
			} 			
			
			//System.out.println("Seller: @ round= "+this.currentRound+" , offer = "+ acceptable_price);
			//set the buyer current offer as the previous offer of the buyer
			//this.setSellerPreviousOffer(acceptable_price, this.currentTime, this.currentRound);		
			
			//this.utilityPriceArray.add(generatedOffer.getUtilityPriceValue()); 
			//this.utilityTimeArray.add(generatedOffer.getUtilityTimeValue());
			
			if(orderIndex > -1 && opponentIndex > -1 && this.currentRound > 0){
				
				this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityPriceArray().add(generatedOffer.getUtilityPriceValue());
				this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityTimeArray().add(generatedOffer.getUtilityTimeValue());
				
				/*System.out.println(generatedOffer.getUtilityPriceValue()+" - "+ generatedOffer.getUtilityTimeValue());
				System.out.println(	this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityPriceArray().get(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityPriceArray().size()-1));
				System.out.println(	this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityTimeArray().get(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityTimeArray().size()-1));
				*/
			} 
			
			
			agent.getLogger().info(agent.getAgentName()+" proposed: " + acceptable_price);
			
			// Store proposal data in plan parameters.
			goal.setProposal(acceptable_price);  
			String report = "Made proposal: "+acceptable_price +" For Buyer : " +this.buyerRequestQueue.peek().getAgentName();
			
			//System.out.println(this.agent.getAgentName() +" : 2. report = "+ report);
			
			NegotiationReport nr = new NegotiationReport(order, report, getTime());
			reports.add(nr);
			
			//System.out.println("@Before remove : peek element = "+ this.buyerRequestQueue.peek().getAgentName());
			this.buyerRequestQueue.remove();
			//System.out.println("@After remove : peek element = "+ this.buyerRequestQueue.peek().getAgentName());
			
			//System.out.println("\nSeller: Make proposal = "+ acceptable_price+ " @ round = "+ this.currentRound+"\n");
		}		
	  
	}
	
	
	/**
	 * Initialize the detection region for the buyer.
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
		//System.out.println(agent.getAgentName()+ " @  execute task");

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
		//System.out.println("order detailArray size = "+ this.orderDetailsArrayList.size());
		
		if(this.getIndexOfOrder(order.getName()) == -1){
			//System.out.println(order.getName() + "Order index is added to "+ this.orderDetailsArrayList.size());
			this.orderDetailsArrayList.add(this.orderDetailsArrayList.size(),new OrderDetails(order));
		}
		
		String neg_strategy = order.getNegotiationStrategy();
		
		int orderIndex = this.getIndexOfOrder(order.getName());
		int opponentIndex = -1;
		if(orderIndex > -1 ){
			opponentIndex = this.orderDetailsArrayList.get(orderIndex).getOpponentIndexInOpponentDetailArrayList(this.buyerRequestQueue.peek().getAgentName());
		} 
		
		//set current round of the agent
		if(orderIndex != -1 && opponentIndex != -1){
			this.currentRound = this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentCurrentRound();
		} 
		
		//set seller previous offer
		if(orderIndex > -1 && opponentIndex > -1){
			this.setSellerPreviousOffer(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentPreviousOffer());
			
		}
		// Use most urgent order for preparing proposal.

		if (order != null) {

			//Strategy 1- Strategy suggested by the available system - test strategy
			if (neg_strategy.equals("strategy-1")) {
				//System.out.println("Seller: strategy - 1 @ execute task");
				/*double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				double price_span = order.getLimit() - order.getStartPrice();
				acceptable_price = (int) (price_span * elapsed_time / time_span) + order.getStartPrice();*/
				
				Offer generatedOffer = strategy_call.callForStrategy1(currentTime,order.getLimit()*1.0,order.getDeadline(), offerHistory,new Offer(this.historyPrice, this.currentTime, 0),this.sellerPreviousOffer, false, betaValue);
				
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
				
				
				if(orderIndex > -1 && opponentIndex > -1 && this.currentRound > 0){
					
					this.averagePriceUtility = (new Calculator()).calculateAverageUtility(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityPriceArray());
					this.averageTimeUtility = (new Calculator()).calculateAverageUtility(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityTimeArray());
										
					this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).setAveragePriceUtility(this.averagePriceUtility);
					this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).setAverageTimeUtility(this.averageTimeUtility);
					
					/*System.out.println(generatedOffer.getUtilityPriceValue()+" - "+ generatedOffer.getUtilityTimeValue());
					System.out.println(	this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityPriceArray().get(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityPriceArray().size()-1));
					System.out.println(	this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityTimeArray().get(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentUtilityTimeArray().size()-1));
					*/
				} 
				
				//System.out.println("Seller: average price utility = "+ this.averagePriceUtility );
				//System.out.println("Seller: average time utility = "+ this.averageTimeUtility );
				
				order.setState(Order.DONE);
				order.setExecutionPrice(price);
				order.setExecutionDate(new Date(getTime()));

				String report = "Sold for: " + price+ " For Buyer: "+ this.buyerRequestQueue.peek().getAgentName();
				NegotiationReport nr = new NegotiationReport(order, report, getTime());
				reports.add(nr);
				
				this.buyerRequestQueue.remove();
			}
			else
			{    
				throw new PlanFailureException();
			}
		}
		
		
	}
	
	/**
	 * Get the index of the given order's name in the order detail array list.
	 */
	public int getIndexOfOrder(String orderName){
		int indexAvailable = -1;
		if(this.orderDetailsArrayList.size() >0){
			for(int i=0; i< this.orderDetailsArrayList.size(); i++){
				if(this.orderDetailsArrayList.get(i).getOrder().getName().equals(orderName)){
					indexAvailable = i;
					break;
				}
			}
		}
		return indexAvailable;
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
	public IFuture<Integer> callForProposal(String agentName,String name) {
		
		this.buyerRequestQueue.add(new AgentRequests(agentName, name));
		
		//System.out.println("Entered buyer agent to queue = "+ agentName);
		
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
	 * set the previous offer to the global variable.
	 */
	public void setSellerPreviousOffer(Offer prevOffer){	
		
		this.sellerPreviousOffer.setOfferPrice(prevOffer.getOfferPrice());
		this.sellerPreviousOffer.setOfferTime(prevOffer.getOfferTime());
		this.sellerPreviousOffer.setRoundNumber(prevOffer.getRoundNumber());
		
	} 


	/**
	 *  Buy an item
	 *  @param name	The item name.
	 *  @param price	The price to pay.
	 *  @return A future indicating if the transaction was successful.
	 */
	public IFuture<Void> acceptProposal(String agentName, String name, int price) {
		
		this.buyerRequestQueue.add(new AgentRequests(agentName, name));
		
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
	 * Set Buyer's offer history at the sellerBDI
	 * @param name
	 * @param price
	 */
	public void setacceptablePrice(String agentName, Order order, String name, int price,Offer sellerPrevOffer) {
		//System.out.println("\n "+agentName+ " :this is set acceptable price");
		
		int orderIndex;
		
		if (order.getName().equals(name)) {
			//offerHistory.add(new Offer(price, new Date(), (currentRound-1)));
			
			//find the index of the order
			orderIndex = this.getOrderDetailIndex(order.getName());
			int opponentIndex = -1;
			int agentRoundNo = -1;
			
			if( orderIndex > -1 ){
				opponentIndex = this.orderDetailsArrayList.get(orderIndex).getOpponentIndexInOpponentDetailArrayList(agentName );
			}
			
			if(orderIndex > -1 && opponentIndex > -1){
				agentRoundNo = this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentCurrentRound();
				
			}
			 //in the 0th round
			if(agentRoundNo == -1 ){
				agentRoundNo = 0;
			} 
			 
			
			if(orderIndex > -1){
				//System.out.println(agentName+" agent round no in offer history = "+ agentRoundNo );
				//Insert acceptable offer for the opponent detail array list
			 
				this.orderDetailsArrayList.get(orderIndex).addOfferToOpponentOfferHistory(agentName, new Offer(price, new Date(), agentRoundNo));
				
				//System.out.println(agentName+" opponent index = "+ opponentIndex);
				
				if(opponentIndex == -1){
					opponentIndex = this.orderDetailsArrayList.get(orderIndex).getOpponentIndexInOpponentDetailArrayList(agentName );
				}
				this.currentRound = this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).increaseCurrentRound();
				this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).setAgentPreviousOffer(sellerPrevOffer);
				//System.out.println(agentName+ " agent round no in offer history after increment = "+ this.currentRound );
			 } 	
			 
		} 
		
	}
	
 
	
	/**
	 * Get order detail index for the given order name
	 * @param orderName
	 * @return index
	 */
	public int getOrderDetailIndex(String orderName){
		
		int index = -1;
		
		
		if(this.orderDetailsArrayList.size() > 0){
			
			for(int i=0; i< this.orderDetailsArrayList.size();i++){
				if(this.orderDetailsArrayList.get(i).getOrder().getName().equals(orderName)){
					index = i;
					break;
				}
			}
		} 
		return index;
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
