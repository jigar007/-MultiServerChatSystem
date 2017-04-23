package Server;

public class ChatroomInfo {
	
	private String chatroomid;
	
	public ChatroomInfo(){
		chatroomid="MainHall-"+ServerInfoPool.getInstance().getLocalServerInfo().getServerName();
	}

	public String getChatroomid() {
		return chatroomid;
	}

	public void setChatroomid(String chatroomid) {
		this.chatroomid = chatroomid;
	}

}
