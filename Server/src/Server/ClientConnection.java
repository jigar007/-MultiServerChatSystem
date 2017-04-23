package Server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ClientConnection extends Thread {

	private SSLSocket clientSocket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private int clientNum;
	private List<String> returnLockIdentity;
	private List<String> returnLockChatroom;
	private String currentUserid;
	private String currentRoomid;
	private String currentRoomOwner;
	private String configPath = "";

	public ClientConnection(SSLSocket clientSocket, int clientNum, String configPath) {
		try {
			this.clientSocket = clientSocket;
			reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
			this.clientNum = clientNum;
			returnLockIdentity = new ArrayList<String>();
			returnLockChatroom = new ArrayList<String>();
			this.configPath = configPath;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		try {

			String clientMsg = null;
			while ((clientMsg = reader.readLine()) != null) {
				System.out.println(Thread.currentThread().getName() + " - Message from client " + clientNum
						+ " received: " + clientMsg);
				JSONParser parser = new JSONParser();
				JSONObject msg = (JSONObject) parser.parse(clientMsg);
				clientCommandClassifer(msg);
			}

			ServerInfoPool.getInstance().userDisconnected(currentUserid);
			((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(currentRoomid))
					.userDisconnected(ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid));
			clientSocket.close();
			System.out.println(Thread.currentThread().getName() + " - Client " + clientNum + " disconnected");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// this method includes all the recongnition and handling of the client
	// input command
	@SuppressWarnings("unchecked")
	public void clientCommandClassifer(JSONObject msg) throws InterruptedException, FileNotFoundException {

		if (msg.get("type").equals("newidentity")) {
			String identity = (String) msg.get("identity");
			String password = (String) msg.get("password");
			boolean isRegisted = false;
			Scanner inputStream = null;
			String[] userSavedInfo;
			boolean whetherBroadCast = false;
			inputStream = new Scanner(new FileInputStream("userinfo.dat"));
			while (inputStream.hasNextLine()) {
				userSavedInfo = inputStream.nextLine().split("\t");
				if (identity.equals(userSavedInfo[0]) && password.equals(userSavedInfo[1])) {
					isRegisted = true;

					if (((String) msg.get("identity")).length() >= 3 && ((String) msg.get("identity")).length() <= 16
							&& !ServerInfoPool.getInstance().getUserInfoMap().containsKey(msg.get("identity"))) {
						updateCurrentUserState();
						JSONObject newidentity = new JSONObject();
						newidentity.put("type", "lockidentity");
						newidentity.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
						newidentity.put("identity", msg.get("identity"));
						String sendToServer = newidentity.toJSONString();
						// update current server info and then communicate with
						// all other server with "on" status
						// also save other server info which the current server
						// connect with into otherServerInfoList
						for (Config i : updateConfig()) {
							if (!i.getServerid()
									.equals(ServerInfoPool.getInstance().getLocalServerInfo().getServerName())
									&& i.getServerStatus().equals("on")) {
								whetherBroadCast = true;
								ServerInfo si2 = new ServerInfo();
								si2.setServerName(i.getServerid());
								si2.setAddress(i.getServerAddress());
								si2.setClientPort(i.getClientPort());
								si2.setCoordinationPort(i.getCoordinationPort());
								ServerInfoPool.getInstance().addOtherServer(si2);
								serverToServer sts = new serverToServer(i.getServerAddress(), i.getCoordinationPort(),
										this);
								sts.openSocket(sendToServer);

							}
						}
						// only check the return message from otherServerList
						// because current server only send message to these
						// server just now
						// the function of otherServerList has changed
						//when we only open one server, we don't need to broadcast
						while (whetherBroadCast) {
							if (returnLockIdentity.size() == ServerInfoPool.getInstance().getOtherServerInfoList()
									.size()) {
								if (returnLockIdentity.contains("false")) {
									JSONObject disapprove = new JSONObject();
									disapprove.put("type", "newidentity");
									disapprove.put("approved", "false");
									this.write(disapprove.toJSONString());
									returnLockIdentity.clear();
								} else {
									JSONObject approve = new JSONObject();
									approve.put("type", "newidentity");
									approve.put("approved", "true");
									this.write(approve.toJSONString());
									LocalChatroomInfo lci = new LocalChatroomInfo();

									if (!ServerInfoPool.getInstance().getChatroomMap()
											.containsKey(lci.getChatroomid())) {
										ServerInfoPool.getInstance().chatroomConnected(lci.getChatroomid(), lci);
									}
									returnLockIdentity.clear();
									addNewUser((String) msg.get("identity"));
								}
								break;
							}
						}
						//when we only open one server, we need to allow new user if it not exists in current server
						if(whetherBroadCast == false){
							JSONObject approve = new JSONObject();
							approve.put("type", "newidentity");
							approve.put("approved", "true");
							this.write(approve.toJSONString());
							LocalChatroomInfo lci = new LocalChatroomInfo();

							if (!ServerInfoPool.getInstance().getChatroomMap()
									.containsKey(lci.getChatroomid())) {
								ServerInfoPool.getInstance().chatroomConnected(lci.getChatroomid(), lci);
							}
							addNewUser((String) msg.get("identity"));
						}
						JSONObject releaseidentity = new JSONObject();
						releaseidentity.put("type", "releaseidentity");
						releaseidentity.put("serverid",
								ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
						releaseidentity.put("identity", msg.get("identity"));
						sendToServer = releaseidentity.toJSONString();

						for (ServerInfo i : ServerInfoPool.getInstance().getOtherServerInfoList()) {
							serverToServer sts = new serverToServer(i.getAddress(), i.getCoordinationPort(), this);
							sts.openSocket(sendToServer);
						}
						// clear otherServerInfolist after sending
						// releaseIdentity
						// because next time the otherServerInfoList will change
						// acrooding to new configList
						ServerInfoPool.getInstance().clearOtherServerInfoList();

						JSONObject broadcast = new JSONObject();
						broadcast.put("type", "roomchange");
						broadcast.put("identity", msg.get("identity"));
						broadcast.put("former", "");
						broadcast.put("roomid",
								"MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
						String broadcastS = broadcast.toJSONString();
						for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
								.get("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName()))
										.getUserInfoList()) {
							i.getUserThread().write(broadcastS);
						}
					} else {
						JSONObject disapprove = new JSONObject();
						disapprove.put("type", "newidentity");
						disapprove.put("approved", "false");
						this.write(disapprove.toJSONString());
						try {
							clientSocket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					break;
				}
			}
			inputStream.close();
			if (isRegisted == false) {
				JSONObject refuseRegist = new JSONObject();
				refuseRegist.put("type", "notregister");
				this.write(refuseRegist.toJSONString());
			}

		} else if (msg.get("type").equals("list")) {// show other mainhall st
													// the begining
			updateCurrentUserState();
			JSONObject list = new JSONObject();
			list.put("type", "roomlist");
			List<String> roomList = new ArrayList<String>();
			roomList.add("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
			// roomList.addAll(sip.getChatroomLock().keySet());
			
			for (Config i : updateConfig()) {
				if (!i.getServerid().equals(ServerInfoPool.getInstance().getLocalServerInfo().getServerName())&& i.getServerStatus().equals("on")) {
					roomList.add("MainHall-" + i.getServerid());
				}
			}

			roomList.addAll(ServerInfoPool.getInstance().getChatroomMap().keySet());
			roomList.remove("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
			list.put("rooms", roomList);
			this.write(list.toJSONString());

		} else if (msg.get("type").equals("who")) {
			updateCurrentUserState();
			for (Map.Entry<String, UserInfo> entry : ServerInfoPool.getInstance().getUserInfoMap().entrySet()) {
				if (entry.getValue().getUserThread() == this) {
					String room = entry.getValue().getCunrrentChatroom();
					String owner = ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(room))
							.getOwnid();
					List<UserInfo> users = ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(room))
							.getUserInfoList();
					List<String> userid = new ArrayList<String>();
					for (UserInfo i : users) {
						userid.add(i.getIdentity());
					}
					JSONObject who = new JSONObject();
					who.put("type", "roomcontents");
					who.put("roomid", room);
					who.put("identities", userid);// output format bugs
					who.put("owner", owner);
					this.write(who.toJSONString());
					break;
				}
			}

		} else if (msg.get("type").equals("createroom")) {// create mainhall
			updateCurrentUserState(); // show errors
			if (currentUserid != currentRoomOwner && ((String) msg.get("roomid")).length() >= 3
					&& ((String) msg.get("roomid")).length() <= 16
					&& !ServerInfoPool.getInstance().getChatroomMap().containsKey(msg.get("roomid"))) {
				String formerChatroom = "";
				boolean whetherBroadCast = false;
				
				formerChatroom = currentRoomid;

				JSONObject lockroomid = new JSONObject();
				lockroomid.put("type", "lockroomid");
				lockroomid.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				lockroomid.put("roomid", msg.get("roomid"));

				String sendToServer = lockroomid.toJSONString();
				
				for (Config i : updateConfig()) {
					if (!i.getServerid().equals(ServerInfoPool.getInstance().getLocalServerInfo().getServerName())&& i.getServerStatus().equals("on")) {
						whetherBroadCast = true;
						ServerInfo si2 = new ServerInfo();
						si2.setServerName(i.getServerid());
						si2.setAddress(i.getServerAddress());
						si2.setClientPort(i.getClientPort());
						si2.setCoordinationPort(i.getCoordinationPort());
						ServerInfoPool.getInstance().addOtherServer(si2);
						serverToServer sts = new serverToServer(i.getServerAddress(), i.getCoordinationPort(),
								this);
						sts.openSocket(sendToServer);

					}
				}

				while (whetherBroadCast) {

					if (returnLockChatroom.size() == ServerInfoPool.getInstance().getOtherServerInfoList().size()) {

						if (returnLockChatroom.contains("false")) {
							JSONObject disapproved = new JSONObject();
							disapproved.put("type", "createroom");
							disapproved.put("roomid", msg.get("roomid"));
							disapproved.put("approved", "false");
							this.write(disapproved.toJSONString());
						} else {
							addLocalChatroom(msg.get("roomid").toString());
							JSONObject approved = new JSONObject();
							approved.put("type", "createroom");
							approved.put("roomid", msg.get("roomid"));
							approved.put("approved", "true");
							this.write(approved.toJSONString());
							returnLockChatroom.clear();

						}
						break;
					}

				}
				if(whetherBroadCast == false){
					addLocalChatroom(msg.get("roomid").toString());
					JSONObject approved = new JSONObject();
					approved.put("type", "createroom");
					approved.put("roomid", msg.get("roomid"));
					approved.put("approved", "true");
					this.write(approved.toJSONString());
				}
				JSONObject releaseidentity = new JSONObject();
				if (returnLockChatroom.contains("false")) {
					releaseidentity.put("type", "releaseroomid");
					releaseidentity.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
					releaseidentity.put("roomid", msg.get("roomid"));
					releaseidentity.put("approved", "false");
				} else {
					releaseidentity.put("type", "releaseroomid");
					releaseidentity.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
					releaseidentity.put("roomid", msg.get("roomid"));
					releaseidentity.put("approved", "true");
				}

				sendToServer = releaseidentity.toJSONString();

				for (ServerInfo i : ServerInfoPool.getInstance().getOtherServerInfoList()) {
					serverToServer sts = new serverToServer(i.getAddress(), i.getCoordinationPort(), this);
					sts.openSocket(sendToServer);
				}
				ServerInfoPool.getInstance().clearOtherServerInfoList();
				JSONObject broadcast = new JSONObject();
				broadcast.put("type", "roomchange");
				broadcast.put("identity", currentUserid);
				broadcast.put("former", formerChatroom);
				broadcast.put("roomid", msg.get("roomid"));
				for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(formerChatroom)).getUserInfoList()) {
					System.out.println("broadcast");
					i.getUserThread().write(broadcast.toJSONString());
				}
				((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(currentRoomid)).userDisconnected(ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid));

			} else {
				JSONObject disapproved = new JSONObject();
				disapproved.put("type", "createroom");
				disapproved.put("roomid", msg.get("roomid"));
				disapproved.put("approved", "false");
				this.write(disapproved.toJSONString());
			}

		} else if (msg.get("type").equals("join")) {
			updateCurrentUserState();
			String formerRoom = currentRoomid;
			String changeToRoom = msg.get("roomid").toString();
			if (ServerInfoPool.getInstance().getChatroomMap().containsKey(changeToRoom)
					&& currentUserid != ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get(currentRoomid)).getOwnid()) {
				if (ServerInfoPool.getInstance().getChatroomMap().get(changeToRoom) instanceof LocalChatroomInfo) {
					currentRoomid = changeToRoom;
					JSONObject roomchangeLocal = new JSONObject();
					roomchangeLocal.put("type", "roomchange");
					roomchangeLocal.put("identity", currentUserid);
					roomchangeLocal.put("former", formerRoom);
					roomchangeLocal.put("roomid", currentRoomid);

					for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get(formerRoom)).getUserInfoList()) {
						i.getUserThread().write(roomchangeLocal.toJSONString());
					}
					for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get(currentRoomid)).getUserInfoList()) {
						i.getUserThread().write(roomchangeLocal.toJSONString());
					}
					((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(formerRoom))
							.userDisconnected(ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid));
					ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid).setCunrrentChatroom(currentRoomid);
					((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(currentRoomid))
							.userConnected(ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid));
					this.write(roomchangeLocal.toJSONString());

				} else {
					String remoteHost = ((RemoteChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get(changeToRoom)).getServerInfo().getAddress();
					System.out.println(ServerInfoPool.getInstance().getChatroomMap().get(changeToRoom) instanceof RemoteChatroomInfo);
					int remotePort = ((RemoteChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get(changeToRoom)).getServerInfo().getClientPort();
					System.out.println(remotePort);
					currentRoomid = changeToRoom;
					JSONObject roomchangeLocal = new JSONObject();
					roomchangeLocal.put("type", "roomchange");
					roomchangeLocal.put("identity", currentUserid);
					roomchangeLocal.put("former", formerRoom);
					roomchangeLocal.put("roomid", currentRoomid);

					for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get(formerRoom)).getUserInfoList()) {
						i.getUserThread().write(roomchangeLocal.toJSONString());
					}
					((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(formerRoom))
							.userDisconnected(ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid));
					JSONObject route = new JSONObject();
					route.put("type", "route");
					route.put("roomid", changeToRoom);
					route.put("host", remoteHost);
					route.put("port", Integer.toString(remotePort));
					this.write(route.toJSONString());
				}
			} else {
				JSONObject roomchange = new JSONObject();
				roomchange.put("type", "roomchange");
				roomchange.put("identity", currentUserid);
				roomchange.put("former", currentRoomid);
				roomchange.put("roomid", currentRoomid);
				this.write(roomchange.toJSONString());
			}

		} else if (msg.get("type").equals("deleteroom")) {// cannot delete
															// mainhall
			String roomWillDelete = msg.get("roomid").toString();
			updateCurrentUserState();
			if (currentUserid.equals(currentRoomOwner) && roomWillDelete.equals(currentRoomid)) {
				JSONObject sendToServer = new JSONObject();
				sendToServer.put("type", "deleteroom");
				sendToServer.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				sendToServer.put("roomid", roomWillDelete);
				
				for (Config i : updateConfig()) {
					if (!i.getServerid().equals(ServerInfoPool.getInstance().getLocalServerInfo().getServerName())&& i.getServerStatus().equals("on")) {
						serverToServer sts = new serverToServer(i.getServerAddress(), i.getCoordinationPort(),this);
						sts.openSocket(sendToServer.toJSONString());

					}
				}

				for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
						.get(roomWillDelete)).getUserInfoList()) {
					JSONObject roomchangeLocal = new JSONObject();
					roomchangeLocal.put("type", "roomchange");
					roomchangeLocal.put("identity", i.getIdentity());
					roomchangeLocal.put("former", roomWillDelete);
					roomchangeLocal.put("roomid",
							"MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
					i.getUserThread().write(roomchangeLocal.toJSONString());

					for (UserInfo j : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName()))
									.getUserInfoList()) {
						j.getUserThread().write(roomchangeLocal.toJSONString());
					}

					i.setCunrrentChatroom(
							"MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
					ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid).setCunrrentChatroom(
							"MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
					((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName()))
									.userConnected(i);
				}

				ServerInfoPool.getInstance().chatroomDisconnected(roomWillDelete);

				JSONObject deleteRoom = new JSONObject();
				deleteRoom.put("type", "deleteroom");
				deleteRoom.put("roomid", currentRoomid);
				deleteRoom.put("approved", "true");
				this.write(deleteRoom.toJSONString());

			} else {
				JSONObject deleteRoom = new JSONObject();
				deleteRoom.put("type", "deleteroom");
				deleteRoom.put("roomid", currentRoomid);
				deleteRoom.put("approved", "false");
				this.write(deleteRoom.toJSONString());
			}
		} else if (msg.get("type").equals("message")) {// delete extra codes

			String message = msg.get("content").toString();
			for (Map.Entry<String, UserInfo> entry : ServerInfoPool.getInstance().getUserInfoMap().entrySet()) {
				if (entry.getValue().getUserThread() == this) {
					String currentUserid = entry.getValue().getIdentity();
					String currentRoomid = entry.getValue().getCunrrentChatroom();
					JSONObject messageJ = new JSONObject();
					messageJ.put("type", "message");
					messageJ.put("identity", currentUserid);
					messageJ.put("content", message);
					List<UserInfo> clientList = ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get(currentRoomid)).getUserInfoList();
					for (UserInfo client : clientList) {
						if (client.getUserThread() != this) {
							client.getUserThread().write(messageJ.toJSONString());
						}
					}

					break;
				}
			}

		} else if (msg.get("type").equals("quit")) {

			updateCurrentUserState();
			if (currentUserid.equals(currentRoomOwner)) {
				JSONObject sendToServer = new JSONObject();
				sendToServer.put("type", "deleteroom");
				sendToServer.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				sendToServer.put("roomid", currentRoomid);
				
				for (Config i : updateConfig()) {
					if (!i.getServerid().equals(ServerInfoPool.getInstance().getLocalServerInfo().getServerName())&& i.getServerStatus().equals("on")) {
						serverToServer sts = new serverToServer(i.getServerAddress(), i.getCoordinationPort(),this);
						sts.openSocket(sendToServer.toJSONString());

					}
				}

				((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(currentRoomid))
						.userDisconnected(ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid));
				((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(currentRoomid)).setOwnid("");

				for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(currentRoomid))
						.getUserInfoList()) {
					JSONObject roomchangeLocal = new JSONObject();
					roomchangeLocal.put("type", "roomchange");
					roomchangeLocal.put("identity", i.getIdentity());
					roomchangeLocal.put("former", currentRoomid);
					roomchangeLocal.put("roomid",
							"MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
					i.getUserThread().write(roomchangeLocal.toJSONString());
					for (UserInfo j : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName()))
									.getUserInfoList()) {
						j.getUserThread().write(roomchangeLocal.toJSONString());
					}

					i.setCunrrentChatroom("MainHall");
					ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid).setCunrrentChatroom(
							"MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
					((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
							.get("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName()))
									.userConnected(i);
				}

				ServerInfoPool.getInstance().userDisconnected(currentUserid);
				ServerInfoPool.getInstance().chatroomDisconnected(currentRoomid);
				JSONObject quitClient = new JSONObject();
				quitClient.put("type", "roomchange");
				quitClient.put("identity", currentUserid);
				quitClient.put("former", currentRoomid);
				quitClient.put("roomid", "");
				this.write(quitClient.toJSONString());

			} else {

				((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(currentRoomid))
						.userDisconnected(ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid));
				ServerInfoPool.getInstance().userDisconnected(currentUserid);

				JSONObject quitClient = new JSONObject();
				quitClient.put("type", "roomchange");
				quitClient.put("identity", currentUserid);
				quitClient.put("former", currentRoomid);
				quitClient.put("roomid", "");
				this.write(quitClient.toJSONString());
			}

		} else if (msg.get("type").equals("movejoin")) {

			UserInfo ui = new UserInfo();
			String moveInUser = msg.get("identity").toString();
			String moveInRoom = msg.get("roomid").toString();
			if (ServerInfoPool.getInstance().getChatroomMap().containsKey(moveInRoom)) {
				ui.setIdentity(moveInUser);
				ui.setCunrrentChatroom(moveInRoom);
				ui.setUserSocket(clientSocket);
				ui.setUserThread(this);
				ServerInfoPool.getInstance().userConnected(moveInUser, ui);

				JSONObject roomchange = new JSONObject();
				roomchange.put("type", "roomchange");
				roomchange.put("identity", moveInUser);
				roomchange.put("former", msg.get("former"));
				roomchange.put("roomid", moveInRoom);
				for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(moveInRoom))
						.getUserInfoList()) {
					i.getUserThread().write(roomchange.toJSONString());
				}
				((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap().get(moveInRoom)).userConnected(ui);
				JSONObject serverchange = new JSONObject();
				serverchange.put("type", "serverchange");
				serverchange.put("approved", "true");
				serverchange.put("serverid", ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				this.write(serverchange.toJSONString());
			} else {
				addNewUser(moveInUser);
				JSONObject roomchange = new JSONObject();
				roomchange.put("type", "roomchange");
				roomchange.put("identity", moveInUser);
				roomchange.put("former", msg.get("former"));
				roomchange.put("roomid",
						"MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
				for (UserInfo i : ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
						.get("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName()))
								.getUserInfoList()) {
					i.getUserThread().write(roomchange.toJSONString());
				}
			}

		}
	}

	// when new identity add to system , it invoke this method to add the user
	// into UserMap
	public void addNewUser(String userid) {
		UserInfo ui = new UserInfo();
		ui.setIdentity(userid);
		ui.setUserSocket(clientSocket);
		ui.setUserThread(this);
		ui.setCunrrentChatroom("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName());// try
																												// this:ui.setCunrrentChatroom()
		ServerInfoPool.getInstance().userConnected(userid, ui);
		((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
				.get("MainHall-" + ServerInfoPool.getInstance().getLocalServerInfo().getServerName()))
						.userConnected(ui);
	}

	// when new room add to system , it invoke this method to add the room into
	// RoomMap
	public void addLocalChatroom(String roomid) {
		LocalChatroomInfo lci = new LocalChatroomInfo();
		lci.setChatroomid(roomid);
		lci.setOwnid(currentUserid);
		lci.userConnected(ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid));
		ServerInfoPool.getInstance().chatroomConnected(roomid, lci);
		ServerInfoPool.getInstance().getUserInfoMap().get(currentUserid).setCunrrentChatroom(roomid);
	}

	public void lockIdentity(String lock) {
		returnLockIdentity.add(lock);
	}

	public void lockChatroom(String lock) {
		returnLockChatroom.add(lock);
	}

	// each time when this class handle client input, the system should update
	// current user information
	public void updateCurrentUserState() {
		for (Map.Entry<String, UserInfo> entry : ServerInfoPool.getInstance().getUserInfoMap().entrySet()) {
			if (entry.getValue().getUserThread() == this) {
				currentUserid = entry.getValue().getIdentity();
				currentRoomid = entry.getValue().getCunrrentChatroom();
				currentRoomOwner = ((LocalChatroomInfo) ServerInfoPool.getInstance().getChatroomMap()
						.get(currentRoomid)).getOwnid();
			}
		}

	}

	// update other server info
	public List<Config> updateConfig() {
		List<Config> configList = new ArrayList<Config>();
		ConfigLoader cl = new ConfigLoader();
		configList = cl.loadConfig(configPath);
		return configList;
	}

	// Needs to be synchronized because multiple threads can me invoking this
	// method at the same
	// time
	public synchronized void write(String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
			System.out.println(Thread.currentThread().getName() + " - Message sent to client " + clientNum+" "+msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
