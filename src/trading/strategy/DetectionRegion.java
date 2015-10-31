package trading.strategy;

import java.util.Date;

public class DetectionRegion {
	
	//define boundaries of the detection region
	private double LowerReservePrice;
	private double UpperReservePrice;
	private Date LowerDeadline;
	private Date UpperDeadline;
	
	//define cells to divide the detection region 
	private Cell[][] cells;
	
	//total number of cells
	private int numberOfCells;

	/**
	 *  Constructor 
	 */
	public DetectionRegion(double LRP, double URP, Date LD, Date UD, int numOfRows, int numOfColumns){
		this.LowerReservePrice = LRP;
		this.UpperReservePrice = URP;
		this.LowerDeadline = LD;
		this.UpperDeadline = UD ;
		this.cells = new Cell [numOfRows][numOfColumns];
		this.numberOfCells = numOfRows * numOfColumns;
		this.initializeCells(LRP, URP, LD, UD, numOfRows, numOfColumns);
	}
	
	/**
	 *  Initialize the cells in the detection region. 
	 */
	public void initializeCells(double LRP, double URP, Date LD, Date UD, int numOfRows, int numOfColumns){ 
		
		for (int i = 0; i < numOfRows; i++) {
			
			double cellLowerPrice = LRP + (((URP - LRP) / numOfRows) * i);
			double cellUpperPrice = LRP + (((URP - LRP) / numOfRows) * (i+1)); 
			
			for (int j = 0; j < numOfColumns; j++) {				   
				
				Date cellLowerDate = new Date( LD.getTime()	+ (((UD.getTime() - LD.getTime())/ (numOfColumns)) * j));
				Date cellUpperDate = new Date( LD.getTime()	+ (((UD.getTime() - LD.getTime())/ (numOfColumns)) * (j+1)));
				
				//set middle point of the cell price region as the cell reserve price
				double cellReservePrice = (cellUpperPrice + cellLowerPrice) / 2;
				
				//set middle point of the cell time region as the cell deadline
				Date cellDeadline = new Date((cellUpperDate.getTime() + cellLowerDate.getTime()) / 2);
				
				//equally distributed the probability among all the cells
				double cellProbability = (1.0 / this.numberOfCells);

				//add the cell into the cell array
				cells[i][j] = new Cell(cellLowerPrice, cellUpperPrice, cellLowerDate, cellUpperDate, cellReservePrice,
						cellDeadline, cellProbability);
				
				//print cells
				//this.cellToString(i, j);
				 
			} //end of inner for loop
		}// end of the outer for loop		
	}
	
	/**
	 *  Cells to string method. 
	 */
	public void cellToString(int row, int column){
		System.out.println("Cell["+row+","+column+"] : "+ cells[row][column].getCellLowerPrice()+", "+ cells[row][column].getCellUpperPrice()+", "+cells[row][column].getCellLowerDeadline()+", "+ cells[row][column].getCellUpperDeadline()+", "+ cells[row][column].getCellReservePrice()+", "+
				cells[row][column].getCellDeadline()+", "+cells[row][column].getProbability());		
	}

	/**
	 *  Get the lower reserve price. 
	 */
	public double getLowerReservePrice() {
		return LowerReservePrice;
	}

	/**
	 *  Get the upper reserve price. 
	 */
	public double getUpperReservePrice() {
		return UpperReservePrice;
	}

	/**
	 *  Get the lower deadline. 
	 */
	public Date getLowerDeadline() {
		return LowerDeadline;
	}

	/**
	 *  Get the upper deadline
	 */
	public Date getUpperDeadline() {
		return UpperDeadline;
	}
	
	/**
	 *  Set the cells 
	 */
	public void setCells(Cell[][] cells){
		this.cells = cells;
	}
	
	/**
	 * Get the cells. 
	 */
	public Cell[][] getCells() {
		return cells;
	}

	/**
	 *  Get the number of cells. 
	 */
	public int getNumberOfCells() {
		return numberOfCells;
	}

	/**
	 *  Set the number of cells. 
	 */
	public void setNumberOfCells(int numberOfCells) {
		this.numberOfCells = numberOfCells;
	}
	
		
}
