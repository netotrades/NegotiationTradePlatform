package trading.common;

import java.util.ArrayList;

import trading.strategy.Offer;

public class OpponentDetails {
	
	private boolean isBuyer;
	private String opponentName;
	private ArrayList<Offer> opponentOfferHistory = new ArrayList<Offer>();
	private int agentCurrentRound = 0;
	private Offer agentPreviousOffer;
	private ArrayList<Double> agentUtilityPriceArray = new ArrayList<Double>();
	private ArrayList<Double> agentUtilityTimeArray = new ArrayList<Double>();
	private double averagePriceUtility = 0.0;
	private double averageTimeUtility = 0.0;
	
	//constructor 1
	public OpponentDetails(boolean isBuyer, String opponentName, ArrayList<Offer> opponentOfferHistory){
		this.isBuyer = isBuyer;
		this.opponentName = opponentName;
		this.opponentOfferHistory = opponentOfferHistory;	 
	}
	
	public int increaseCurrentRound(){
		this.agentCurrentRound++;
		return this.agentCurrentRound;
	}
	
	
	
	/**
	 * @return the averagePriceUtility
	 */
	public double getAveragePriceUtility() {
		return averagePriceUtility;
	}

	/**
	 * @param averagePriceUtility the averagePriceUtility to set
	 */
	public void setAveragePriceUtility(double averagePriceUtility) {
		this.averagePriceUtility = averagePriceUtility;
	}

	/**
	 * @return the averageTimeUtility
	 */
	public double getAverageTimeUtility() {
		return averageTimeUtility;
	}

	/**
	 * @param averageTimeUtility the averageTimeUtility to set
	 */
	public void setAverageTimeUtility(double averageTimeUtility) {
		this.averageTimeUtility = averageTimeUtility;
	}

	/**
	 * @return the agentUtilityPriceArray
	 */
	public ArrayList<Double> getAgentUtilityPriceArray() {
		return agentUtilityPriceArray;
	}

	/**
	 * @param agentUtilityPriceArray the agentUtilityPriceArray to set
	 */
	public void setAgentUtilityPriceArray(ArrayList<Double> agentUtilityPriceArray) {
		this.agentUtilityPriceArray = agentUtilityPriceArray;
	}

	/**
	 * @return the agentUtilityTimeArray
	 */
	public ArrayList<Double> getAgentUtilityTimeArray() {
		return agentUtilityTimeArray;
	}

	/**
	 * @param agentUtilityTimeArray the agentUtilityTimeArray to set
	 */
	public void setAgentUtilityTimeArray(ArrayList<Double> agentUtilityTimeArray) {
		this.agentUtilityTimeArray = agentUtilityTimeArray;
	}

	/**
	 * @param offerAt int of the given position
	 * @return retOffer offer at the given position
	 */
	public Offer getOfferHistoryAt(int offerAt){
		
		Offer retOffer = new Offer();
		
		if(this.opponentOfferHistory.size() != 0){
			retOffer = this.opponentOfferHistory.get(offerAt);
		}
		
		return retOffer;
	}
	
	/**
	 * @param newOffer to add to the opponent offer history list
	 */
	public void addOfferToTheOpponentOfferHistory(Offer newOffer){
		this.opponentOfferHistory.add(newOffer);
	}
	
	/**
	 * @return the size of the opponent offer history list
	 */
	public int getOpponentOfferHistorySize(){
		 return this.opponentOfferHistory.size();
	}	
		
	/**
	 * @return the isBuyer
	 */
	public boolean isBuyer() {
		return isBuyer;
	}
	
	/**
	 * @param isBuyer the isBuyer to set
	 */
	public void setBuyer(boolean isBuyer) {
		this.isBuyer = isBuyer;
	}
	
	/**
	 * @return the opponentName
	 */
	public String getOpponentName() {
		return opponentName;
	}
	
	/**
	 * @param opponentName the opponentName to set
	 */
	public void setOpponentName(String opponentName) {
		this.opponentName = opponentName;
	}
	
	/**
	 * @return the opponentOfferHistory
	 */
	public ArrayList<Offer> getOpponentOfferHistory() {
		return opponentOfferHistory;
	}
	
	/**
	 * @param opponentOfferHistory the opponentOfferHistory to set
	 */
	public void setOpponentOfferHistory(ArrayList<Offer> opponentOfferHistory) {
		this.opponentOfferHistory = opponentOfferHistory;
	}
	
	/**
	 * @return the opponentCurrentRound
	 */
	public int getAgentCurrentRound() {
		//System.out.println("this is agent current round ="+ this.agentCurrentRound);
		return agentCurrentRound;
	}
	
	/**
	 * @param agentCurrentRound the agentCurrentRound to set
	 */
	public void setAgentCurrentRound(int agentCurrentRound) {
		this.agentCurrentRound = agentCurrentRound;
	}
	
	/**
	 * @return the agentPreviousOffer
	 */
	public Offer getAgentPreviousOffer() {
		return agentPreviousOffer;
	}
	
	/**
	 * @param agentPreviousOffer the agentPreviousOffer to set
	 */
	public void setAgentPreviousOffer(Offer agentPreviousOffer) {
		this.agentPreviousOffer = agentPreviousOffer;
	}
	
	
	

}
