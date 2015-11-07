package trading.strategy;

import java.util.Date;

public class UtilityCalculation { 
	
	public double calculatePriceUtility(double currentOffer,double startingPrice, double reservePrice){
		double priceUtility = 0.0;
		/*System.out.println("curetn = "+currentOffer);
		System.out.println("starting = "+startingPrice);
		System.out.println("reserve pricee = "+ reservePrice); 
		System.out.println("upper = "+(currentOffer - reservePrice));
		System.out.println("lower = "+(startingPrice - reservePrice));
		*/
		priceUtility = Math.abs((double)(currentOffer - reservePrice)*1.0/(startingPrice - reservePrice));
		/*System.out.println("price utility = "+priceUtility);
		*/
		return priceUtility;
	}
	
	public double calculateTimeUtility(Date currentTime, Date deadline, double betaValue){
		double timeUtility = 0.0;
		/*System.out.println("current Time ="+currentTime);
		System.out.println("deadline = "+deadline);
		System.out.println("beta = "+betaValue);
		System.out.println("upper part = "+ (double)(currentTime.getTime()*1.0/ deadline.getTime()) );
		System.out.println("time powr = "+Math.pow((double)(currentTime.getTime()*1.0/ deadline.getTime()), betaValue));
		*/
		
		timeUtility = Math.abs(1 - Math.pow((double)(currentTime.getTime()*1.0/ deadline.getTime()), betaValue));
		//System.out.println("=======================");
		return timeUtility;
	}
}
