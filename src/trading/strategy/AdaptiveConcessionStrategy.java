package trading.strategy;

import java.util.ArrayList;
import java.util.Date;

public class AdaptiveConcessionStrategy {
	
	//Attributes
	private Calculator calculator;
	
	//initialize the parameters
	double concessionPrice = 0.0;				
	double opponentOfferAtTb = 0.0;
	double opponentOfferAtT0 = 0.0;		
	Date concessionTime = null;	
	ConcessionPoint newConcessionPoint;
	
	double p0 = 0.0 ; //agent's previous offer price
	long t0 = 0; // agent's previous offer time
	long T = 0; //agent's own deadline
	long tix = 0; // time of the random point in the cell	
	double pix = 0.0; // price of the random point in the cell
	double piMax = 0; // (0 <<<<< piMax <  1)
	double piMin = 0; // (0 < piMin <<<< 1)
	Date timeAtOfferP0; //time at the offer p0
	
	/**
	 *  Constructor. 
	 */
	public AdaptiveConcessionStrategy(Calculator calculator){
		this.calculator = calculator;
	}
	
	/**
	 *  Method to find concession point for each and every cell in the detection region. 
	 */
	public void FindConcessionPoint(int numberOfRows, int numberOfColumns, DetectionRegion detReg, double reservePrice, Date deadline, long stepSize, ArrayList<Offer> offerHistory, Offer prevOffer, boolean isBuyer){
		//System.out.println("\n==========This is the find concession point method==================\n");
 		
		if(offerHistory.size() > 0){
			
			this.p0 = prevOffer.getOfferPrice(); //last offer price
			this.t0 = prevOffer.getOfferTime().getTime(); //last offer time
			//System.out.println("p0 = "+ p0);
			//System.out.println("t0 = "+ t0);
			
			//Travel through each cell to update the concession points
			for(int i = 0; i < numberOfRows; i++){
				
				//System.out.println("number of rows = "+numberOfRows);
				
				for(int j = 0; j < numberOfColumns; j++){
					
					//System.out.println("\n\nnumber of columns = "+numberOfColumns);
					//System.out.println("cell["+i+","+j+"]: Expired = "+ detReg.getCells()[i][j].isExpired()+"\n----------------------------");
					
					//concession point is calculated only for the valid cell in negotiation region 
					if(!detReg.getCells()[i][j].isExpired()){
						
						this.pix = detReg.getCells()[i][j].getCellReservePrice();
						this.tix = detReg.getCells()[i][j].getCellDeadline().getTime();
						this.T = deadline.getTime(); //agent's own deadline						
						
						//System.out.println("tix = "+ tix);
						//System.out.println("T = "+ T);
						//System.out.println("pix = "+ pix); 
						
						//calculate the opponent's offer @ Tb (deadline) and @ T0 (current time)
						this.opponentOfferAtTb = calculator.GenerateFittedOfferForGivenTime(detReg, i, j, offerHistory, deadline);
						this.opponentOfferAtT0 = calculator.GenerateFittedOfferForGivenTime(detReg, i, j, offerHistory, prevOffer.getOfferTime());
						
						//System.out.println("opponentOfferAtTb = "+ opponentOfferAtTb);
						//System.out.println("opponentOfferAtT0 = "+ opponentOfferAtT0);  
						
											
						if(isBuyer){
							this.buyerConcessionStrategyForGivenCell(i, j, detReg, reservePrice, deadline, stepSize, offerHistory);
						}
						else{
							this.sellerConcessionStrategyForGivenCell(i, j, detReg, reservePrice, deadline, stepSize, offerHistory);
						}
						
						//create a concession point
						this.newConcessionPoint = new ConcessionPoint(concessionPrice, concessionTime);
						
						//System.out.println("create new concession point = "+concessionPrice+" time= "+concessionTime);
						
						//set the concession point to the corresponding cell
						detReg.getCells()[i][j].setConcessionPoint(newConcessionPoint);	
						
						//System.out.println("get added concession point: price= "+detReg.getCells()[i][j].getConcessionPoint().getConcessionPointPrice()+" time= "+detReg.getCells()[i][j].getConcessionPoint().getConcessionPointTime());
						//System.out.println("inner for loop = "+i+", "+j);
					}
					else{ 					
						
						//System.out.println("else method;;;;;;;;;");
						
						//set the concession point to the corresponding cell
						detReg.getCells()[i][j].setConcessionPoint(new ConcessionPoint(p0, new Date()));	
					}
					
				}// end of inner for loop
				
				//System.out.println("End = number Of rows ="+ numberOfRows);
				
			}//end of the outer for loop
			
			//System.out.println("finish setting concession points");
		}
		
	}
	
	/**
	 *  identify the concession strategy according to the buyers perspective. 
	 */
	public void buyerConcessionStrategyForGivenCell(int row, int col, DetectionRegion detReg, double reservePrice, Date deadline, long stepSize, ArrayList<Offer> offerHistory){
				 
		//concession strategy scenario : 1
		if(( tix < T) && (pix > p0)){
			
			System.out.println("this is strategy 1");
			
			//get random point as the concession point
			this.concessionPrice = pix;
			this.concessionTime = detReg.getCells()[row][col].getCellDeadline();
			
		} //end of concession strategy scenario : 1
		
		//concession strategy scenario : 2
		else if((tix >= T) && (pix >= p0)){
			
			System.out.println("this is strategy 2");
			
			//case 1: line l2 - regression line which does not travel across the negotiation region
			if( opponentOfferAtTb > reservePrice){
				
				// random value piMax belongs to the range (0<<<< piMax < 1)
				this.piMax = (0.90 + ((0.99 - 0.90)* Math.random()));
				
				//set the point which is the next step with the the price approximate to the deadline
				this.concessionPrice = (reservePrice * piMax);
				this.concessionTime = new Date( t0 + stepSize);
				
			}// end of case: 1
			
			//case 2: line l1 - regression line which travels across the negotiation region
			else{						
				
				//set the point which is in the previous step to the deadline and on the regression line
				concessionTime = new Date(deadline.getTime() - stepSize);
				this.concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, row, col, offerHistory, concessionTime);
			} //end of case: 2
			
		} //end of the concession strategy scenario : 2
		
		//concession strategy scenario : 3
		else if(( tix < T) && (pix < p0)){
			
			System.out.println("this is strategy 3");
			
			//case 1: line l1 - regression line which travels across the negotiation region
			if( opponentOfferAtT0 > p0){
				
				//calculate the time at the offer p0
				Date timeAtOfferP0 = calculator.GeneratedTimeForGivenFittedOffer(detReg, row, col, offerHistory, p0);
				
				//set the point in which is in the previous step to the p0 price line and on the regression line
				this.concessionTime = new Date(timeAtOfferP0.getTime() - stepSize);
				this.concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, row, col, offerHistory,  concessionTime);
				
			}// end of the case 1
			
			//case 2: line l2 - regression line which does not travel across the negotiation region
			else{
				// random value piMin belongs to the range (0 < piMin <<<<< 1)
				this.piMin = (0.20 - ((0.20 - 0.01)* Math.random()));
				
				//set the point which is the next step with the the price approximate to the deadline
				this.concessionPrice = ( p0 * ( 1 + piMin ));						 
				this.concessionTime = new Date(deadline.getTime() - stepSize);
			}
			//end of the case 2
			
		}//end of the concession strategy scenario : 3
		
		//concession strategy scenario : 4
		else if(( tix >= T) && (pix <= p0)){
			
			System.out.println("this is strategy 4");
			
			//case 1: line l3 - regression line which does not travel across the negotiation region but underneath of the negotiation region
			if( opponentOfferAtT0 < p0){
				
				// random value piMin belongs to the range (0 < piMin <<<<< 1)
				this.piMin = (0.20 - ((0.20 - 0.01)* Math.random()));
				
				//set the point which is the previous step to the deadline and approximate to the p0
				this.concessionPrice = p0 * (1 + piMin);
				this.concessionTime = new Date(deadline.getTime() - stepSize);
				
			} //end of the case 1
			
			//case 2: line l4 - regression line which does not travel across the negotiation region and goes over the negotiation region
			else if( opponentOfferAtTb > reservePrice ){
				
				// random value piMax belongs to the range (0<<<< piMax < 1)
				this.piMax = (0.90 + ((0.98 - 0.90)* Math.random()));
				
				//set the point which is the next step with the the price approximate to the deadline
				this.concessionPrice = (reservePrice * piMax);
				this.concessionTime = new Date( t0 + stepSize); 
				
			}//end of the case 2
			
			//case 3: line l1 - regression line which travels across the negotiation region and goes between the reserve price and the p0 at the deadline
			else if(opponentOfferAtTb <= reservePrice && opponentOfferAtTb > p0){
			
				//set the point which is in the previous step to the deadline and on the regression line
				this.concessionTime = new Date(deadline.getTime() - stepSize);
				this.concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, row, col, offerHistory, concessionTime);
			
			}//end of the case 3
			
			//case 4: line l2- regression line which travels across the negotiation region and goes underneath the p0 line at the dead line
			else{ 
				//calculate the time at the offer p0
				this.timeAtOfferP0 = calculator.GeneratedTimeForGivenFittedOffer(detReg, row, col, offerHistory,   p0);
				
				//set the point in which is in the previous step to the p0 price line interception and on the regression line
				this.concessionTime = new Date(timeAtOfferP0.getTime() - stepSize);
				this.concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, row, col, offerHistory, concessionTime);
			
			} //end of the case 4					
		
		}//end of the concession strategy scenario : 4
		
		if(this.concessionPrice == Double.NaN){
			this.concessionPrice = reservePrice;
		} 
		
		//System.out.println("This is the end of this methodd\n");
	}
	
	/**
	 *  identify the concession strategy for seller's perspective. 
	 */
	public void sellerConcessionStrategyForGivenCell(int row, int col, DetectionRegion detReg, double reservePrice, Date deadline, long stepSize, ArrayList<Offer> offerHistory){
		
		this.concessionTime = deadline; 
		this.concessionPrice = reservePrice;
		
		//concession strategy scenario : 1
		if(( tix < T) && (pix < p0)){
			System.out.println("this is strategy 1");
			
			//get random point as the concession point
			this.concessionPrice = pix;
			this.concessionTime = detReg.getCells()[row][col].getCellDeadline();
			
		} //end of concession strategy scenario : 1
		
		//concession strategy scenario : 2
		else if((tix >= T) && (pix <= p0)){
			
			System.out.println("this is strategy 2");
			//case 1: line l2 - regression line which does not travel across the negotiation region
			if( opponentOfferAtTb < reservePrice){
				
				// random value piMin belongs to the range (0 < piMin <<<<< 1)
				this.piMin = (0.10 - ((0.10 - 0.01)* Math.random()));
				
				//set the point which is the next step with the the price approximate to the deadline
				this.concessionPrice = ( reservePrice * ( 1 + piMin ));						 
				this.concessionTime = new Date( t0 + stepSize);				 
				
			}// end of case: 1
			
			//case 2: line l1 - regression line which travels across the negotiation region
			else{						
				
				//set the point which is in the previous step to the deadline and on the regression line
				concessionTime = new Date(deadline.getTime() - stepSize);
				this.concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, row, col, offerHistory,  concessionTime);
			} //end of case: 2
			
		} //end of the concession strategy scenario : 2
		
		//concession strategy scenario : 3
		else if(( tix < T) && (pix > p0)){
			System.out.println("this is strategy 3");
			
			//case 1: line l1 - regression line which travels across the negotiation region
			if( opponentOfferAtT0 < p0){
				
				//calculate the time at the offer p0
				Date timeAtOfferP0 = calculator.GeneratedTimeForGivenFittedOffer(detReg, row, col, offerHistory, p0);
				
				//set the point in which is in the previous step to the p0 price line and on the regression line
				this.concessionTime = new Date(timeAtOfferP0.getTime() - stepSize);
				this.concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, row, col, offerHistory, concessionTime);
				
			}// end of the case 1
			
			//case 2: line l2 - regression line which does not travel across the negotiation region
			else{
				// random value piMin belongs to the range (0 < piMin <<<<< 1)
				this.piMin = (0.20 - ((0.20 - 0.01)* Math.random()));
				
				//set the point which is the next step with the the price approximate to the deadline
				this.concessionPrice = ( p0 * ( 1 - piMin ));						 
				this.concessionTime = new Date(deadline.getTime() - stepSize);
			}
			//end of the case 2
			
		}//end of the concession strategy scenario : 3
		
		//concession strategy scenario : 4
		else if(( tix >= T) && (pix >= p0)){
			System.out.println("this is strategy 4");
			
			//case 1: line l3 - regression line which does not travel across the negotiation region but underneath of the negotiation region
			if( opponentOfferAtT0 < p0){
				System.out.println("line 3");
				// random value piMin belongs to the range (0 < piMin <<<<< 1)
				this.piMin = (0.20 - ((0.20 - 0.01)* Math.random()));
				
				//set the point which is the previous step to the deadline and approximate to the p0
				this.concessionPrice = p0 * (1 - piMin);
				this.concessionTime = new Date(deadline.getTime() - stepSize);
				
			} //end of the case 1
			
			//case 2: line l4 - regression line which does not travel across the negotiation region and goes over the negotiation region
			else if( opponentOfferAtTb < reservePrice ){
				System.out.println("line 4");
				// random value piMin belongs to the range (0 < piMin <<<<< 1)
				this.piMin = (0.10 - ((0.10 - 0.01)* Math.random()));
				
				//set the point which is the next step with the the price approximate to the deadline
				this.concessionPrice = (reservePrice *(1 + piMin) );
				this.concessionTime = new Date( t0 + stepSize); 
				
			}//end of the case 2
			
			//case 3: line l1 - regression line which travels across the negotiation region and goes between the reserve price and the p0 at the deadline
			else if(opponentOfferAtTb <= reservePrice && opponentOfferAtTb < p0){
				System.out.println("line 1");
				//set the point which is in the previous step to the deadline and on the regression line
				this.concessionTime = new Date(deadline.getTime() - stepSize);
				this.concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, row, col, offerHistory, concessionTime);
			
			}//end of the case 3
			
			//case 4: line l2- regression line which travels across the negotiation region and goes underneath the p0 line at the dead line
			else{ 
				System.out.println("line 4");
				//calculate the time at the offer p0
				this.timeAtOfferP0 = calculator.GeneratedTimeForGivenFittedOffer(detReg, row, col, offerHistory, p0);
				System.out.println("time@p0"+this.timeAtOfferP0);
				//set the point in which is in the previous step to the p0 price line interception and on the regression line
				this.concessionTime = new Date(timeAtOfferP0.getTime() - stepSize);
				this.concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, row, col, offerHistory, concessionTime);
			
			} //end of the case 4					
		
		}//end of the concession strategy scenario : 4
		
		if(this.concessionPrice == Double.NaN){
			this.concessionPrice = reservePrice;
		}
	}
	
	
	
	/**
	 *  Generate next offer. 
	 */
	public Offer GenerateNextOffer(DetectionRegion detReg, double reservePrice, Date deadline, int numberOfRows, int numberOfColumns, Offer newOffer, long stepSize, boolean isBuyer){
		return calculator.GenerateNextOffer(detReg, reservePrice,deadline, numberOfRows, numberOfColumns, newOffer, stepSize, isBuyer);
	}
}
