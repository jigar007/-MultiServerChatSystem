package Server;

import java.util.ArrayList;
import java.util.List;

public class LocalChatroomInfo extends ChatroomInfo {
	
	private String ownid;
	
	private List<UserInfo> userInfoList;
	
	public LocalChatroomInfo(){
		ownid = "";
		userInfoList = new ArrayList<UserInfo>();
	}
	
	public synchronized void userConnected(UserInfo newUser){
		userInfoList.add(newUser);
	}
	
	public synchronized void userDisconnected(UserInfo user){
		userInfoList.remove(user);
	}

	public String getOwnid() {
		return ownid;
	}

	public void setOwnid(String ownid) {
		this.ownid = ownid;
	}

	public List<UserInfo> getUserInfoList() {
		return userInfoList;
	}


}
