package trading.common;

import java.util.ArrayList;

import trading.strategy.Offer;

public class OrderDetails {
	
	private Order order;
	private ArrayList<OpponentDetails> opponentDetailArrayList = new ArrayList<OpponentDetails>();

	//constructor 1
	public OrderDetails(Order newOrder){
		this.order = newOrder;		
	}
	
	public ArrayList<Offer> getOfferHistoryOfGivenOpponent(String opponentName){
		
		ArrayList<Offer> retArrayList = new ArrayList<Offer>();
		
		//if the opponent records are already available
		if(this.getOpponentIndexInOpponentDetailArrayList(opponentName)!= -1){
			retArrayList = this.opponentDetailArrayList.get(this.getOpponentIndexInOpponentDetailArrayList(opponentName)).getOpponentOfferHistory(); 
		}
		 return retArrayList;
	}
	
	public void addOfferToOpponentOfferHistory(String opponentName, Offer newOffer){
		
		boolean isBuyer = false;
		
		//if the opponent records are already available
		if(this.getOpponentIndexInOpponentDetailArrayList(opponentName)!= -1){
			this.opponentDetailArrayList.get(this.getOpponentIndexInOpponentDetailArrayList(opponentName)).addOfferToTheOpponentOfferHistory(newOffer); 
			//System.out.println(opponentName+ " opponent offer history size = "+this.opponentDetailArrayList.get(this.getOpponentIndexInOpponentDetailArrayList(opponentName)).getOpponentOfferHistory().size() );
		}
		else{
			
			if(opponentName.startsWith("Buyer")){
				isBuyer = true;				
			}
			
			ArrayList<Offer> newArrayList = new ArrayList<Offer>();
			newArrayList.add(newOffer);
			OpponentDetails newOpponentDetailObject = new OpponentDetails(isBuyer, opponentName, newArrayList);
			
			this.opponentDetailArrayList.add(this.opponentDetailArrayList.size(), newOpponentDetailObject);
			//System.out.println(opponentName+ " opponent offer history size = "+this.opponentDetailArrayList.get(this.getOpponentIndexInOpponentDetailArrayList(opponentName)).getOpponentOfferHistory().size() );
			
		}
	}
	
	
	public int getOpponentIndexInOpponentDetailArrayList(String opponentName){
		
		//if the opponent is not at the array list return the index of -1
		int opponentAt = -1;
		
		if(this.opponentDetailArrayList.size()!= 0){
			
			for(int i=0; i < this.opponentDetailArrayList.size(); i++ ){
				
				if(this.opponentDetailArrayList.get(i).getOpponentName().equals(opponentName)){
					opponentAt = i;
					break;
				}
			}
			
		}
		
		return opponentAt;
	}

	/**
	 * @return the order
	 */
	public Order getOrder() {
		return order;
	}

	/**
	 * @param order the order to set
	 */
	public void setOrder(Order order) {
		this.order = order;
	}

	/**
	 * @return the opponentDetailArrayList
	 */
	public ArrayList<OpponentDetails> getOpponentDetailArrayList() {
		return opponentDetailArrayList;
	}

	/**
	 * @param opponentDetailArrayList the opponentDetailArrayList to set
	 */
	public void setOpponentDetailArrayList(
			ArrayList<OpponentDetails> opponentDetailArrayList) {
		this.opponentDetailArrayList = opponentDetailArrayList;
	}
	
	
	
}
