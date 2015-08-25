package trading.strategy;

import java.util.ArrayList;
import java.util.Date;

public class StrategyCall {
	private Calculator calculator;
	private RegressionAnalyser regressionAnalyser;
	private BayesianLearner bayesianLearner;
	private AdaptiveConcessionStrategy ConcessionStrategy;

	private int numberOfRows = 10;
	private int numberOfColumns = 10;

	private DetectionRegion detReg;
	private Cell[][] cells;

	private Cell newCell;
	private double cellLowerPrice;
	private double cellUpperPrice;
	private Date cellLowerDate;
	private Date cellUpperDate;
	private double cellReservePrice;
	private Date cellDeadline;
	private double cellProbability;

	private Date currentTime;

	public StrategyCall() {
		calculator = new Calculator();
		regressionAnalyser = new RegressionAnalyser(calculator);
		bayesianLearner = new BayesianLearner(calculator);
		ConcessionStrategy = new AdaptiveConcessionStrategy(calculator);

		double itemPriceGuess = 1600.00;
		currentTime = new Date();

		// Initialize detection region
		detReg = new DetectionRegion(itemPriceGuess / 2, itemPriceGuess * 2, currentTime,
				new Date(currentTime.getTime() + 42000000));
		cells = new Cell[numberOfRows][numberOfColumns];

		for (int i = 0; i < numberOfRows; i++) {
			for (int j = 0; j < numberOfColumns; j++) {
				cellLowerPrice = detReg.getLowerReservePrice()
						+ ((detReg.getUpperReservePrice() - detReg.getLowerReservePrice()) / numberOfRows) * j;
				cellUpperPrice = detReg.getLowerReservePrice()
						+ ((detReg.getUpperReservePrice() - detReg.getLowerReservePrice()) / numberOfRows) * (j + 1);
				cellLowerDate = new Date(detReg.getLowerDeadline().getTime()
						+ ((detReg.getUpperDeadline().getTime() - detReg.getLowerDeadline().getTime())
								/ numberOfColumns) * i);
				cellUpperDate = new Date(detReg.getLowerDeadline().getTime()
						+ ((detReg.getUpperDeadline().getTime() - detReg.getLowerDeadline().getTime())
								/ numberOfColumns) * (i + 1));
				cellReservePrice = (cellUpperPrice + cellLowerPrice) / 2;
				cellDeadline = new Date((cellUpperDate.getTime() + cellLowerDate.getTime()) / 2);
				cellProbability = 1 / (numberOfRows * numberOfColumns);

				newCell = new Cell(cellLowerPrice, cellUpperPrice, cellLowerDate, cellUpperDate, cellReservePrice,
						cellDeadline, cellProbability);
				cells[i][j] = newCell;
			}
		}

		detReg.setCells(cells);
		detReg.setNumberOfCells(numberOfRows * numberOfColumns);
	}

	public void callForStrategy(double reservePrice, Date deadline, ArrayList<Offer> offerHistory, int numberOfRounds) {

		Offer newOffer = offerHistory.get(offerHistory.size() - 1);
		long stepSize = (newOffer.getOfferTime().getTime() - offerHistory.get(0).getOfferTime().getTime())
				/ (newOffer.getRoundNumber() - 1);

		regressionAnalyser.Analyse(detReg, numberOfRows, numberOfColumns, offerHistory, numberOfRounds, newOffer);
		bayesianLearner.Learn(numberOfRows, numberOfColumns, detReg);
		ConcessionStrategy.FindConcessionPoint(numberOfRows, numberOfColumns, detReg, newOffer, reservePrice, deadline,
				stepSize, numberOfRounds, offerHistory);
		ConcessionStrategy.GenerateNextOffer(detReg, reservePrice, deadline, numberOfRows, numberOfColumns, newOffer,
				stepSize);

	}

}
