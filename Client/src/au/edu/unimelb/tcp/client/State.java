package au.edu.unimelb.tcp.client;

public class State {

	private String identity;
	private String roomId;
	/*----------------------------------------------*/
	//add a assword and its getter method;
	private String password;
	
	public State(String identity, String roomId,String password) {
		this.identity = identity;
		this.roomId = roomId;
		this.password = password;
		
	}
	
	public synchronized String getRoomId() {
		return roomId;
	}
	public synchronized void setRoomId(String roomId) {
		this.roomId = roomId;
	}
	
	public String getIdentity() {
		return identity;
	}
	
	public String getPassword(){
		return password;
	}
	
}
