package trading;
 

import java.util.Date;

import trading.common.Order;
import trading.strategy.Offer;
import jadex.commons.future.IFuture;

/**
 *  The buy item service is provided by the seller and used by the buyer.
 */
public interface IBuyItemService
{
	/**
	 *  Ask the seller for a quote on an item.
	 *  @param name	The item name.
	 *  @return The price.
	 */
	public IFuture<Integer>	callForProposal(String agentName,String name);

	/**
	 *  Buy an item
	 *  @param name	The item name.
	 *  @param price	The price to pay.
	 *  @return A future indicating if the transaction was successful.
	 */
	public IFuture<Void> acceptProposal(String agentName,String name, int price);
	
	/**
	 * Set seller's offer history
	 * @param name
	 * @param price
	 */
	public void setacceptablePrice(String agentName,Order order, String name, int price, Offer sellerPrevOffer);
	
	
	
//	/**
//	 *  Refuse to buy an item
//	 *  @param name	The item name.
//	 *  @param price	The requested price.
//	 */
//	public void	rejectProposal(String item, int price);
}
