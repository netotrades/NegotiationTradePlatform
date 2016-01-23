package trading.common;

public class IResultListenerData {
	
	private int result;
	private String agentName;
	
	public IResultListenerData(String agentName, int result){
		this.agentName = agentName;
		this.result = result;
	}

	/**
	 * @return the result
	 */
	public int getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	public void setResult(int result) {
		this.result = result;
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
	
	

}
