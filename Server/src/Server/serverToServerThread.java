package Server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.SSLSocket;
import java.io.*;

public class serverToServerThread extends Thread {

	private SSLSocket socket;
	private BufferedReader reader;
	private DataOutputStream writer;
	private ClientConnection clientConnection;
	private String command;

	public serverToServerThread(SSLSocket socket, String command, ClientConnection clientConnection) {
		this.socket = socket;
		this.command = command;
		this.clientConnection = clientConnection;
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			writer = new DataOutputStream(socket.getOutputStream());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		String receive = "";
		JSONObject receiveMsg = new JSONObject();
		JSONParser parser = new JSONParser();
		if (command.contains("lockidentity")) {
			write(command);
			while (true) {
				try {
					if (!(receive = reader.readLine()).equals("")) {
						try {
							receiveMsg = (JSONObject) parser.parse(receive);
							clientConnection.lockIdentity(receiveMsg.get("locked").toString());
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				System.out.println("connection close");
				reader.close();
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}else if (command.contains("releaseidentity")){
			write(command);
			
		}else if(command.contains("lockroomid")){
			write(command);
			while(true){
				try {
					if (!(receive = reader.readLine()).equals("")) {
						try {
							receiveMsg = (JSONObject) parser.parse(receive);
							clientConnection.lockChatroom(receiveMsg.get("locked").toString());
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else if(command.contains("releaseroomid")){
			write(command);
		}else if(command.contains("deleteroom")){
			write(command);
		}else if(command.contains("tellMeYourChatrooms")){
			write(command);
			while (true) {
				try {
					if (!(receive = reader.readLine()).equals("") && receive.contains("replyForChatRooms")) {
						try {
							receiveMsg = (JSONObject) parser.parse(receive);
							String serverName = (String) receiveMsg.get("serverid");
							String address = (String) receiveMsg.get("serverAddress");
							int clientPort = Integer.parseInt(receiveMsg.get("clientPort").toString());
							int coordinationPort = Integer.parseInt(receiveMsg.get("coordinatinPort").toString());
							String[] chatroomList = ((String) receiveMsg.get("chatrooms")).split(" ");
							ServerInfo si = new ServerInfo();
							si.setServerName(serverName);
							si.setAddress(address);
							si.setClientPort(clientPort);
							si.setCoordinationPort(coordinationPort);
							for(int i=0;i<chatroomList.length;i++){
								RemoteChatroomInfo rci = new RemoteChatroomInfo();
								rci.setChatroomid(chatroomList[i]);
								rci.setServerInfo(si);
								ServerInfoPool.getInstance().chatroomConnected(chatroomList[i], rci);
							}
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void write(String msg) {
		try {
			writer.write((msg + '\n').getBytes("UTF-8"));
			writer.flush();
			System.out.println(Thread.currentThread().getName() + " - Message sent to server ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}