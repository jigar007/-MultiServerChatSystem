package Server;

public class RemoteChatroomInfo extends ChatroomInfo{
	
	private ServerInfo otherServerInfo;
	
	public RemoteChatroomInfo(){
		otherServerInfo = new ServerInfo();
	}

	public ServerInfo getServerInfo() {
		return otherServerInfo;
	}

	public void setServerInfo(ServerInfo serverInfo) {
		this.otherServerInfo = serverInfo;
	}
	
	

}
