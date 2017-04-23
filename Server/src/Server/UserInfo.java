package Server;

import java.net.Socket;

public class UserInfo {
	
	private String identity;
	private String cunrrentChatroom;
	private Socket userSocket;
	private ClientConnection userThread;
	
	public UserInfo(){
		identity = "";
		cunrrentChatroom = "";
		userSocket = null;
		userThread = null;
	}
	
	public String getIdentity() {
		return identity;
	}
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	public String getCunrrentChatroom() {
		return cunrrentChatroom;
	}
	public void setCunrrentChatroom(String cunrrentChatroom) {
		this.cunrrentChatroom = cunrrentChatroom;
	}
	public Socket getUserSocket() {
		return userSocket;
	}
	public void setUserSocket(Socket userSocket) {
		this.userSocket = userSocket;
	}
	public ClientConnection getUserThread() {
		return userThread;
	}
	public void setUserThread(ClientConnection userThread) {
		this.userThread = userThread;
	}

}