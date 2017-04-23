package Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ServerInfoPool {
	
	private static ServerInfoPool instance;
	
	private ServerInfo localServerInfo;
	
	private List<ServerInfo> otherServerInfoList;
	
	private HashMap<String,UserInfo> userInfoMap;
	
	private HashMap<String,String> userLock;

	private HashMap<String,ChatroomInfo> chatroomMap;
	
	private HashMap<String,String>chatroomLock;

	public static synchronized ServerInfoPool getInstance(){
		if(instance == null){
			instance = new ServerInfoPool();
		}
		return instance;
	}
	
	private ServerInfoPool(){
		localServerInfo = new ServerInfo();
		otherServerInfoList = new ArrayList<ServerInfo>();
		userInfoMap = new HashMap<String,UserInfo>();
		userLock = new HashMap<String,String>();
		chatroomMap = new HashMap<String,ChatroomInfo>();
		chatroomLock = new HashMap<String,String>();
	}

	public ServerInfo getLocalServerInfo() {
		return localServerInfo;
	}

	public void setLocalServerInfo(ServerInfo localServerInfo) {
		this.localServerInfo = localServerInfo;
	}

	public synchronized void addOtherServer(ServerInfo otherServerInfo){
		otherServerInfoList.add(otherServerInfo);
	}

	public synchronized void userConnected(String userid,UserInfo userInfo){
		userInfoMap.put(userid, userInfo);
	}
	public synchronized void userDisconnected(String userid){
		userInfoMap.remove(userid);
	}
	public synchronized void userLocked(String userid, String serverid){
		userLock.put(userid, serverid);
	}
	public synchronized void userUnlocked(String userid){
		userLock.remove(userid);
	}
	public synchronized void chatroomConnected(String chatroomId,ChatroomInfo chatroomInfo){
		chatroomMap.put(chatroomId, chatroomInfo);
	}
	public synchronized void chatroomDisconnected(String chatroomId){
		chatroomMap.remove(chatroomId);
	}
	public synchronized void chatroomLocked(String chatroomId ,String serverid){
		chatroomLock.put(chatroomId, serverid);
	}
	public synchronized void chatroomUnlocked(String chatroomId){
		chatroomLock.remove(chatroomId);
	}

	public List<ServerInfo> getOtherServerInfoList() {
		return otherServerInfoList;
	}
	public void clearOtherServerInfoList(){
		otherServerInfoList.clear();
	}

	public Map<String, UserInfo> getUserInfoMap() {
		return userInfoMap;
	}

	public Map<String,String> getUserLock() {
		return userLock;
	}

	public Map<String, ChatroomInfo> getChatroomMap() {
		return chatroomMap;
	}

	public Map<String,String> getChatroomLock() {
		return chatroomLock;
	}

}
