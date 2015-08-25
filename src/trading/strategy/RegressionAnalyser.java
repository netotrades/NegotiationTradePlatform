package trading.strategy;

import java.util.ArrayList;

public class RegressionAnalyser {

	private Calculator calculator;
	
	public RegressionAnalyser(Calculator calculator){
		this.calculator = calculator;
	}
	
	public void Analyse(DetectionRegion detReg, int numberOfRows, int numberOfColumns, ArrayList<Offer> offerHistory, int numberOfRounds, Offer newOffer){
		
		for(int i = 0; i < numberOfRows; i++){
			
			for(int j = 0; j < numberOfColumns; j++){
				
				double offer = calculator.GenerateFittedOffer(detReg, i, j, offerHistory, newOffer, numberOfRounds);
				Offer offerObj = new Offer(offer, newOffer.getOfferTime(), numberOfRounds);
				detReg.getCells()[i][j].setNewFittedOffer(offerObj);
				
				double avgHistory = calculator.FindAverage(offerHistory);
				double avgFitted = calculator.FindAverage(detReg.getCells()[i][j].getFittedOffers());
				double gammaValue = calculator.generateGammaValue(newOffer, avgHistory, numberOfRounds, avgFitted, i, j, detReg);	
				detReg.getCells()[i][j].setNewGammaValue(gammaValue);			
			}
		}
	}
	
}
