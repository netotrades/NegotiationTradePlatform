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
import trading.common.ExcelWriter;
import trading.common.Gui;
import trading.common.NegotiationReport;
import trading.common.Order; 
import trading.strategy.Calculator;
import trading.strategy.DetectionRegion;
import trading.strategy.Offer;
import trading.strategy.StrategyCall;

import java.io.IOException;
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
@RequiredServices({ @RequiredService(name = "buyservice", type = IBuyItemService.class, multiple = true),
		@RequiredService(name = "clockser", type = IClockService.class, binding = @Binding(scope = RequiredServiceInfo.SCOPE_PLATFORM) ) })
@Arguments(@Argument(name = "initial_orders", clazz = Order[].class))
public class BuyerBDI implements INegotiationAgent {
	@Agent
	protected BDIAgent agent;
	
	@Belief
	protected List<NegotiationReport> reports = new ArrayList<NegotiationReport>();	
	protected Gui gui;
	
	//seller's historical offers
	private ArrayList<Offer> offerHistory;
	
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
		
		int acceptable_price = 0;
		Order order = goal.getOrder();
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
				 
		// Initiate a call-for-proposal.
		Future<Collection<Tuple2<IBuyItemService, Integer>>>	cfp	= new Future<Collection<Tuple2<IBuyItemService, Integer>>>();
		final CollectionResultListener<Tuple2<IBuyItemService, Integer>>	crl	= new CollectionResultListener<Tuple2<IBuyItemService, Integer>>(services.length, true,
		new DelegationResultListener<Collection<Tuple2<IBuyItemService, Integer>>>(cfp));
			
		 	
		for(int i=0; i<services.length; i++)
		{
			final IBuyItemService	seller	= services[i];
			
					//System.out.println(agent.getAgentName()+ " call for proposal\n");
					
			seller.callForProposal(agent.getAgentName(),order.getName()).addResultListener(new IResultListener<Integer>()
			{
				public void resultAvailable(Integer result)
				{
					crl.resultAvailable(new Tuple2<IBuyItemService, Integer>(seller, result));
					
					//System.out.println("@BuyerBDI: Seller's make proposal = "+ result);
					//System.out.println("@BuyerBDI: Seller's round = "+ (currentRound-1)+"\n===========Buyer got the seller's proposall=====================\n");
					Offer newSellerOffer = new Offer(result,currentTime,currentRound);
					offerHistory.add(newSellerOffer);
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
		Arrays.sort(proposals, new Comparator<Tuple2<IBuyItemService, Integer>>()
		{
			public int compare(Tuple2<IBuyItemService, Integer> o1, Tuple2<IBuyItemService, Integer> o2)
			{
				return o1.getSecondEntity().compareTo(o2.getSecondEntity());
			}
		});
		
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
		
		this.utilityPriceArray.add(generatedOffer.getUtilityPriceValue());
		this.utilityTimeArray.add(generatedOffer.getUtilityTimeValue());
		
		//System.out.println("Buyer: @ round= "+this.currentRound+" , offer = "+ acceptable_price);
		this.setBuyerPreviousOffer(acceptable_price, generatedOffer.getOfferTime(), generatedOffer.getRoundNumber());

		this.currentRound++;
		
		goal.acceptedPrice = acceptable_price;			

		// check whether a winner is available
		if(proposals.length>0 && proposals[0].getSecondEntity().intValue()<= acceptable_price)
		{
			proposals[0].getFirstEntity().acceptProposal(this.agent.getAgentName(),order.getName(), proposals[0].getSecondEntity().intValue()).get();
			
			this.averagePriceUtility = (new Calculator()).calculateAverageUtility(utilityPriceArray);
			this.averageTimeUtility = (new Calculator()).calculateAverageUtility(utilityTimeArray);
			
			//System.out.println("Buyer: average price utility = "+ this.averagePriceUtility );
			//System.out.println("Buyer: average time utility = "+ this.averageTimeUtility );
			
			generateNegotiationReport(order, proposals, acceptable_price);
			// If contract-net succeeds, store result in order object.
			order.setState(Order.DONE);
			order.setExecutionPrice(proposals[0].getSecondEntity());
			order.setExecutionDate(new Date(getTime()));
			
		} else {

			for (int i = 0; i < services.length; i++) {
				
				//System.out.println(agent.getAgentName()+ "Set acceptable price @ Seller");
				
				services[i].setacceptablePrice(this.agent.getAgentName(),order, order.getName(), acceptable_price, this.offerHistory.get(this.offerHistory.size()-1));
			}
			
			NegotiationReport nr = generateNegotiationReport(order, proposals, acceptable_price);
			//System.out.println("BUYER " + nr.toString());
			try {
				ew.writefile(nr.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			throw new PlanFailureException();
		}
		//System.out.println("result: "+cnp.getParameter("result").getValue());
		//System.out.println(agent.getAgentName()+ " @  end - purchase item");	
		
		
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
	 * Generate and add a negotiation report.
	 * @return 
	 */
	protected NegotiationReport generateNegotiationReport(Order order, Tuple2<IBuyItemService, Integer>[] proposals,
			double acceptable_price) {
		String report = "Accepable price: " + acceptable_price + ", proposals: ";
		
		if (proposals != null) {
			
			for (int i = 0; i < proposals.length; i++) {
				report += proposals[i].getSecondEntity() + "-" + proposals[i].getFirstEntity().toString();
				
				//System.out.println(this.agent.getAgentName() +" "+i+ "- report = "+ report);
				
				if (i + 1 < proposals.length)
					report += ", ";
			}
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