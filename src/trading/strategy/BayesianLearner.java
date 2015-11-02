package trading.strategy;

public class BayesianLearner {

	//attributes of the calculator
	private Calculator calculator;
	
	/**
	 *  Constructor. 
	 */
	public BayesianLearner(Calculator calculator){
		this.calculator = calculator;
	}
	
	/**
	 *  Bayesian learning Method
	 *  update the hypothesis for each cell
	 */
	public void Learn(int numberOfRows, int numberOfColumns, DetectionRegion detReg, int currentRound){	   
		
		//Bayesian learning is done on 1st round onwards
		if( currentRound > 0){
			
			double newConditionalProbability;
			//Calculate the denominator of the conditional probability formula
			double sum = this.SumOfProbabilityDistribution(numberOfRows, numberOfColumns, detReg);

			for (int i = 0; i < numberOfRows; i++) {
				for (int j = 0; j < numberOfColumns; j++) {
					
					if(sum != 0.0 && sum != Double.POSITIVE_INFINITY && sum != Double.NEGATIVE_INFINITY){
						
						if(!detReg.getCells()[i][j].isExpired()){
							
							newConditionalProbability = this.productOfPriorProbAndConditionalProb(i, j, detReg)*1.0 / (sum);
							detReg.getCells()[i][j].setProbability(newConditionalProbability);
							//System.out.println("set prob @ cell ["+i+","+j+"] = "+ detReg.getCells()[i][j].getProbability());
						}						
					 } 				
					
					//System.out.println("posterior probability @ cell ["+i+","+j+"] = "+ detReg.getCells()[i][j].getProbability());
				
				} //end of the inner for loop
			}// end of outer for loop
		}//end of if clause
		
	}
	
	
	/** 
	 * Calculate the product of the prior probability and the conditional probability 
	 */
	public double productOfPriorProbAndConditionalProb(int row, int col, DetectionRegion detReg){
		
		//initialize the parameters 
		double pOHi = detReg.getCells()[row][col].getProbability() * 1.0;
		double gamma =  detReg.getCells()[row][col].getNewGammaValue() * 1.0;				
		
		//System.out.println("upper ratio = "+ (pOHi * gamma));
				
		return  (double)(pOHi * gamma);
	}
	
	/**
	 *  Calculate the denominator of the conditional probability formula
	 *   calculate the sum of the probability distribution
	 */
	public double SumOfProbabilityDistribution(int numberOfRows, int numberOfColumns, DetectionRegion detReg){
		
		//System.out.println("======In the sum of probability distribution======");
		
		//initialize the parameters
		double sum = 0.0; 
		
		//get the sum of the probability distribution - denominator of conditional probability
		for (int i = 0; i < numberOfRows; i++) { //outer for loop
			for (int j = 0; j < numberOfColumns; j++) { //inner for loop						
						
				//System.out.println("i= "+i+" , j = "+j ); 
				
				//probability calculate for valid cells only
				if(!detReg.getCells()[i][j].isExpired()){
					
					double	pOHi = (detReg.getCells()[i][j].getProbability() * 1.0);
					double gamma =  (detReg.getCells()[i][j].getNewGammaValue()* 1.0);
					
					//System.out.println("pOHi= "+pOHi+" , gamma = "+gamma );	
					
					sum += (pOHi * gamma);
				}
				
				//System.out.println("sum= "+sum );
					
			}//end of inner for loop
		}//end of outer for loop
		
		//System.out.println("\nlower ratio = "+ sum);
		return sum;
	}
	
	/**
	 *  print the probability of each cell
	 */
	public void printCondProbability(DetectionRegion detectionRegion, int numberOfRows, int numberOfColumns){
		for(int i = 0; i< numberOfRows; i++ ){
			for(int j=0; j<numberOfColumns; j++){
				System.out.println("cell["+i+","+j+"] : probability = "+ detectionRegion.getCells()[i][j].getProbability());
			}			
		}		
	}
}
