package trading.strategy;

import java.util.Date;


public class ConcessionPoint {
	
	private double concessionPointPrice;
	private Date concessionPointTime;	
	
	public ConcessionPoint(double price, Date time){
		this.concessionPointPrice = price;
		this.concessionPointTime = time;
	}

	public double getConcessionPointPrice() {
		return concessionPointPrice;
	}

	public void setConcessionPointPrice(double concessionPointPrice) {
		this.concessionPointPrice = concessionPointPrice;
	}

	public Date getConcessionPointTime() {
		return concessionPointTime;
	}

	public void setConcessionPointTime(Date concessionPointTime) {
		this.concessionPointTime = concessionPointTime;
	}
	
}
