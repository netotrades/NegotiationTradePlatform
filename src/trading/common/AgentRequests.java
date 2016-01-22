package trading.common;

public class AgentRequests {

	private String agentName;
	private String orderName;
	
	//constructor 1
	public AgentRequests(String agentName, String orderName){
		this.agentName = agentName;
		this.orderName = orderName;
	}
	
	/**
	 * @return the agentName
	 */
	public String getAgentName() {
		return agentName;
	}
	/**
	 * @param agentName the agentName to set
	 */
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}
	/**
	 * @return the orderName
	 */
	public String getOrderName() {
		return orderName;
	}
	/**
	 * @param orderName the orderName to set
	 */
	public void setOrderName(String orderName) {
		this.orderName = orderName;
	}
	
	
}
