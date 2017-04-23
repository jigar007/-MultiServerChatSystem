package Server;

public class Config {
	
	private String serverid;
	private String serverAddress;
	private int clientPort;
	private int coordinationPort;
	private String serverStatus;
	public Config(String serverid,String serverAddress,int clientPort,int coordinationPort,String serverStatus){
		super();
		this.serverid=serverid;
		this.serverAddress=serverAddress;
		this.clientPort=clientPort;
		this.coordinationPort=coordinationPort;
		this.serverStatus = serverStatus;
	}

	public String getServerStatus() {
		return serverStatus;
	}

	public void setServerStatus(String serverStatus) {
		this.serverStatus = serverStatus;
	}

	public String getServerid() {
		return serverid;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public int getClientPort() {
		return clientPort;
	}

	public int getCoordinationPort() {
		return coordinationPort;
	}

}