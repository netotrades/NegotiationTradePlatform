package trading;
 

import trading.common.Order;
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
	public IFuture<Integer>	callForProposal(String name);

	/**
	 *  Buy an item
	 *  @param name	The item name.
	 *  @param price	The price to pay.
	 *  @return A future indicating if the transaction was successful.
	 */
	public IFuture<Void> acceptProposal(String name, int price);
	
	/**
	 * Set seller's offer history
	 * @param name
	 * @param price
	 */
	public void setacceptablePrice(Order order, String name, int price);
	
	
	
//	/**
//	 *  Refuse to buy an item
//	 *  @param name	The item name.
//	 *  @param price	The requested price.
//	 */
//	public void	rejectProposal(String item, int price);
}
