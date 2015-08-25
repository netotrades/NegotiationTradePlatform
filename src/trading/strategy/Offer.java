package trading.strategy;

import java.util.Date;


public class Offer {
	private double offerPrice;
	private Date offerTime;
	private int roundNumber;
	
	public Offer(double price, Date time, int round){
		this.offerPrice = price;
		this.offerTime = time;
		this.roundNumber = round;
	}

	public double getOfferPrice() {
		return offerPrice;
	}

	public void setOfferPrice(double offerPrice) {
		this.offerPrice = offerPrice;
	}

	public Date getOfferTime() {
		return offerTime;
	}

	public void setOfferTime(Date offerTime) {
		this.offerTime = offerTime;
	}

	public int getRoundNumber() {
		return roundNumber;
	}

	public void setRoundNumber(int roundNumber) {
		this.roundNumber = roundNumber;
	}
		
}
