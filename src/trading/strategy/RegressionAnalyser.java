package trading.strategy;

import java.util.ArrayList;

public class RegressionAnalyser {

	//initialize the calculator
	private Calculator calculator;
	
	/**
	 *  Constructor. 
	 */
	public RegressionAnalyser(Calculator calculator){
		this.calculator = calculator;
	}
	
	/**
	 *  Analyze the offer. 
	 */
	public void Analyse(DetectionRegion detReg, int numberOfRows, int numberOfColumns, ArrayList<Offer> offerHistory,int currentRound, boolean isBuyer){
 		
		if(offerHistory.size()>0){
			
			//For travel through each and every cell
			for(int i = 0; i < numberOfRows; i++){ //outer for loop			
				for(int j = 0; j < numberOfColumns; j++){ //inner for loop	
					
					//System.out.println("\n cell["+i+", "+j+"]\n------------");
					
					//calculate the fitted offer of the cell[i,j]
					ArrayList<Double> fittedOfferDoubleValueArrayList = calculator.GenerateFittedOffer(detReg, i, j, offerHistory);
					
					//System.out.println("\nCalculated fitted offer = "+ offer);
					
					ArrayList<Offer> fittedOfferArrayList = new ArrayList<Offer>();
					
					for(int fittedOfferAt = 0 ; fittedOfferAt < offerHistory.size(); fittedOfferAt++){
						//create a offer object from the calculated fitted offer
						Offer offerObj = new Offer(fittedOfferDoubleValueArrayList.get(fittedOfferAt), offerHistory.get(offerHistory.size()-1).getOfferTime(), (offerHistory.size()-1));
						fittedOfferArrayList.add(fittedOfferAt, offerObj);
					}
					
					//System.out.println("create offer object successfully");
					//System.out.println("offer history array size = "+ offerHistory.size()); 
					//System.out.println("fitted offer array size = "+ detReg.getCells()[i][j].getFittedOffers().size()); 
						
					//add the fitted offer to the cell 				
					//detReg.getCells()[i][j].setNewFittedOffer(offerObj);
					detReg.getCells()[i][j].setFittedOffers(fittedOfferArrayList);
					
					//System.out.println(" New fitted offer array size = "+ detReg.getCells()[i][j].getFittedOffers().size());
					//System.out.println("added offer = "+ detReg.getCells()[i][j].getFittedOffers().get(detReg.getCells()[i][j].getFittedOffers().size()-1).getOfferPrice());
	 
					if( currentRound > 0 ){
						//calculate p_bar : average of the offer history
						double avgHistory = calculator.FindAverage(offerHistory);					
						//System.out.println("history offers average = "+ avgHistory);
						
						//calculate p_hat_bar : average of the fitted offer history
						double avgFitted = calculator.FindAverage(detReg.getCells()[i][j].getFittedOffers());
						//System.out.println("fitted offers average = "+ avgFitted);
						
						//calculate the gamma value
						double gammaValue = calculator.generateGammaValue(offerHistory, avgHistory, avgFitted, i, j, detReg , numberOfRows, numberOfColumns);	
						
						//System.out.println("gamma value = "+ gammaValue);
						
						if(gammaValue != 0.0 && gammaValue != Double.POSITIVE_INFINITY && gammaValue != Double.NEGATIVE_INFINITY){
							detReg.getCells()[i][j].setNewGammaValue(gammaValue);
						}
						else{
							gammaValue = ( 1 / (numberOfRows* numberOfColumns));
							detReg.getCells()[i][j].setNewGammaValue(gammaValue);
						}
						
						//System.out.println("gamma value = "+ gammaValue);
						//System.out.println("added gamma value : "+ detReg.getCells()[i][j].getNewGammaValue());	
					}
					
				} //end of the inner for loop
			}// end of the outer for loop
		}
		

		
	}
	
}
