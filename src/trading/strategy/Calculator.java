package trading.strategy; 

import java.util.ArrayList;
import java.util.Date;

/**
 *  All the possible calculations are done in this calculator class. 
 */
public class Calculator {
	
	/**
	 *  Calculate average of the offers in the array list.
	 *  @param offers: array list of offers. 
	 *   @return avg: average offer prices value of the given offer list.
	 */
	public double FindAverage(ArrayList<Offer> offers) {
		
		//initialize the parameters
		double sum = 0.0;
		double avg = 0.0;
		
		//if the denominator is a non zero value
		if(offers.size()> 0){
			
			for (int i = 0; i < offers.size(); i++) { //start of the for loop
				
				sum += offers.get(i).getOfferPrice(); 
				
			}// end of the for loop
			
			avg = ((sum * 1.0) / offers.size());
		}		

		//return average value of the given offer list
		return avg;
	}

	/**
	 *  Generate fitted offer for a given cell.
	 *  @param detReg: array list of offers. 
	 *  @param row: row number of the cell.
	 *  @param col: column number of the cell.
	 *  @param offerHistory: offerHistory of the opponent.
	 *  @return fittedOffer: return fitted offer for the given cell. 
	 */
	public ArrayList<Double> GenerateFittedOffer(DetectionRegion detReg, int row, int col, ArrayList<Offer> offerHistory) {
		
		ArrayList<Double> fittedOfferList = new ArrayList<Double>();	
		
		//initialize the fitted offer
		double fittedOffer = 0.0;
		
		//initialize parameters
		double p0 = 0 ; double pix = 0 ;long tix =0;		 
		
		if(offerHistory.size() > 0){
			
			p0 = offerHistory.get(0).getOfferPrice();
			pix = detReg.getCells()[row][col].getCellReservePrice();
			tix = detReg.getCells()[row][col].getCellDeadline().getTime();
			
			
			
			//System.out.println("\np0 = " + p0);
			//System.out.println("pix = " + pix);
			//System.out.println("tix = " + tix);
			//System.out.println("b = " + b);	
			
			if(offerHistory.size()== 1){
				fittedOffer = p0;
				fittedOfferList.add(0,fittedOffer);
			}
			//value b and fitted offer is calculated 2nd step onwards
			else{	
				int fittedOfferAtRound;
 			
				for(fittedOfferAtRound = 0; fittedOfferAtRound < offerHistory.size(); fittedOfferAtRound++){
					
					//calculate the fitted offer 
					if( tix != 0 && tix != Double.POSITIVE_INFINITY && tix != Double.NEGATIVE_INFINITY){
						// calculate the value 'b' 
						double b = this.GenerateBValue(detReg, row, col, offerHistory);
						long t = offerHistory.get(offerHistory.size()-1).getOfferTime().getTime();
						
						fittedOffer = (double)(p0 + ((pix - p0)* (Math.pow(Math.abs((t * 1.0 / tix)) , b))));
					}
					else{
						fittedOffer = p0;
					}
					
					fittedOfferList.add(fittedOfferAtRound, fittedOffer);
				}
									
			}//end of else
		}//end of if clause	
		
			
			
		//return calculated fitted offer 
		return fittedOfferList;
	}
	
	/**
	 *  Generate beta or "b" value for the given cell.
	 *  @param detReg: array list of offers. 
	 *  @param row: row number of the cell.
	 *  @param col: column number of the cell.
	 *  @param offerHistory: offerHistory of the opponent.
	 *  @return b: return value b. 
	 */
	private double GenerateBValue(DetectionRegion detReg, int row, int col, ArrayList<Offer> offerHistory) { 
		
		//initialize the parameters
		double tpSum = 0; double t2Sum = 1.0; double b = 0;
		double p0 = 0; double pi = 0; double pix = 0;
		double piStar = 1; double tiStar = 1;
		long ti = 0; long tix = 0;
		
		//sometimes b is needed when there is one offer or no offer
		if( offerHistory.size()== 0 || offerHistory.size() == 1 ){
			b = 1;
		}		//b value cannot be calculated if there are no more than or equal to 2 elements in the history
		else{ 
			
			for (int k = 1; k < offerHistory.size(); k++) {//for loop start
				
				p0 = offerHistory.get(0).getOfferPrice();
				pi = offerHistory.get(k).getOfferPrice();
				pix = detReg.getCells()[row][col].getCellReservePrice();
				
				//calculate pi star value
				if((p0 - pix)!= 0 && (p0 - pix) != Double.POSITIVE_INFINITY && (p0 - pix) != Double.NEGATIVE_INFINITY){
					if((p0 - pi)!= 0 && (p0 - pi) != Double.POSITIVE_INFINITY && (p0 - pi) != Double.NEGATIVE_INFINITY){
						piStar = (double)Math.log(Math.abs((p0 - pi)* 1.0 /(p0 - pix)));
						
						//System.out.println("pistar calculated using the formula.......");
						
					}else{
						piStar = 1.0;
					}				
				}
				else{
					piStar = 1.0;
				}
				
				//System.out.println("method for loop");
				//System.out.println("k = "+ k);
				//System.out.println("p0 = "+ p0);
				//System.out.println("pi = "+ pi);
				//System.out.println("pix = "+ pix);
				//System.out.println("pistar = "+ piStar); 
				
				ti = offerHistory.get(k).getOfferTime().getTime();
				tix = detReg.getCells()[row][col].getCellDeadline().getTime();
				
				//System.out.println("ti = "+ ti);
				//System.out.println("tix = "+ tix); 
				//System.out.println("ti / tix = "+ (ti * 1.0 / tix));
				
				//calculate tStar value
				if( ti!=0 && tix != 0 && tix!= Double.POSITIVE_INFINITY && tix!= Double.NEGATIVE_INFINITY){
					if(ti!=0 && tix != 0 && tix!= Double.POSITIVE_INFINITY && tix!= Double.NEGATIVE_INFINITY){
						
						tiStar = (double) Math.log(Math.abs(ti * 1.0 / tix));
						
						//System.out.println("tistar calculated using the formula............");
						
					}else{
						tiStar = 1.0;
					}					
				}else{
					tiStar = 1.0;
				}
				
				//System.out.println("tistar = "+ tiStar );
				
				//cumulative sum for the numerator of b calculation 
					tpSum += (double) tiStar * piStar;					
				
					//System.out.println("tpsum = "+ tpSum );
				
				//cumulative sum for the denominator of b calculation
				if(tiStar!=0 && tiStar!= Double.POSITIVE_INFINITY && tiStar!= Double.NEGATIVE_INFINITY){
					t2Sum += Math.pow(Math.abs(tiStar), 2);
				}else{
					t2Sum = 1;
				}
				
				//System.out.println("t2Sum = "+ t2Sum );
				
			}//end of the for loop 
			
			//System.out.println("Final tpSum ="+ tpSum);
			//System.out.println("Final t2Sum ="+ t2Sum);
			
			//if the denominator is non zero calculate the b value
			if ( t2Sum != 0 && t2Sum != Double.POSITIVE_INFINITY && t2Sum != Double.NEGATIVE_INFINITY ){				
				if ( tpSum != 0 && tpSum != Double.POSITIVE_INFINITY && tpSum != Double.NEGATIVE_INFINITY ){
					
					//System.out.println("(tpSum * 1.0 / t2Sum)"+(tpSum * 1.0 / t2Sum));
					
					b = (double)(tpSum * 1.0 / t2Sum);
				}else{
					b= 1;
				}  
			}else{
				b = 1;
			}
			
			
			if(b== Double.POSITIVE_INFINITY||b== Double.NEGATIVE_INFINITY){
				b = 1.0;
			}
			
			//System.out.println("b = "+ b);
			
		}// end of the if clause
		 
		return b; //return value of b
	}

	/**
	 *  Generate gamma value for the given cell: 
	 *  To check the non-linear correlation between historical offers and the fitted offers.
	 *  @param offerHistory: offerHistory of the opponent. 
	 *  @param avgHistory: average value of the opponent history offers.
	 *  @param avgFitted: average value of the opponent fitted offers.
	 *  @param row: row number of the given cell.
	 *  @param col: column number of the given cell. 
	 *  @param detReg: detection region.
	 *  @param numberOfRows: number of the rows in the detection region.
	 *  @param numberOfColumns: number of the columns of the detection region .
	 *  @return foundGamma: calculated gamma value. 
	 */
	public double generateGammaValue(ArrayList<Offer> offerHistory, double avgHistory, double avgFitted, int row,
			int col, DetectionRegion detReg,  int numberOfRows, int numberOfColumns) {

		//initialize the parameters
		double sumUP = 0.0;	double sumDown1 = 0.0; 	double sumDown2 = 0.0;	double foundGamma = 1.0;
		double pi = 0.0; double piHat=0.0;  
		
		//cell should have fitted offers corresponds to each history
		if(detReg.getCells()[row][col].getFittedOffers().size() == offerHistory.size()){ 
			
			//gamma can only calculate if the offer size is greater than 1
			if(offerHistory.size()>1){
				
				for (int round = 1; round < (offerHistory.size()); round++) {//for loop start

					pi = offerHistory.get(round).getOfferPrice();
					piHat = detReg.getCells()[row][col].getFittedOffers().get(round).getOfferPrice();
					
	/*				System.out.println(" pi ="+ pi);
					System.out.println("piHat ="+ piHat);
					System.out.println(" avgHistory ="+ avgHistory);
					System.out.println("avgFitted ="+ avgFitted);*/
					
					//get production of (offer and average offer history difference) and (fitted offer and average fitted offer history difference)
					sumUP += ((pi - avgHistory)	* (piHat - avgFitted));
					
					//System.out.println("sumUP ="+ sumUP);
					
					//sum of square of  (offer and average offer history difference)
					sumDown1 += Math.pow(Math.abs(pi - avgHistory), 2);
					//System.out.println("sumDown1 ="+ sumDown1);
					
					//sum of square of (fitted offer and average fitted offer history difference)
					sumDown2 += Math.pow(Math.abs(piHat - avgFitted), 2);
					//System.out.println("sumDown2 ="+ sumDown2);
					
				}//end of the for loop
				
				//calculate gamma value
				if(Math.sqrt(Math.abs(sumDown1 * sumDown2))> 0){
					foundGamma = (double)(sumUP * 1.0 / Math.sqrt(Math.abs(sumDown1 * sumDown2)));
				}
				else{
					foundGamma = 1.0;
				}
				
			}//end of else if clause
			
		} 
		
		//System.out.println("foundGamma ="+ foundGamma);
		
		// return calculated gamma value
		return foundGamma;
	}
	
	/**
	 *  Calculate fitted offer for the given time. 
	 *  @param detReg: array list of offers. 
	 *  @param row: row number of the cell.
	 *  @param col: column number of the cell.
	 *  @param offerHistory: offerHistory of the opponent. 
	 *  @param time: given time.
	 *  @return fittedOffer: return fitted offer. 
	 */
	public double GenerateFittedOfferForGivenTime(DetectionRegion detReg, int i, int j, ArrayList<Offer> offerHistory,
			Date time) {
		
		//System.out.println("This is the generate fitted offer for the given time method of the cell= "+i+","+j);
		
		//calculate the b value : here b is referred to as the 'betaIHat' value
		double betaIHat = GenerateBValue(detReg, i, j, offerHistory);

		//System.out.println("\n----------beta i hat = "+ betaIHat);
		
		double p0 = offerHistory.get(0).getOfferPrice();
		double pix = detReg.getCells()[i][j].getCellReservePrice();
		long ti = time.getTime(); 
		long tix = detReg.getCells()[i][j].getCellDeadline().getTime();
		double fittedOffer = 0.0;
		
		/*System.out.println(" p0 ="+ p0);
		System.out.println("pix ="+ pix);
		System.out.println(" ti ="+ ti);
		System.out.println("tix ="+ tix);*/
		
		//calculate the fitted offer using the b value
		if(tix != 0 && tix != Double.POSITIVE_INFINITY && tix != Double.NEGATIVE_INFINITY){
			if(betaIHat != 0 && betaIHat != Double.POSITIVE_INFINITY && betaIHat != Double.NEGATIVE_INFINITY){
				fittedOffer = (p0 + (pix - p0) * Math.pow(Math.abs (ti * 1.0 / tix) , betaIHat));
			}else{
				
				betaIHat =1.0;
				fittedOffer = (p0 + (pix - p0) * Math.pow(Math.abs (ti * 1.0 / tix) , betaIHat));
			}
		}else{
			fittedOffer = p0;
		}
		
		//System.out.println("@ generated fitteded offer for given time fitted offer= "+ fittedOffer+"\n\n");
		return fittedOffer;
	}
	
	/**
	 *  Generate time for the given fitted offer.
	 *  @param detReg: array list of offers. 
	 *  @param row: row number of the cell.
	 *  @param col: column number of the cell.
	 *  @param offerHistory: offerHistory of the opponent. 
	 *  @param offerPrice: given offer price.
	 *  @return time: return time. 
	 */
	public Date GeneratedTimeForGivenFittedOffer(DetectionRegion detReg, int row, int col, ArrayList<Offer> offerHistory,
			 double offerPrice) {

		double b = GenerateBValue(detReg, row, col, offerHistory);
		double p0 = offerHistory.get(0).getOfferPrice();
		double pix = detReg.getCells()[row][col].getCellReservePrice();
		long tix = detReg.getCells()[row][col].getCellDeadline().getTime();
		Date time = new Date();
		
		//calculate the time correspond to the given time using the offer calculation formula
		if((pix - p0)!= 0.0 && (pix - p0)!= Double.POSITIVE_INFINITY &&(pix - p0)!= Double.NEGATIVE_INFINITY){
			if(b != 0.0 && b != Double.POSITIVE_INFINITY && b != Double.NEGATIVE_INFINITY){
				time = new Date((long)(Math.pow((Math.abs(offerPrice - p0)/ (pix - p0)), 1 / b) * (tix)));
			}
		}
		//System.out.println("@ generate time for given fitted offer method "+time);
		return time;
	}

	/**
	 *  Calculate log of base.
	 *  @param base: base for calculate log value. 
	 *  @param num: number for calculate log value.
	 *  @return log: return calculated log for given base and the number.  
	 */
	private double logOfBase(double base, double num) {
		
		if(num!=0 && base!=0){
			return Math.log(Math.abs(num)) / Math.log(Math.abs(base)); 
		}
		else{
			return 1.0;
		}
		
	}

	/**
	 *  Generate next offer.
	 *  @param detReg: detection region. 
	 *  @param reservePrice: reserve price.
	 *  @param deadline: deadline. 
	 *  @param numberOfRows: number Of Rows in the detection region.
	 *  @param numOfColumns: number of columns in the detection region. 
	 *  @param prevPrice: previous offer price.
	 *  @param stepSize: negotiation step size for a round. 
	 *  @param isBuyer: whether buyer or not.
	 *  @param offerHistory: offerHistory of the opponent. 
	 *  @return nextOffer: counter offer for the opponent.  
	 */
	public Offer GenerateNextOffer(Date currentTime, DetectionRegion detReg, double reservePrice, Date deadline, int numberOfRows,
			int numberOfColumns, Offer prevOffer, long stepSize, boolean isBuyer, ArrayList<Offer> offerHistory) {

		double base = 0.0; 	double value = 0.0; double BetaHat = 0.0; 	double Sum = 0.0; double BetaBar = 0.0;
		double offerPrice = 0.0; double p0 = 0.0;  long t = 0; long t0; double timeRatio = 0.0; 
		 
		
		Offer nextOffer;
		//buyer's oth round
		if(offerHistory.size()==1 && isBuyer){
			//System.out.println("Buyer's history size is 1");
			nextOffer = new Offer(prevOffer.getOfferPrice(),currentTime,0);
		}
		else if(offerHistory.size()>0 ){
			
			//Traveling through each and every cell
			for (int i = 0; i < numberOfRows; i++) { //outer for loop
				for (int j = 0; j < numberOfColumns; j++) { //inner for loop
					 
					//for the sum calculation only used valid cells in the negotiation region
					if(!detReg.getCells()[i][j].isExpired()){
						
						//base value can be 0 if there is an error
						base = this.calculateBase(detReg, i, j, prevOffer, deadline);
						
						//value also can be zero if there is an error
						value = this.calculateValue(detReg, prevOffer, i, j, reservePrice);
						
						//calculate beta hat value
						BetaHat = this.logOfBase(base, value); //System.out.println("BetaHat"+BetaHat);
						
	 					Sum += (double)(detReg.getCells()[i][j].getProbability() / (1 + BetaHat)); //
	 					//System.out.println("Sum"+Sum);
					} 
					//System.out.println();
				}//end of inner for loop
			}//end of outer for loop
			
			
			//calculate the beta bar value
			if(Sum != 0 && Sum!= Double.POSITIVE_INFINITY && Sum != Double.NEGATIVE_INFINITY){
				BetaBar = (double)((1 / Sum) - 1);
				//System.out.println("beta bar value = "+BetaBar);
				BetaBar= Math.abs(BetaBar);
				//BetaBar = 0.8;
				
			}
			else{
				BetaBar = 1.0;
			}
			
			String agent="";
			if(isBuyer){
				agent = "Buyer: ";
			}
			else{
				agent = "Seller: ";
			} 
			//System.out.println(agent+Math.abs(BetaBar)); 
			
			
			p0 = prevOffer.getOfferPrice();
			t = currentTime.getTime(); //current time
			t0 = prevOffer.getOfferTime().getTime();
			
			//calculate time ratio
			if(deadline.getTime()!= t0){
				timeRatio = ((double) (t - t0)/ (deadline.getTime() - t0));
			}
			else{
				timeRatio = 0.0;
			}
			/*System.out.println("sum = "+ Sum);
			System.out.println("betabar "+BetaBar);
			System.out.println("time ratio "+timeRatio);
			System.out.println("power "+(Math.pow(timeRatio, BetaBar)));
			System.out.println("po + "+((reservePrice - p0)* (Math.pow(timeRatio, BetaBar))));*/
			//calculate the next offer
			if(BetaBar!= Double.POSITIVE_INFINITY && BetaBar != Double.NEGATIVE_INFINITY){
				if(timeRatio!= Double.POSITIVE_INFINITY && timeRatio != Double.NEGATIVE_INFINITY){
					 if((p0 < reservePrice) && isBuyer){
						offerPrice = (p0 + ((reservePrice - p0)* (Math.pow(timeRatio, BetaBar))));
						//System.out.println("Buyer offer cal = "+offerPrice);
					}
					else if((p0 > reservePrice) && !isBuyer){
						offerPrice = (p0 + ((reservePrice - p0)* (Math.pow(timeRatio, BetaBar))));
						//System.out.println("Seller offer cal = "+offerPrice);
					}
					else{
						offerPrice = reservePrice;
						//System.out.println("p0 exceed the reserve price "+offerPrice);
					}
				}
				else{
					offerPrice = reservePrice;
					//System.out.println("error in time ratio "+timeRatio);
				}
			} 
			else{
				offerPrice = reservePrice;
				//System.out.println("error in time beta bar "+BetaBar);
			} 
			
			double opponentLastOffer = offerHistory.get((offerHistory.size()-1)).getOfferPrice();
			
			//if generated offer is exceed the opponents last offer
			if(isBuyer && offerPrice >= opponentLastOffer ){
				//set the opponent last offer as the next offer
				offerPrice = opponentLastOffer; //System.out.println("sellers last offer is lower than than the buyers generated value");
			}
			else if(!isBuyer && offerPrice <= opponentLastOffer){
				offerPrice = opponentLastOffer;
			}
			
			
			//create offer object
			nextOffer = new Offer(offerPrice,currentTime, (prevOffer.getRoundNumber() + 1));
			System.out.println("1:" +nextOffer.getOfferPrice());
		}
		else{
			nextOffer = new Offer(prevOffer.getOfferPrice(),currentTime, 0);
			System.out.println("2:" +nextOffer.getOfferPrice());
		} 
		return nextOffer;
	}
	
	/**
	 *  Generate next offer.
	 *  @param detReg: detection Region. 
	 *  @param row: row number of the given cell.
	 *  @param col: column number of the given cell. 
	 *  @param prevOffer: previous offer.  
	 *  @param deadline: deadline.
	 *  @return base: calculated base value
	 */
	public double calculateBase(DetectionRegion detReg, int row, int col , Offer prevOffer, Date deadline ){
		
		double base =0.0; long tp = 0; long t0 = 0 ; 
		
		tp = detReg.getCells()[row][col].getConcessionPoint().getConcessionPointTime().getTime();
		t0 = prevOffer.getOfferTime().getTime();
		/*System.out.println("tp"+ tp);
		System.out.println("t0"+ t0);*/
		
		if(deadline.getTime() != t0 && tp != t0){
			base = ((double)( tp- t0 ) / (deadline.getTime() - t0)); //System.out.println("base = "+ base);
		}
		else{
			base = 1.0;
		}	
		
		return base;
	}
	
	/**
	 *  Generate next offer.
	 *  @param detReg: detection Region.    
	 *  @param prevOffer: previous offer. 
	 *  @param row: row number of the given cell.
	 *  @param col: column number of the given cell. 
	 *  @return base: calculated base value 
	 */
	public double calculateValue(DetectionRegion detReg, Offer prevOffer, int row, int col, double reservePrice){
		double value = 0.0; double p0 = 0.0; double pp = 0.0;
		
		p0 = prevOffer.getOfferPrice();
		pp = detReg.getCells()[row][col].getConcessionPoint().getConcessionPointPrice();
		/*System.out.println("cell ;"+row+","+col);
		System.out.println("p0 = "+ p0);
		System.out.println("pp = "+ pp);*/
		
		if(p0!=reservePrice && p0!= pp){
			value =(double) ((p0 - pp)/ (p0 - reservePrice));
			//System.out.println("value ="+value);
		}
		else{
			value = 1.0;
		}
		//System.out.println();
		return value;
	}
	
	/**
	 *  Generate pretended deadline.
	 *  @param detReg: detection Region.    
	 *  @param numberOfRows: total number of rows. 
	 *  @param numberOfColumns: total number of columns.
	 *  @param newOffer: new offer. 
	 *  @return pretenededDeadline: deadline 
	 */
	public Date GeneratePretendedDeadline(DetectionRegion detReg, int numberOfRows, int numberOfColumns, Date deadline){
		
		//estimated deadline of the opponent
		Date estimatedDeadline = null;
		Date pretendedDeadline = null; 
		double initialRisk = 0.0;
		Date currentTime = new Date();
		 
		//get the estimated deadline of the opponent
		estimatedDeadline = this.opponentEstimatedDeadline(detReg, numberOfRows, numberOfColumns, currentTime);
		//System.out.println("estimated deadline of the opponent= "+ estimatedDeadline);
		
		//calculate the initial risk
		initialRisk = this.calculateRisk(estimatedDeadline, deadline, currentTime);
		 
		//if risk cannot be calculated
		if(initialRisk == 1.0){
			
			//set pretended deadline as the original deadline
			pretendedDeadline = deadline;
			//System.out.println("Initial risk = "+initialRisk+" , pretended deadline = "+pretendedDeadline);
		}
		//if the deadline of the opponent is higher- disadvantage
		else if(initialRisk > 0){	
			
			for(double risk = 0.1 ; risk < initialRisk; risk+=0.1){
				//System.out.println("\n nw risk is taken as "+risk);
				Date calculatedPret_Deadline = this.calculatePretendedDeadlineForGivenRisk(estimatedDeadline, currentTime, risk);
				
				if(calculatedPret_Deadline!= null && calculatedPret_Deadline.getTime() > currentTime.getTime() && calculatedPret_Deadline.getTime()<= estimatedDeadline.getTime() && calculatedPret_Deadline.getTime() <= deadline.getTime() ){
					pretendedDeadline = calculatedPret_Deadline;
					break;
				}
			}//end of for loop
			
			if(pretendedDeadline == null){
				pretendedDeadline = deadline;
			}
		}
		//if the deadline of the opponent is lower or equal to zero- advantage
		else{
			
			for(double risk = 0.1 ; risk < 1; risk+=0.1){
				
				//System.out.println("\n nw risk is taken as "+risk);
				Date calculatedPret_Deadline = this.calculatePretendedDeadlineForGivenRisk(estimatedDeadline, currentTime, risk);
				
				if(calculatedPret_Deadline!= null && calculatedPret_Deadline.getTime() > currentTime.getTime() && calculatedPret_Deadline.getTime()<= estimatedDeadline.getTime() && calculatedPret_Deadline.getTime() <= deadline.getTime() ){
					pretendedDeadline = calculatedPret_Deadline;
					break;
				}
			}//end of for loop
			
			if(pretendedDeadline == null){
				pretendedDeadline = estimatedDeadline;
			}
		}
		 
		return pretendedDeadline;
	}
	
	/**
	 *  get opponent estimated deadline.
	 *  @param detReg: detection Region.    
	 *  @param numberOfRows: total number of rows. 
	 *  @param numberOfColumns: total number of columns. 
	 *  @return pretenededDeadline: deadline 
	 */
	public Date opponentEstimatedDeadline(DetectionRegion detReg, int numberOfRows, int numberOfColumns, Date currentTime){
		
		double maxProbability = 0.0;
		Date estimatedDeadlineOfOpponent = null;
		
		for(int i = 0; i < numberOfRows; i++){
			for(int j = 0; j < numberOfColumns; j++){
				if((detReg.getCells()[i][j].getProbability() > maxProbability) && !detReg.getCells()[i][j].isExpired() && (detReg.getCells()[i][j].getCellUpperDeadline().getTime()>currentTime.getTime())){
					maxProbability = detReg.getCells()[i][j].getProbability();
					estimatedDeadlineOfOpponent = detReg.getCells()[i][j].getCellDeadline();
				}//end of if				
			}//end of inner for loop
		}//end of outer for loop
		
		return estimatedDeadlineOfOpponent;
	}
	
	/**
	 *  calculate the risk.
	 *  @param opponentDeadline: opponent deadline.    
	 *  @param deadline: deadline.  
	 *  @return risk: risk of dealing with the opponent 
	 */	
	 public double calculateRisk(Date opponentDeadline, Date deadline, Date currentTime){
		 double risk; 
		 
		 //risk can be calculated
		 if((opponentDeadline.getTime() > currentTime.getTime()) && (deadline.getTime() > currentTime.getTime())&& (deadline.getTime()>0) ){
			 
				risk=((opponentDeadline.getTime() - deadline.getTime())* (currentTime.getTime()))/(((0.5*(opponentDeadline.getTime()+deadline.getTime()))-currentTime.getTime())* deadline.getTime());
				//System.out.println("calculated initial risk = " + risk);
		 }
		else{ 
			risk = 1.0;
		}
		 
		 return risk;
	 }
	 
	 /**
		 *  calculate the pretendedDeadline for given risk.
		 *  @param opponentDeadline: opponent deadline.    
		 *  @param deadline: deadline.  
		 *  @return risk: risk of dealing with the opponent 
		 */	
	public Date calculatePretendedDeadlineForGivenRisk(Date opponentDeadline, Date currentTime, double risk){
		Date pretendedDeadline = null;
		double a = 0,b =0 ,c=0 ;
		
		// calculate a
		a = (0.5 * risk);
		//System.out.println("a = "+a);
		
		//calculate b
		b = ((0.5* risk * opponentDeadline.getTime()) + ((1+risk)* currentTime.getTime()));
		//System.out.println("b = "+b);
		
		//calculate c
		c= opponentDeadline.getTime()* (-1.0);
		//System.out.println("c = "+c);
				
		ArrayList<Double> roots = this.calculateTheRoots(a, b, c); 
		
		//if there are roots
		if(roots.get(0) != null && roots.get(1) != null){
			
			//if the 1st root has a positive value which is higher than the current time
			if( roots.get(0)> currentTime.getTime() && roots.get(1)> currentTime.getTime()){
				
				if(roots.get(0) < opponentDeadline.getTime() && roots.get(1) < opponentDeadline.getTime()){
					if(roots.get(0) >=  roots.get(1)){
						pretendedDeadline = new Date(roots.get(0).longValue());	
					}
					else{
						pretendedDeadline = new Date(roots.get(1).longValue());
					}
					
				}
				else if(roots.get(0) < opponentDeadline.getTime()){
					pretendedDeadline = new Date(roots.get(0).longValue());
				}
				else{
					pretendedDeadline = new Date(roots.get(1).longValue());
				}
			}			
			//if the 1st root has a positive value which is higher than the current time
			else if( roots.get(0)> currentTime.getTime() && roots.get(1)<= currentTime.getTime()){
				pretendedDeadline = new Date(roots.get(0).longValue());
			}
			//if the 1st root has a positive value which is higher than the current time
			else if( roots.get(1)> currentTime.getTime() && roots.get(0)<= currentTime.getTime()){
				pretendedDeadline = new Date(roots.get(1).longValue());
			}
			else{
				pretendedDeadline = null;
			}
			
		}
		else{
			pretendedDeadline = null;
		}
		
		return pretendedDeadline;
	}
	
	/**
	 *  calculate the roots for given quadratic equation.
	 *  @param a: coefficient of X^2.    
	 *  @param b: coefficient of x.  
	 *  @return c: coefficient of x^0 
	 */
	public ArrayList<Double> calculateTheRoots(double a, double b, double c){
		
		//return array list 
		ArrayList<Double> roots = new ArrayList<Double>(2);
		
		//calculate delta value
		double delta = (Math.pow(a, 2) -(4 * a* c));
		
		if(delta>= 0 && a != 0){
			//calculate roots
			double root1 = ((-1*b) - Math.pow(Math.abs(delta), (1/2)))/(2*a);
			double root2 = ((-1*b) + Math.pow(Math.abs(delta), (1/2)))/(2*a);
			
			//add roots to the return array list
			roots.add(0, root1);
			roots.add(1, root2);
		}
		else{
			//add null values to the return array list
			roots.add(0, null);
			roots.add(1, null);
			//System.out.println("Warning: Complex roots are available for the given quadratic eqation!.......");
		}
		
		return roots;
	}
	
}
