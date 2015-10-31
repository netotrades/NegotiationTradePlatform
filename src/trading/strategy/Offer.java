package trading.strategy;

import java.util.Date;


public class Offer {
	
	//define offer related attributes
	private double offerPrice;
	private Date offerTime;
	private int roundNumber;
	
	/**
	 *  constructor 1
	 */
	public Offer(double price, Date time, int round){
		this.offerPrice = price;
		this.offerTime = time;
		this.roundNumber = round;
	}
	
	/**
	 *  constructor 2 
	 */
	public Offer(){}
	
	
	/**
	 *  get offer price. 
	 */
	public double getOfferPrice() {
		return offerPrice;
	}

	/**
	 *  set offer price. 
	 */
	public void setOfferPrice(double offerPrice) {
		this.offerPrice = offerPrice;
	}

	/**
	 *  get offer date time. 
	 */
	public Date getOfferTime() {
		return offerTime;
	}

	/**
	 *  set offer date time. 
	 */
	public void setOfferTime(Date offerTime) {
		this.offerTime = offerTime;
	}

	/**
	 * get round number. 
	 */
	public int getRoundNumber() {
		return roundNumber;
	}

	/**
	 *  set round number. 
	 */
	public void setRoundNumber(int roundNumber) {
		this.roundNumber = roundNumber;
	}
		
}
