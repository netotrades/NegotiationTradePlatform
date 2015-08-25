package trading.strategy;

public class BayesianLearner {

	private Calculator calculator;
	
	public BayesianLearner(Calculator calculator){
		this.calculator = calculator;
	}
	
	public void Learn(int numberOfRows, int numberOfColumns, DetectionRegion detReg){
		float sum = 0;
		for (int i = 0; i < numberOfRows; i++) {
			for (int j = 0; j < numberOfColumns; j++) {
				sum += detReg.getCells()[i][j].getProbability() * detReg.getCells()[i][j].getNewGammaValue();
			}
		}

		for (int i = 0; i < numberOfRows; i++) {
			for (int j = 0; j < numberOfColumns; j++) {
				detReg.getCells()[i][j].setProbability((float) ((detReg.getCells()[i][j].getProbability() * detReg.getCells()[i][j].getNewGammaValue() / (sum))));
			}
		}
	}
}
