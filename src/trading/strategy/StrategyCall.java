package trading.strategy;
 

import java.util.ArrayList;
import java.util.Date;

public class StrategyCall {
	
	//the attributes for strategy call
	private Calculator calculator;
	private RegressionAnalyser regressionAnalyser;
	private BayesianLearner bayesianLearner;
	private AdaptiveConcessionStrategy ConcessionStrategy;
	
	Date lastExpiredCellCheckTime = new Date();

	/**
	 *  Constructor 
	 */
	public StrategyCall() {
		
		//create objects
		calculator = new Calculator();
		regressionAnalyser = new RegressionAnalyser(calculator);
		bayesianLearner = new BayesianLearner(calculator);
		ConcessionStrategy = new AdaptiveConcessionStrategy(calculator);	 		
	}
	
	

	/**
	 *  Strategy-3 execution method 
	 */
	public Offer callForStrategy3(DetectionRegion detectionRegion, double startPrice, double reservePrice, Date deadline, ArrayList<Offer> offerHistory, int numberOfRounds, int currentRound , int numberOfRows, int numberOfColumns, Offer prevOffer, boolean isBuyer) {
		
		/*//To print the agent in print statements
		String agent="";
		if(isBuyer){
			agent = "Buyer: ";
		}
		else{
			agent = "Seller: ";
		}		
		
		//System.out.println("\n "+agent+"------\nprevious Offer History Length= "+offerHistory.size()+" , current Round = "+ currentRound);
		
		//Temporally adding values
		if(offerHistory.size() < currentRound && !isBuyer){
			
			if(offerHistory.size()==0){
				//System.out.println("calculated value temp: "+ 1400);
				offerHistory.add(new Offer(1400, new Date() ,0));
			}
			else{
				
				for(int i = (offerHistory.size()); i< (currentRound) ; i++){
					//System.out.println("calculated value temp: "+ 1400+(i*5));
					offerHistory.add(new Offer(1400+(i*5), new Date() ,i));
				}
			}
		} 
		

		//System.out.println("\n"+agent+"------------------------------------------------------------------");
		
		//print the offer history array list
		for(int i = 0; i< offerHistory.size(); i++){			
			System.out.println(agent+"offer History @ "+i+" = "+offerHistory.get(i).getOfferPrice());
		} 
		
		System.out.println(agent+"------------------------------------------------------------------\n");
		System.out.println(agent+"\nNow Offer History Length= "+offerHistory.size()+" , current Round = "+ currentRound);
		
		System.out.println("\n"+agent+"=====================Before the regression method execution=========================" );*/			 
		
		//regression analyze the historical offers
		regressionAnalyser.Analyse(detectionRegion, numberOfRows, numberOfColumns, offerHistory, currentRound, isBuyer);
		
		//System.out.println(agent+"=====================After the regression method execution========================= " );
		
		//before calculating the bayesian learning set the expired cells
		this.setExpiredCells(detectionRegion, reservePrice, numberOfRows, numberOfColumns, prevOffer, isBuyer);
		
		//Bayesian update is done 2nd round onwards offer as gamma has a value then
		if(offerHistory.size()>1){
			
			 //update the hypothesis of each cell in the detection region using Bayesian learning
			bayesianLearner.Learn(numberOfRows, numberOfColumns, detectionRegion, currentRound);			
			
		} 
		
		//System.out.println(agent+"=====================After the bayesian learning method execution========================= " );

		
		//calculate the step size
		long stepSize = this.getStepSize(currentRound, offerHistory);
		//System.out.println(agent+"\ncalculate step size = "+ stepSize);
		
		//System.out.println(agent+"=====================After the step size calculation========================= " );
		
		//before calculating the concession strategy set the expired cells
		this.setExpiredCells(detectionRegion, reservePrice, numberOfRows, numberOfColumns, prevOffer, isBuyer);
		
		//System.out.println(agent+"\n================After setting expired cells==============\n"); 
		
		//Optimal concession strategy : find the concession points for each cell in the negotiation region
		ConcessionStrategy.FindConcessionPoint(numberOfRows, numberOfColumns, detectionRegion, reservePrice, deadline,
				stepSize, offerHistory, prevOffer , isBuyer);
		
		//System.out.println(agent+"\n================end of the concession points calculations==============\n");		
		
		
		//this.printConcessionPoints(detectionRegion, numberOfRows, numberOfColumns);
		
		//System.out.println(agent+"\n================end of the printing concessionpoints==============\n");
		
		
		Offer nextOffer = ConcessionStrategy.GenerateNextOffer(detectionRegion, reservePrice, deadline, numberOfRows, numberOfColumns, prevOffer,
				stepSize, isBuyer, offerHistory);
		
		//System.out.println(agent+"||||||||||||||||||||||||||||||||THE END OF STRATEGY CALL||||||||||||||||||||||||||\n");
		 
		return nextOffer;
	}
	
	/**
	 *  Strategy-3 execution method 
	 */
	public Offer callForStrategy2(DetectionRegion detectionRegion, double startPrice, double reservePrice, Date deadline, ArrayList<Offer> offerHistory, int numberOfRounds, int currentRound , int numberOfRows, int numberOfColumns, Offer prevOffer, boolean isBuyer) {
		
		//To print the agent in print statements
		String agent="";
		if(isBuyer){
			agent = "Buyer: ";
		}
		else{
			agent = "Seller: ";
		}		
		
		/*//System.out.println("\n "+agent+"------\nprevious Offer History Length= "+offerHistory.size()+" , current Round = "+ currentRound);
		
		//Temporally adding values
		if(offerHistory.size() < currentRound && !isBuyer){
			
			if(offerHistory.size()==0){
				//System.out.println("calculated value temp: "+ 1400);
				offerHistory.add(new Offer(1400, new Date() ,0));
			}
			else{
				
				for(int i = (offerHistory.size()); i< (currentRound) ; i++){
					//System.out.println("calculated value temp: "+ 1400+(i*5));
					offerHistory.add(new Offer(1400+(i*5), new Date() ,i));
				}
			}
		} 
		

		//System.out.println("\n"+agent+"------------------------------------------------------------------");
		
		//print the offer history array list
		for(int i = 0; i< offerHistory.size(); i++){			
			System.out.println(agent+"offer History @ "+i+" = "+offerHistory.get(i).getOfferPrice());
		} 
		
		System.out.println(agent+"------------------------------------------------------------------\n");
		System.out.println(agent+"\nNow Offer History Length= "+offerHistory.size()+" , current Round = "+ currentRound);*/
		
		
		System.out.println("\n"+agent+"=====================change reserve price=========================" );
		
		Date pretendedDeadline = calculator.GeneratePretendedDeadline(detectionRegion, numberOfRows, numberOfColumns, deadline);
		System.out.println("pretened deadline = "+ pretendedDeadline);
		if(pretendedDeadline == deadline){
			System.out.println("calculated pretended deadline as same as the real deadline = " + deadline);
		}else{
			System.out.println("pretended deadline is not equal as real deadline = "+ deadline);
		}
		
		///*System.out.println("\n"+agent+"=====================Before the regression method execution=========================" );*/			 
		
		//regression analyze the historical offers
		regressionAnalyser.Analyse(detectionRegion, numberOfRows, numberOfColumns, offerHistory, currentRound, isBuyer);
		
		//System.out.println(agent+"=====================After the regression method execution========================= " );
		
		//before calculating the bayesian learning set the expired cells
		this.setExpiredCells(detectionRegion, reservePrice, numberOfRows, numberOfColumns, prevOffer, isBuyer);
		
		//Bayesian update is done 2nd round onwards offer as gamma has a value then
		if(offerHistory.size()>1){
			
			 //update the hypothesis of each cell in the detection region using Bayesian learning
			bayesianLearner.Learn(numberOfRows, numberOfColumns, detectionRegion, currentRound);			
			
		} 
		
		//System.out.println(agent+"=====================After the bayesian learning method execution========================= " );

		
		//calculate the step size
		long stepSize = this.getStepSize(currentRound, offerHistory);
		//System.out.println(agent+"\ncalculate step size = "+ stepSize);
		
		//System.out.println(agent+"=====================After the step size calculation========================= " );
		
		//before calculating the concession strategy set the expired cells
		this.setExpiredCells(detectionRegion, reservePrice, numberOfRows, numberOfColumns, prevOffer, isBuyer);
		
		//System.out.println(agent+"\n================After setting expired cells==============\n"); 
		
		//Optimal concession strategy : find the concession points for each cell in the negotiation region
		ConcessionStrategy.FindConcessionPoint(numberOfRows, numberOfColumns, detectionRegion, reservePrice, pretendedDeadline,
				stepSize, offerHistory, prevOffer , isBuyer);
		
		//System.out.println(agent+"\n================end of the concession points calculations==============\n");		
		
		
		//this.printConcessionPoints(detectionRegion, numberOfRows, numberOfColumns);
		
		//System.out.println(agent+"\n================end of the printing concessionpoints==============\n");
		
		
		Offer nextOffer = ConcessionStrategy.GenerateNextOffer(detectionRegion, reservePrice, pretendedDeadline, numberOfRows, numberOfColumns, prevOffer,
				stepSize, isBuyer, offerHistory);
		
		//System.out.println(agent+"||||||||||||||||||||||||||||||||THE END OF STRATEGY CALL||||||||||||||||||||||||||\n");
		 
		return nextOffer;
	}
	 
	
	
	/**
	 *  Get the step size of the negotiation.
	 *  Here assume that the opponent also negotiate at the same step size as the agent
	 */
	private long getStepSize(int currentRound, ArrayList<Offer> offerHistory){
		
		long stepSize = 0;
		
		if(offerHistory.size() > 1){
			
			//get 1st and last round time
			long firstRoundTime = offerHistory.get(0).getOfferTime().getTime();
			long lastRoundTime = offerHistory.get(offerHistory.size()-1).getOfferTime().getTime();
			
			stepSize = ((lastRoundTime - firstRoundTime ) / (offerHistory.size()-1));
			
		}
		else if(offerHistory.size()==1){
			
			//get 1st and current round time
			long firstRoundTime = offerHistory.get(0).getOfferTime().getTime();
			long currentTime = new Date().getTime();
			
			stepSize = (currentTime - firstRoundTime );
			
		} 
		return stepSize;
	}
	
	/**
	 *  check and mark the expired cells.
	 */
	public void setExpiredCells(DetectionRegion detectionRegion, double reservePrice, int numberOfRows, int numberOfColumns, Offer prevOffer, boolean isBuyer){
		
		//set current Time To last expired cell check time
		Date currentExpiredCellCheckTime = new Date();
		
		for(int j = 0; j< numberOfColumns; j++ ){ 
			
			for(int i =0; i< numberOfRows; i++){
				
				//get the unchecked cells
				 if(this.lastExpiredCellCheckTime.getTime() <= detectionRegion.getCells()[i][j].getCellLowerDeadline().getTime()){
					 
					 //check whether the cell has expired or not
					 if( currentExpiredCellCheckTime.getTime() >= detectionRegion.getCells()[i][j].getCellUpperDeadline().getTime()){
						 
						 //if the cell has not marked already as expired
						 if(!detectionRegion.getCells()[i][j].isExpired()){
							 detectionRegion.getCells()[i][j].setExpired(true); 
						 }
						 						 
					 }// end of first inner if clause
					 
					//expire the cells which are beyond the reserve price if it is the buyer and otherwise
					 if(isBuyer){						 
						 if(detectionRegion.getCells()[i][j].getCellLowerPrice()>= reservePrice){
							 
							//if the cell has not marked already as expired
							 if(!detectionRegion.getCells()[i][j].isExpired()){
								 detectionRegion.getCells()[i][j].setExpired(true); 
							 }
						 }						 
					 }
					 else{
						 if(detectionRegion.getCells()[i][j].getCellUpperPrice()<= reservePrice){
							 
								//if the cell has not marked already as expired
								 if(!detectionRegion.getCells()[i][j].isExpired()){
									 detectionRegion.getCells()[i][j].setExpired(true); 
								 }
							 }	
					 }//end of inner if else clause	 
					 
				 }//end of outer if clause
				 //System.out.println("cell["+i+","+j+"]: expired= "+detectionRegion.getCells()[i][j].isExpired());
			
			} //end of  inner for loop			
		} //end of outer for loop
		
		//set the current checking time as the last checking time
		this.lastExpiredCellCheckTime = currentExpiredCellCheckTime;
		
	}
	
	/**
	 *  print all the concession points in the region.
	 *  @param detectionRegion: detectionRegion
	 *  @param numberOfRows: number of rows in the detection region
	 *  @param numberOfColumns: number of columns in th detection region
	 */
	public void printConcessionPoints(DetectionRegion detectionRegion, int numberOfRows, int numberOfColumns){
		for(int i=0; i< numberOfRows; i++){
			for(int j=0; j<numberOfColumns; j++){ 
				System.out.println("cell["+i+","+j+"]: concessionPoint = "+detectionRegion.getCells()[i][j].getConcessionPoint().getConcessionPointPrice());
			}
			System.out.println();
		}
	}
	
	
	

}
