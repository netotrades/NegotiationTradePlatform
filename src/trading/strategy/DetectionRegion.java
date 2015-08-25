package trading.strategy;

import java.util.Date;


public class DetectionRegion {
	
	private double LowerReservePrice;
	private double UpperReservePrice;
	private Date LowerDeadline;
	private Date UpperDeadline;
	private Cell[][] cells;
	private int numberOfCells;

	public DetectionRegion(double LRP, double URP, Date LD, Date UD){
		this.LowerReservePrice = LRP;
		this.UpperReservePrice = URP;
		this.LowerDeadline = LD;
		this.UpperDeadline = UD;
	}

	public double getLowerReservePrice() {
		return LowerReservePrice;
	}

	public double getUpperReservePrice() {
		return UpperReservePrice;
	}

	public Date getLowerDeadline() {
		return LowerDeadline;
	}

	public Date getUpperDeadline() {
		return UpperDeadline;
	}
	
	public void setCells(Cell[][] cells){
		this.cells = cells;
	}
	
	public Cell[][] getCells() {
		return cells;
	}

	public int getNumberOfCells() {
		return numberOfCells;
	}

	public void setNumberOfCells(int numberOfCells) {
		this.numberOfCells = numberOfCells;
	}
	
		
}
