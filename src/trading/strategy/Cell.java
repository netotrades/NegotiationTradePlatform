package trading.strategy;

import java.util.ArrayList;
import java.util.Date;


public class Cell {

	//define the boundaries of the cell
	private double CellLowerPrice;	
	private double CellUpperPrice;
	private Date CellLowerDeadline;
	private Date CellUpperDeadline;
	
	//define the random values to the cell
	private double CellReservePrice;
	private Date CellDeadline;
	
	//define probability of the cell
	private double Probability;
	
	//fitted offer list for each cell  
	private ArrayList<Offer> FittedOffers;
	
	//gamma value to express the non-linear correlation
	private double newGammaValue;
 
	private ConcessionPoint concessionPoint;
	private boolean expired;
	 
	
	
	/**
	 *  Constructor 
	 */
	public Cell(double CLP, double CUP, Date CLD, Date CUD, double CRP, Date CD, double PB){
		this.CellLowerPrice = CLP;
		this.CellUpperPrice = CUP;
		this.CellLowerDeadline = CLD;
		this.CellUpperDeadline = CUD;
		this.CellReservePrice = CRP;
		this.CellDeadline = CD;
		this.Probability = PB;
		this.newGammaValue = 1.0;
		this.FittedOffers = new ArrayList<Offer>(); 
		this.concessionPoint = new ConcessionPoint(0.0, null); 
		this.expired = false;
	}
	
	/**
	 *  Set cell lower price 
	 */
	public void setCellLowerPrice(double cellLowerPrice) {
		CellLowerPrice = cellLowerPrice;
	}

	/**
	 *  Set cell upper price
	 */
	public void setCellUpperPrice(double cellUpperPrice) {
		CellUpperPrice = cellUpperPrice;
	}

	/**
	 *  Set cell lower deadline 
	 */
	public void setCellLowerDeadline(Date cellLowerDeadline) {
		CellLowerDeadline = cellLowerDeadline;
	}

	/**
	 *  Set cell upper deadline 
	 */
	public void setCellUpperDeadline(Date cellUpperDeadline) {
		CellUpperDeadline = cellUpperDeadline;
	}

	/**
	 *  Set cell reserve price 
	 */
	public void setCellReservePrice(double cellReservePrice) {
		CellReservePrice = cellReservePrice;
	}

	/**
	 *  Set cell deadline 
	 */
	public void setCellDeadline(Date cellDeadline) {
		CellDeadline = cellDeadline;
	}

	/**
	 *  Set probability 
	 */
	public void setProbability(double probability) {
		Probability = probability;
	}

	/**
	 *  get cell lower price. 
	 */
	public double getCellLowerPrice() {
		return CellLowerPrice;
	}

	/**
	 * get cell upper price. 
	 */
	public double getCellUpperPrice() {
		return CellUpperPrice;
	}

	/**
	 *  get cell lower deadline. 
	 */
	public Date getCellLowerDeadline() {
		return CellLowerDeadline;
	}

	/**
	 *  get cell upper deadline.  
	 */
	public Date getCellUpperDeadline() {
		return CellUpperDeadline;
	}

	/**
	 *  get cell random reserve price correspond to the cell.  
	 */
	public double getCellReservePrice() {
		return CellReservePrice;
	}

	/**
	 * get cell random deadline correspond to the cell.  
	 */
	public Date getCellDeadline() {
		return CellDeadline;
	}

	/**
	 *  get probability correspond to the cell.  
	 */
	public double getProbability() {
		return Probability;
	}
	
	/**
	 *  set a new fitted offer only. 
	 */
	public void setNewFittedOffer(Offer offer){ 
		//System.out.println("previous offer list size= "+ this.FittedOffers.size());
		this.FittedOffers.add(offer);
		//System.out.println("current offer list size= "+ this.FittedOffers.size());
	}
	
	/**
	 *  get fitted offer sets correspond to the cell. 
	 */
	public ArrayList<Offer> getFittedOffers(){
		return this.FittedOffers;
	}
	
	/**
	 *  Set fitted offer sets correspond to the cell. 
	 */
	public void setFittedOffers(ArrayList<Offer> fittedOffers) {
		FittedOffers = fittedOffers;
	}

	/**
	 *  get gamma value correspond to the cell. 
	 */
	public double getNewGammaValue() {
		return newGammaValue;
	}

	/**
	 *  set gamma value correspond to the cell. 
	 */
	public void setNewGammaValue(double newGammaValue) {
		this.newGammaValue = newGammaValue;
	}
 
	 
	public ConcessionPoint getConcessionPoint() {
		return concessionPoint;
	}
 
	public void setConcessionPoint(ConcessionPoint concessionPoint) {
		this.concessionPoint = concessionPoint;
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	} 
	
}
