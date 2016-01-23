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
import trading.common.AgentRequests; 
import trading.common.ExcelWriter;
import trading.common.Gui;
import trading.common.NegotiationReport;
import trading.common.Order; 
import trading.common.OrderDetails;
import trading.strategy.Calculator;
import trading.strategy.DetectionRegion;
import trading.strategy.Offer;
import trading.strategy.StrategyCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.SwingUtilities;

/**
 * 
 */
@Agent
@RequiredServices({ @RequiredService(name = "buyservice", type = IBuyItemService.class, multiple = true),
		@RequiredService(name = "clockser", type = IClockService.class, binding = @Binding(scope = RequiredServiceInfo.SCOPE_PLATFORM) ) })
@Arguments(@Argument(name = "initial_orders", clazz = Order[].class))
public class BuyerBDI implements INegotiationAgent {
	@Agent
	protected BDIAgent agent;
	
	@Belief
	protected List<NegotiationReport> reports = new ArrayList<NegotiationReport>();	
	private Queue<AgentRequests> sellerRequestQueue = new LinkedList<AgentRequests>();
	private boolean isDone = false;
	
	protected Gui gui;
	
	//seller's historical offers
	private ArrayList<Offer> offerHistory;
	
	private ArrayList<OrderDetails> orderDetailsArrayList = new ArrayList<OrderDetails>();
	
	//other parameters
	private int historyPrice;	
	private int currentRound;
	private Offer buyerPreviousOffer;
	private StrategyCall strategy_call = new StrategyCall();
	private ExcelWriter ew = new ExcelWriter();
	private DetectionRegion detectionRegion;
	private Date currentTime;
	
	//initialize the detection regions division using rows and columns
	private final int numberOfRows = 100;
	private final int numberOfColumns = 100;
	
	//initialize the default max number of rounds to 15
	//private final int numberOfRounds = 5;
	private final double betaValue = 0.8;
	
	private ArrayList<Double> utilityPriceArray = new ArrayList<Double>();
	private ArrayList<Double> utilityTimeArray = new ArrayList<Double>();
	private double averagePriceUtility = 0.0;
	private double averageTimeUtility = 0.0;
	private ArrayList<Offer> tempArray = new ArrayList<Offer>();
	
	/**
	 *  The agent body.
	 */
	@AgentBody
	public void body() {
		Order[] ios = (Order[]) agent.getArgument("initial_orders");
		if (ios != null) {
			for (Order o : ios) {
				//System.out.println(agent.getAgentName()+" : create goal @ Body, number of goals = "+ ios.length);
				createGoal(o);
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					gui = new Gui(agent.getExternalAccess());
				} 
				catch(ComponentTerminatedException cte)
				{
					System.out.println("There is an error in generating the gui!");
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
	
	/**
	 *  Create a class of PurchaseItem. 
	 */
	@Goal(recur=true, recurdelay=10000, unique=true)
	public class PurchaseItem implements INegotiationGoal
	{
		@GoalParameter
		protected Order order;
		protected double acceptedPrice; 

		/**
		 * Constructor
		 *  Create a new PurchaseItem. 
		 */
		public PurchaseItem(Order order) {
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

	/**
	 * Get the order list
	 */
	@Belief(rawevents = { @RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED),
			@RawEvent(ChangeEvent.PARAMETERCHANGED) })
	public List<Order> getOrders() {
		
		// System.out.println("getOrders belief called");
		List<Order> ret = new ArrayList<Order>();
		Collection<PurchaseItem> goals = agent.getGoals(PurchaseItem.class);
		
		for (PurchaseItem goal : goals) {
			ret.add(goal.getOrder());
		}
		return ret;
	}
	
	/**
	 * get orders list by the given name
	 */
	@Belief(rawevents = { @RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED),
			@RawEvent(ChangeEvent.PARAMETERCHANGED) })
	public List<Order> getOrders(String name) {
		
		List<Order> ret = new ArrayList<Order>();
		Collection<PurchaseItem> goals = agent.getGoals(PurchaseItem.class);
		//System.out.println(" create collection of purchase item");
		for (PurchaseItem goal : goals) {
			
			if (name == null || name.equals(goal.getOrder().getName())) {
				//System.out.println("goal name = "+ goal.getOrder().getName());
				ret.add(goal.getOrder());
			}
			
		}
		return ret;
	}

	/**
	 * Get the current time.
	 */
	protected long getTime() {
		IClockService cs = (IClockService) agent.getServiceContainer().getRequiredService("clockser").get();
		return cs.getTime();
	}

	/**
	 * create counter offers for the seller's call for proposals
	 */
	@Plan(trigger = @Trigger(goals = PurchaseItem.class) )
	protected void purchaseItem(PurchaseItem goal) {
		
		//System.out.println(agent.getAgentName()+ " @  Start - purchase item");	
		
		//initiate acceptable price
		int acceptable_price = 0;
				
		//initialize the time
		final long time = getTime(); 
				
		//set the orders in the goal to the order list
		List<Order> orders = this.getOrders(goal.getOrder().getName());
		
		//System.out.println("orders list size =  "+ orders.size());
		//System.out.println(agent.getAgentName()+ "no of orders for given name = "+ orders.size());
				
		//if order is empty then throw an exception
		if(orders.isEmpty()){
			throw new PlanFailureException();
		}
		
		if(orders.size()>1){
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
				
		}			
			
		Order order = orders.get(0);
				
		//Order order = goal.getOrder();
		//System.out.println(orders.get(0).getName() +"- "+ order.getName());				
		
		String neg_strategy = order.getNegotiationStrategy();
		
		this.currentTime = new Date( this.getTime());

		// Find available seller agents.
		IBuyItemService[]	services = agent.getServiceContainer().getRequiredServices("buyservice").get().toArray(new IBuyItemService[0]);
					 
		if(services.length == 0)
		{
			//System.out.println("No seller found, purchase failed.");
			generateNegotiationReport(order, null, acceptable_price);
					
			throw new PlanFailureException();
		}
		
		//System.out.println("Befor call for proposal method");
						 
		// Initiate a call-for-proposal.
		Future<Collection<Tuple2<IBuyItemService, Integer>>>	cfp	= new Future<Collection<Tuple2<IBuyItemService, Integer>>>();
		final CollectionResultListener<Tuple2<IBuyItemService, Integer>>	crl	= new CollectionResultListener<Tuple2<IBuyItemService, Integer>>(services.length, true,
		new DelegationResultListener<Collection<Tuple2<IBuyItemService, Integer>>>(cfp));
					
				 	
		//System.out.println("After call for proposal method");
		
		tempArray = new ArrayList<Offer>();
		
		// take all services for all corresponding orders
		for(int i=0; i<services.length; i++)
		{
			final IBuyItemService	seller	= services[i];
			
			
			//System.out.println(agent.getAgentName()+ " call for proposal\n");
				
			//wait till the seller agent initiate the negotiation or make proposal- call for proposal by the buyer to the seller
			seller.callForProposal(agent.getAgentName(),order.getName()).addResultListener(new IResultListener<Integer>()
			{
				public void resultAvailable(Integer result)
				{
					crl.resultAvailable(new Tuple2<IBuyItemService, Integer>(seller, result));
					
					//System.out.println("@BuyerBDI: Seller's make proposal = "+ result);
					//System.out.println("@BuyerBDI: Seller's round = "+ (currentRound-1)+"\n===========Buyer got the seller's proposall=====================\n");
					//Offer newSellerOffer = new Offer(result,currentTime,currentRound);
					
					//offerHistory.add(newSellerOffer);
					//tempArray.add(newSellerOffer);
					
					//set offer history data to the opponent offer history list
					//setOfferHistoryData("", order.getName(), result);
				}					
				 

				public void exceptionOccurred(Exception exception)
				{
					crl.exceptionOccurred(exception);
				}
						
			});
		}//end of for loop
		
				
		//===============calculate the next offer for the seller=====================================
				
		// Sort results by price.
		@SuppressWarnings("unchecked")
		Tuple2<IBuyItemService, Integer>[]	proposals	= cfp.get().toArray(new Tuple2[0]);
				
		//System.out.println("proposal array size = "+ proposals.length);
		
		Arrays.sort(proposals, new Comparator<Tuple2<IBuyItemService, Integer>>()
		{
			public int compare(Tuple2<IBuyItemService, Integer> o1, Tuple2<IBuyItemService, Integer> o2)
			{
				return o1.getSecondEntity().compareTo(o2.getSecondEntity());
			}
		});
				
		System.out.println(" \nproposals size = "+ proposals.length);
			
		//System.out.println("index of order = "+ this.getIndexOfOrder(order.getName()));
		
		//if the order has not entered in to the order list of the buyer
		if(this.getIndexOfOrder(order.getName()) == -1){
					
			//System.out.println(order.getName() + "Order index is added to "+ this.orderDetailsArrayList.size());
			
			//add the order to the order array list as the new order
			this.orderDetailsArrayList.add(this.orderDetailsArrayList.size(), new OrderDetails(order));
		}
		else{
			//System.out.println("index of order is already added to the order list");
		}
		
		//generate counter offers for all the proposals received by the sellers
		for(int k = 0; k < proposals.length; k++){
			
			System.out.println(" \n"+k);
			//---------get seller agent's name-----------------------------
			String firstEntity = proposals[k].getFirstEntity().toString();
			
			System.out.println(" first entity = "+firstEntity);
			
			//split the first entity string from the character @
			String[] parts = firstEntity.split("@"); 
			
			//1st part contains the agent name
			String agentName = parts[1];
			
			System.out.println(" agent name = "+agentName);
			
			//String orderName = order.getName(); 
			
			System.out.println("agent's proposal = "+ proposals[k].getSecondEntity().intValue());
			
			//set offer history data to the opponent offer history list
			setOfferHistoryData(agentName, order.getName(), proposals[k].getSecondEntity().intValue());					
					
			//calculate the order index for the order by given name
			int orderIndex = this.getIndexOfOrder(order.getName());
					
			//initiate the opponent index as -1
			int opponentIndex = -1;
					
			//if the order index is a positive or 0 (if order has entered in to the order array list)
			if(orderIndex >-1){
								
				//find the opponent index in the opponent array list corresponds to the order in the index
				opponentIndex = this.orderDetailsArrayList.get(orderIndex).getOpponentIndexInOpponentDetailArrayList(agentName);
			}
			
			System.out.println(orderIndex+ " - "+ opponentIndex);		
	 

			
			//set the previous offer of the buyer
			if(orderIndex > -1 && opponentIndex > -1){
				
				//if the current round is not 0
				if(this.currentRound > 0){
					
					//there is a previous offer of the buyer and retrieve it from the opponent offer history data
					this.setBuyerPreviousOffer(this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentPreviousOffer());
					
					System.out.println("previous offer = "+ this.buyerPreviousOffer.getOfferPrice()+" @ round = "+this.currentRound);
				
				}else{
					
					//There is no previous offer of the buyer @ round 0 therefore set it as the starting offer
					this.setBuyerPreviousOffer(this.historyPrice, this.currentTime,this.currentRound);
					System.out.println("previous offer = "+ this.buyerPreviousOffer.getOfferPrice()+" @ round = "+this.currentRound);
					
				}		
			}
			
			//initialize the generated offer by the buyer		
			Offer generatedOffer = new Offer();
					
			//Strategy 1- Before learning strategy
			if (neg_strategy.equals("strategy-1")) {
				//System.out.println("\nBuyer: strategy - 1  @ purchase item");
				/*double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				double price_span = order.getLimit() - order.getStartPrice();
				acceptable_price = (int) (price_span * elapsed_time / time_span) + order.getStartPrice();*/
						
				//System.out.println("\n+++++++++++++++++++++++++++++\nBuyer: STRategy - 2 @ Purchase item\n+++++++++++++++++++++++++\n"); 
						
				//generate the offers using the model strategy
				generatedOffer =  strategy_call.callForStrategy1(currentTime,order.getLimit()*1.0,order.getDeadline(), offerHistory,new Offer(this.historyPrice, this.currentTime, 0),this.buyerPreviousOffer, true, betaValue);
				//System.out.println("generated offer = "+generatedOffer.getOfferPrice());
			  			 
				acceptable_price =  (int) generatedOffer.getOfferPrice();			
						
			}
					
			//test strategy 2
			else if (neg_strategy.equals("strategy-2")) {
				//System.out.println("\n+++++++++++++++++++++++++++++\nBuyer: STRategy - 2 @ Purchase item\n+++++++++++++++++++++++++\n"); 
						
				//generate the offers using the model strategy
				generatedOffer = strategy_call.callForStrategy2(this.currentTime,this.detectionRegion, order.getStartPrice()* 1.0, order.getLimit()*1.0, order.getDeadline(), offerHistory,currentRound, this.numberOfRows, this.numberOfColumns,this.buyerPreviousOffer, true);
				//System.out.println("generated offer = "+generatedOffer.getOfferPrice());
			  			 
				acceptable_price =  (int) generatedOffer.getOfferPrice(); 
			}
					
			//Strategy 3- Main strategy
			else if (neg_strategy.equals("strategy-3")) { 
				//System.out.println("\n+++++++++++++++++++++++++++++\nBuyer: STRategy - 3 @ Purchase item\n+++++++++++++++++++++++++\n"); 
						
				//generate the offers using the model strategy
				generatedOffer = strategy_call.callForStrategy3(this.currentTime,this.detectionRegion, order.getStartPrice()* 1.0, order.getLimit()*1.0, order.getDeadline(), offerHistory,currentRound, this.numberOfRows, this.numberOfColumns,this.buyerPreviousOffer, true);
				//System.out.println("generated offer = "+generatedOffer.getOfferPrice());
			  			 
				acceptable_price =  (int) generatedOffer.getOfferPrice();
			} 
			//System.out.println("successfully calculated offer = "+ acceptable_price);
			 
			
			//this.utilityPriceArray.add(generatedOffer.getUtilityPriceValue());
			//this.utilityTimeArray.add(generatedOffer.getUtilityTimeValue());
					
			//System.out.println("Buyer: @ round= "+this.currentRound+" , offer = "+ acceptable_price);
			Offer buyerPrevOffer = new Offer(acceptable_price, generatedOffer.getOfferTime(), generatedOffer.getRoundNumber());
			
			System.out.println("Buyer newly Generated offer = "+buyerPrevOffer.getOfferPrice()+" @ round "+ buyerPrevOffer.getRoundNumber()+" for seller =  "+agentName);		
			
			//set the buyer previous offer at the corresponding opponent history data
			if(orderIndex > -1 && opponentIndex > -1){
				 
				//Insert acceptable offer for the opponent detail array list
				 
				//set the buyer's current offer as the previous offer
				this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).setAgentPreviousOffer(buyerPrevOffer);
				
				//System.out.println(buyerPrevOffer.getOfferPrice()+" @ round"+ buyerPrevOffer.getRoundNumber());			
				//System.out.println(agentName+ " agent round no in offer history after increment = "+ this.currentRound );
			}				
					
			//this.currentRound++;
			//set the global current round to the seller current round
			this.currentRound = this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).increaseCurrentRound() ;
					
					
			goal.acceptedPrice = acceptable_price;			

			// check whether a winner is available
			if(proposals.length> 0 && proposals[k].getSecondEntity().intValue() <= acceptable_price && !this.isDone)
				{
					proposals[k].getFirstEntity().acceptProposal(this.agent.getAgentName(),order.getName(), proposals[k].getSecondEntity().intValue()).get();
						
					//this.averagePriceUtility = (new Calculator()).calculateAverageUtility(utilityPriceArray);
					//this.averageTimeUtility = (new Calculator()).calculateAverageUtility(utilityTimeArray);
						
					//System.out.println("Buyer: average price utility = "+ this.averagePriceUtility );
					//System.out.println("Buyer: average time utility = "+ this.averageTimeUtility );
						
					generateNegotiationReport(order, proposals[k], acceptable_price);
						
						
					 
					System.out.println("isdone = false");
					order.setState(Order.DONE);
					order.setExecutionPrice(proposals[k].getSecondEntity());
					order.setExecutionDate(new Date(getTime()));
					this.isDone = true;
					System.out.println("isdone = true");
							
					 
						
						
					} else if(!this.isDone) {

						 	System.out.println("service length = "+  services.length);
							System.out.println(agent.getAgentName()+ "Set acceptable price " + acceptable_price);
							
							services[k].setacceptablePrice(this.agent.getAgentName(),order, order.getName(), acceptable_price, this.offerHistory.get(this.offerHistory.size()-1));
					 
						
						NegotiationReport nr = generateNegotiationReport(order, proposals[k], acceptable_price);
						
						
						//System.out.println("BUYER " + nr.toString());
					/*	try {
							ew.writefile(nr.toString());
						} catch (IOException e) {
							e.printStackTrace();
						}
						throw new PlanFailureException();*/
					}
					//System.out.println("result: "+cnp.getParameter("result").getValue());
					//System.out.println(agent.getAgentName()+ " @  end - purchase item");	
			
		}//end of loop k
				
				
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
	 * Set Buyer's offer history at the sellerBDI
	 * @param name
	 * @param price
	 */
	public void setOfferHistoryData(String agentName, String name, int price) {
		
		//System.out.println("\n this is set acceptable price Method\n=====================================================");
		
		int orderIndex;		 
		
		//find the index of the order
		orderIndex = this.getOrderDetailIndex(name);
		
		//initialize the opponent index and the agent round no to the -1
		int opponentIndex = -1;
		int agentRoundNo = -1;
		
		if( orderIndex > -1 ){
			
			//System.out.println("The order has entered in the list @ "+ orderIndex);
			
			opponentIndex = this.orderDetailsArrayList.get(orderIndex).getOpponentIndexInOpponentDetailArrayList(agentName );
			
			//System.out.println("calculated opponent index =  "+ opponentIndex);
			
			if(opponentIndex == -1){
				//System.out.println("opponent is newly added to the array list");
				
			}
		
		}else{
			//System.out.println("The order has not entered in the list");
		}
		
		if(orderIndex > -1 && opponentIndex > -1){
			
			//System.out.println("both indices are not equal to -1\n Therefore retrive the agent round no");
			
			agentRoundNo = this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentCurrentRound();
			
			//System.out.println("Agent round no = "+ agentRoundNo);
			
		}else{
			
			//System.out.println("one of the index is -1");
			
		}
		 
		
		 //in the 0th round
		if(agentRoundNo == -1 ){
			agentRoundNo = 0;
			//System.out.println("Agent new round no = "+ agentRoundNo);
		} 
		 
		
		if(orderIndex > -1){ 
			
			//System.out.println( "Before  added a opponent history to the offer history: "+this.orderDetailsArrayList.get(orderIndex).getOfferHistoryOfGivenOpponent(agentName).size() );
			
			
			//Insert acceptable offer for the opponent detail array list		 
			this.orderDetailsArrayList.get(orderIndex).addOfferToOpponentOfferHistory(agentName, new Offer(price, new Date(), agentRoundNo));
			
			//System.out.println( "After  added a opponent history to the offer history: "+this.orderDetailsArrayList.get(orderIndex).getOfferHistoryOfGivenOpponent(agentName).size() );
			
			//if after the opponent has entered yet the opponent index is -1 recalculate the opponent index
			if(opponentIndex == -1){
				
				opponentIndex = this.orderDetailsArrayList.get(orderIndex).getOpponentIndexInOpponentDetailArrayList(agentName );
				
				//System.out.println("new opponent index = "+ opponentIndex);
			}
			
			//set the global current round to the seller current round
			this.currentRound = this.orderDetailsArrayList.get(orderIndex).getOpponentDetailArrayList().get(opponentIndex).getAgentCurrentRound() ;
			
			 //System.out.println(agentName+ " agent round no in offer history  = "+ this.currentRound );
			
			//set global value of offer history
			this.offerHistory = this.orderDetailsArrayList.get(orderIndex).getOfferHistoryOfGivenOpponent(agentName);
			
			
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
	 * Initialize the detection region of the seller.
	 */
	public void setDetectionRegion(Order order, Date currentTime){
		
		// Initialize the detection region of the seller
		double detRegLowerPriceBoundary = (order.getStartPrice() + order.getLimit())* 0.5;
		//double detRegLowerPriceBoundary = order.getStartPrice();
		
		//System.out.println("Buyer: Lower price = "+ detRegLowerPriceBoundary);
		//double detRegUpperPriceBoundary = order.getLimit();
		
		double detRegUpperPriceBoundary = order.getLimit() +(order.getLimit() - order.getStartPrice())* 0.5;
		//System.out.println("Buyer: Upper price = "+ detRegUpperPriceBoundary); 
		//Date detRegLowerTimeBoundary = new Date(currentTime.getTime());
		Date detRegLowerTimeBoundary = new Date((order.getDeadline().getTime() + currentTime.getTime())/2);
		//System.out.println("Buyer: Lower time = "+ detRegLowerTimeBoundary);
		
		Date detRegUpperTimeBoundary = new Date(order.getDeadline().getTime() + ((order.getDeadline().getTime()-currentTime.getTime())*1/2));
		//System.out.println("Buyer: Upper time = "+ detRegUpperTimeBoundary);
		
		// Initialize the detection region
		this.detectionRegion = new DetectionRegion(detRegLowerPriceBoundary, detRegUpperPriceBoundary, detRegLowerTimeBoundary, detRegUpperTimeBoundary, numberOfRows, numberOfColumns);
	}
	
	/**
	 * set the buyers current offer to the previous offer.
	 */
	public void setBuyerPreviousOffer(double price, Date time, int round){		
		this.buyerPreviousOffer.setOfferPrice(price);
		this.buyerPreviousOffer.setOfferTime(time);
		this.buyerPreviousOffer.setRoundNumber(round);
	} 
	
	/**
	 * set the previous offer to the global variable.
	 */
	public void setBuyerPreviousOffer(Offer prevOffer){	
		
		this.buyerPreviousOffer.setOfferPrice(prevOffer.getOfferPrice());
		this.buyerPreviousOffer.setOfferTime(prevOffer.getOfferTime());
		this.buyerPreviousOffer.setRoundNumber(prevOffer.getRoundNumber());
		
	}
	
	/**
	 * Generate and add a negotiation report.
	 * @return 
	 */
	protected NegotiationReport generateNegotiationReport(Order order, Tuple2<IBuyItemService, Integer> proposal,
			double acceptable_price) {
		
		System.out.println("Negotiation Report generation Method");
		
		String report = "Accepable price: " + acceptable_price + ", proposals: ";
		
		if (proposal != null) {
			
			 
				report += proposal.getSecondEntity() + "-" + proposal.getFirstEntity().toString();
				//System.out.println("first entity = " + proposals[i].getFirstEntity().toString()); 
				
			 
		} else {
			report += "No seller found, purchase failed.";
		}
		
		NegotiationReport nr = new NegotiationReport(order, report, getTime());
		// System.out.println("REPORT of agent: "+getAgentName()+" "+report);
		
		reports.add(nr);
		
		return nr;
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
	 * Create a purchase or sell oder.
	 */
	public void createGoal(Order order)
	{  
		//main process of creating a goal
		PurchaseItem goal = new PurchaseItem(order);
		agent.dispatchTopLevelGoal(goal);
		
		this.currentTime = new Date(this.getTime());
		this.historyPrice = order.getStartPrice();
		this.buyerPreviousOffer = new Offer( historyPrice, this.currentTime , 0);
		this.offerHistory = new ArrayList<Offer>();
		this.strategy_call = new StrategyCall();
		this.currentRound = 0; 
		
		
		//initialize the detection region of the buyer
		this.setDetectionRegion(order, this.currentTime);

		//System.out.println("Buyer: create Goal  @ round: "+ (currentRound));
	}

	/**
	 * Get all purchase or sell goals.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<INegotiationGoal> getGoals() {
		return (Collection) agent.getGoals(PurchaseItem.class);
	}

	/**
	 * Get all reports.
	 */
	public List<NegotiationReport> getReports(Order order) {
		List<NegotiationReport> ret = new ArrayList<NegotiationReport>();
		for (NegotiationReport rep : reports) {
			if (rep.getOrder().equals(order)) {
				ret.add(rep);
			}
		}
		return ret;
	}
}