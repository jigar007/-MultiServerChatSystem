package Server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerConnection extends Thread {

	private Socket serverSocket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private int serverNum;
	private String configPath = "";

	public ServerConnection(Socket serverSocket, int serverNum,String configPath) {
		try {
			this.serverSocket = serverSocket;
			reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream(), "UTF-8"));
			this.serverNum = serverNum;
			this.configPath = configPath;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		try {

			System.out.println(Thread.currentThread().getName() + " - Reading messages from server's " + serverNum
					+ " connection");

			String serverMsg = "";
			while((serverMsg = reader.readLine()) != null) {
					System.out.println(Thread.currentThread().getName() + " - Message from server " + serverNum
							+ " received: " + serverMsg);

					JSONParser parser = new JSONParser();
					JSONObject msg = (JSONObject) parser.parse(serverMsg);
					serverCommandClassifer(msg);
				}
			
			
			serverSocket.close();
			
			//Update the server state to reflect the client disconnection

			System.out.println(Thread.currentThread().getName() 
						+ " - Client " + serverNum + " disconnected");
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void serverCommandClassifer(JSONObject msg) {

		if (msg.get("type").equals("lockidentity")) {
			if (ServerInfoPool.getInstance().getUserInfoMap().containsKey(msg.get("identity"))
					|| ServerInfoPool.getInstance().getUserLock().containsKey(msg.get("identity"))) {
				JSONObject lockidentityBack = new JSONObject();
				lockidentityBack.put("type", "lockidentity");
				lockidentityBack.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				lockidentityBack.put("identity", msg.get("identity"));
				lockidentityBack.put("locked", "false");
				write(lockidentityBack.toJSONString());
			} else {
				ServerInfoPool.getInstance().userLocked(msg.get("identity").toString(),msg.get("serverid").toString());
				JSONObject lockidentityBack = new JSONObject();
				lockidentityBack.put("type", "lockidentity");
				lockidentityBack.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				lockidentityBack.put("identity", msg.get("identity"));
				lockidentityBack.put("locked", "true");
				write(lockidentityBack.toJSONString());
			}

		} else if (msg.get("type").equals("releaseidentity")) {/// bugs
			
			if(ServerInfoPool.getInstance().getUserLock().containsKey(msg.get("identity"))
					&&ServerInfoPool.getInstance().getUserLock().get(msg.get("identity")).equals(msg.get("serverid"))){
				ServerInfoPool.getInstance().userUnlocked(msg.get("identity").toString());
			}

		} else if (msg.get("type").equals("lockroomid")){
			if(ServerInfoPool.getInstance().getChatroomMap().containsKey(msg.get("roomid"))
					|| ServerInfoPool.getInstance().getChatroomLock().containsKey(msg.get("roomid"))){
				JSONObject lockRoomidBack = new JSONObject();
				lockRoomidBack.put("type", "lockroomid");
				lockRoomidBack.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				lockRoomidBack.put("roomid", msg.get("roomid"));
				lockRoomidBack.put("locked", "false");
				write(lockRoomidBack.toJSONString());
			}else{
				ServerInfoPool.getInstance().chatroomLocked(msg.get("roomid").toString(),msg.get("serverid").toString());
				JSONObject lockRoomidBack = new JSONObject();
				lockRoomidBack.put("type", "lockroomid");
				lockRoomidBack.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				lockRoomidBack.put("roomid", msg.get("roomid"));
				lockRoomidBack.put("locked", "true");
				write(lockRoomidBack.toJSONString());
				
			}
		} else if (msg.get("type").equals("releaseroomid")){
			if(msg.get("approved").equals("true")){
				if(ServerInfoPool.getInstance().getChatroomLock().containsKey(msg.get("roomid").toString())){
					ServerInfoPool.getInstance().chatroomUnlocked(msg.get("roomid").toString());
				}
				RemoteChatroomInfo rcr = new RemoteChatroomInfo();
				rcr.setChatroomid(msg.get("roomid").toString());
				
				for(Config i: updateConfig()){
					if(i.getServerid().equals(msg.get("serverid"))){
						ServerInfo si = new ServerInfo();
						si.setAddress(i.getServerAddress());
						si.setClientPort(i.getClientPort());
						si.setCoordinationPort(i.getCoordinationPort());
						si.setServerName(i.getServerid());
						rcr.setServerInfo(si);
					}
				}
				ServerInfoPool.getInstance().chatroomConnected(msg.get("roomid").toString(), rcr);
			}else{
				if(ServerInfoPool.getInstance().getChatroomLock().containsKey(msg.get("roomid").toString())){
					ServerInfoPool.getInstance().chatroomUnlocked(msg.get("roomid").toString());
				}
			}
		} else if (msg.get("type").equals("deleteroom")){
			ServerInfoPool.getInstance().chatroomDisconnected(msg.get("roomid").toString());
		} else if (msg.get("type").equals("tellMeYourChatrooms")){
			String localChatRoom = "";
			for (Map.Entry<String, ChatroomInfo> entry : ServerInfoPool.getInstance().getChatroomMap().entrySet()) {
					if(entry.getValue() instanceof LocalChatroomInfo){
						localChatRoom += entry.getValue().getChatroomid()+" ";
					}
			}
			
			JSONObject replyToServer = new JSONObject();
			replyToServer.put("type", "replyForChatRooms");
			replyToServer.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
			replyToServer.put("serverAddress", ServerInfoPool.getInstance().getLocalServerInfo().getAddress());
			replyToServer.put("clientPort", ServerInfoPool.getInstance().getLocalServerInfo().getClientPort());
			replyToServer.put("coordinatinPort", ServerInfoPool.getInstance().getLocalServerInfo().getCoordinationPort());
			replyToServer.put("chatrooms", localChatRoom);
			write(replyToServer.toJSONString());
		}
		
	}

	// Needs to be synchronized because multiple threads can me invoking this
	// method at the same
	// time
	public synchronized void write(String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
			System.out.println(Thread.currentThread().getName() + " - Message sent to server " + serverNum);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public List<Config> updateConfig() {
		List<Config> configList = new ArrayList<Config>();
		ConfigLoader cl = new ConfigLoader();
		configList = cl.loadConfig(configPath);
		return configList;
	}
}