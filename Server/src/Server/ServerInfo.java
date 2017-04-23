package Server;

public class ServerInfo {
	
	private String serverName;
	private String address;
	private int clientPort;
	private int coordinationPort;
	
	public ServerInfo(){
		serverName = "";
		address = "";
		clientPort = 0;
		coordinationPort = 0;
	}
	
	public String getServerName() {
		return serverName;
	}
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getClientPort() {
		return clientPort;
	}
	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}
	public int getCoordinationPort() {
		return coordinationPort;
	}
	public void setCoordinationPort(int coordinationPort) {
		this.coordinationPort = coordinationPort;
	}

}