package trading;

import jadex.bdiv3.BDIAgent;
import trading.common.NegotiationReport;
import trading.common.Order;

import java.util.Collection;
import java.util.List;

/**
 * 
 */
public interface INegotiationAgent
{
	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public BDIAgent getAgent();
	
	/**
	 *  Create a purchase or sell oder.
	 */
	public void createGoal(Order order);
	
	/**
	 *  Get all purchase or sell goals.
	 */
	public Collection<INegotiationGoal> getGoals();
	
	/**
	 *  Get all orders.
	 */
	public List<Order> getOrders();
	
	/**
	 *  Get all reports.
	 */
	public List<NegotiationReport> getReports(Order order);
}
