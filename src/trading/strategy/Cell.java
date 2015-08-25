package trading.strategy;

import java.util.ArrayList;
import java.util.Date;


public class Cell {

	private double CellLowerPrice;
	private double CellUpperPrice;
	private Date CellLowerDeadline;
	private Date CellUpperDeadline;
	private double CellReservePrice;
	private Date CellDeadline;
	private double Probability;
	private ArrayList<Offer> FittedOffers;
	private double newGammaValue;
	private ConcessionPoint concessionPoint;
	
	public Cell(double CLP, double CUP, Date CLD, Date CUD, double CRP, Date CD, double PB){
		this.CellLowerPrice = CLP;
		this.CellUpperPrice = CUP;
		this.CellLowerDeadline = CLD;
		this.CellUpperDeadline = CUD;
		this.CellReservePrice = CRP;
		this.CellDeadline = CD;
		this.Probability = PB;
		this.newGammaValue = PB;
		this.concessionPoint = new ConcessionPoint(0.0, null);
	}

	public void setProbability(double probability) {
		Probability = probability;
	}

	public double getCellLowerPrice() {
		return CellLowerPrice;
	}

	public double getCellUpperPrice() {
		return CellUpperPrice;
	}

	public Date getCellLowerDeadline() {
		return CellLowerDeadline;
	}

	public Date getCellUpperDeadline() {
		return CellUpperDeadline;
	}

	public double getCellReservePrice() {
		return CellReservePrice;
	}

	public Date getCellDeadline() {
		return CellDeadline;
	}

	public double getProbability() {
		return Probability;
	}
	
	public void setNewFittedOffer(Offer offer){
		FittedOffers.add(offer);
	}
	
	public ArrayList<Offer> getFittedOffers(){
		return FittedOffers;
	}

	public double getNewGammaValue() {
		return newGammaValue;
	}

	public void setNewGammaValue(double newGammaValue) {
		this.newGammaValue = newGammaValue;
	}

	public ConcessionPoint getConcessionPoint() {
		return concessionPoint;
	}

	public void setConcessionPoint(ConcessionPoint concessionPoint) {
		this.concessionPoint = concessionPoint;
	}
		
}
