package trading.strategy;

import java.util.ArrayList;
import java.util.Date;

public class AdaptiveConcessionStrategy {
	
	private Calculator calculator;
	
	public AdaptiveConcessionStrategy(Calculator calculator){
		this.calculator = calculator;
	}
	
	public void FindConcessionPoint(int numberOfRows, int numberOfColumns, DetectionRegion detReg, Offer newOffer, double reservePrice, Date deadline, long stepSize, int numberOfRounds, ArrayList<Offer> offerHistory){
		double concessionPrice = 0.0;
		Date concessionTime = null;
		ConcessionPoint newConcessionPoint;
		
		double RegressionValueAtTb = 0.0;
		double RegressionValueAtT0 = 0.0;
		
		for(int i = 0; i < numberOfRows; i++){
			for(int j = 0; i < numberOfColumns; j++){
				
				RegressionValueAtTb = calculator.GenerateFittedOfferForGivenTime(detReg, i, j, offerHistory, newOffer, numberOfRounds, deadline);
				RegressionValueAtT0 = calculator.GenerateFittedOfferForGivenTime(detReg, i, j, offerHistory, newOffer, numberOfRounds, newOffer.getOfferTime());
				
				if((detReg.getCells()[i][j].getCellDeadline().getTime() < deadline.getTime()) && (detReg.getCells()[i][j].getCellReservePrice() > newOffer.getOfferPrice())){
					
					concessionPrice = detReg.getCells()[i][j].getCellReservePrice();
					concessionTime = detReg.getCells()[i][j].getCellDeadline();
					
				}
				
				else if((detReg.getCells()[i][j].getCellDeadline().getTime() >= deadline.getTime()) && (detReg.getCells()[i][j].getCellReservePrice() >= newOffer.getOfferPrice())){
					
					if(RegressionValueAtTb > reservePrice){
						concessionPrice = reservePrice;
						concessionTime = new Date(deadline.getTime() - stepSize);
					}
					
					else{
						concessionPrice = reservePrice*(0.9 + (0.01 * newOffer.getRoundNumber()));
						concessionTime = new Date(newOffer.getOfferTime().getTime() + stepSize);
					}
					
				}
				
				else if((detReg.getCells()[i][j].getCellDeadline().getTime() < deadline.getTime()) && (detReg.getCells()[i][j].getCellReservePrice() < newOffer.getOfferPrice())){
					
					if(RegressionValueAtT0 > offerHistory.get(offerHistory.size() - 1).getOfferPrice()){
						Date intersectionTime = calculator.GeneratedTimeForGivenFittedOffer(detReg, i, j, offerHistory, newOffer, numberOfRounds, newOffer.getOfferPrice());
						concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, i, j, offerHistory, newOffer, numberOfRounds, new Date(intersectionTime.getTime() - stepSize));
						concessionTime = new Date(intersectionTime.getTime() - stepSize);
					}
					
					else{
						concessionPrice = offerHistory.get(offerHistory.size() - 1).getOfferPrice()*(1 + (0.01)*newOffer.getRoundNumber());
						concessionTime = new Date(deadline.getTime() - stepSize);
					}
					
				}
				
				else if((detReg.getCells()[i][j].getCellDeadline().getTime() >= deadline.getTime()) && (detReg.getCells()[i][j].getCellReservePrice() <= newOffer.getOfferPrice())){
					
					if(RegressionValueAtT0 < offerHistory.get(offerHistory.size() - 1).getOfferPrice()){
						concessionPrice = offerHistory.get(offerHistory.size() - 1).getOfferPrice()*(1 + (0.01)*newOffer.getRoundNumber());
						concessionTime = new Date(deadline.getTime() - stepSize);
					}
					
					else if((RegressionValueAtT0 > offerHistory.get(offerHistory.size() - 1).getOfferPrice()) && (RegressionValueAtT0 < reservePrice)){
						Date intersectionTime = calculator.GeneratedTimeForGivenFittedOffer(detReg, i, j, offerHistory, newOffer, numberOfRounds, newOffer.getOfferPrice());
						concessionPrice = calculator.GenerateFittedOfferForGivenTime(detReg, i, j, offerHistory, newOffer, numberOfRounds, new Date(intersectionTime.getTime() - stepSize));
						concessionTime = new Date(intersectionTime.getTime() - stepSize);
					}
					
					else{
						
						if(RegressionValueAtTb < reservePrice){
							concessionPrice = reservePrice;
							concessionTime = new Date(deadline.getTime() - stepSize);
						}
						else{
							concessionPrice = reservePrice*(0.9 + (0.01 * newOffer.getRoundNumber()));
							concessionTime = new Date(newOffer.getOfferTime().getTime() + stepSize);
						}
					
					}					
				}
				
				newConcessionPoint = new ConcessionPoint(concessionPrice, concessionTime);
				detReg.getCells()[i][j].setConcessionPoint(newConcessionPoint);					
				
			}
		}
	}
	
	public Offer GenerateNextOffer(DetectionRegion detReg, double reservePrice, Date deadline, int numberOfRows, int numberOfColumns, Offer newOffer, long stepSize){
		return calculator.GenerateNextOffer(detReg, reservePrice,deadline, numberOfRows, numberOfColumns, newOffer, stepSize);
	}
}
