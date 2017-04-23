package Server;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class heartBeat {

	Socket heartBeat = null;
	public static PrintWriter outputStream = null;
	List<Config> configList = new ArrayList<Config>();

	public heartBeat(String configPath) throws UnknownHostException, FileNotFoundException {
		ConfigLoader cl = new ConfigLoader();
		configList = cl.loadConfig(configPath);
		for (Config i : configList) {
			try {
				if (i.getServerStatus().equals("on") && !i.getServerid().equals(ServerInfoPool.getInstance().getLocalServerInfo().getServerName())) {
					heartBeat = new Socket(i.getServerAddress(), i.getCoordinationPort());
					System.out.println("Server " + i.getServerid() + " is normal.");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Server " + i.getServerid() + " has crashed!!!");
				i.setServerStatus("off");
				outputStream = new PrintWriter(new FileOutputStream(configPath));
				for (Config cf2 : configList) {
					outputStream.write(cf2.getServerid() + "\t" + cf2.getServerAddress() + "\t" + cf2.getClientPort()
							+ "\t" + cf2.getCoordinationPort() + "\t" + cf2.getServerStatus() + "\n");
				}
				outputStream.flush();
				outputStream.close();
				@SuppressWarnings({ "unused", "rawtypes" })
				Collection<ChatroomInfo> roomList = ServerInfoPool.getInstance().getChatroomMap().values();
				List<String> waitingForDelete = new ArrayList<String>();
				for (ChatroomInfo ci : roomList) {
					if (ci instanceof RemoteChatroomInfo) {
						if (((RemoteChatroomInfo) ci).getServerInfo().getServerName().equals(i.getServerid())) {
							waitingForDelete.add(ci.getChatroomid());
						}
					}
				}
				for (String s : waitingForDelete) {
					ServerInfoPool.getInstance().chatroomDisconnected(s);
				}

				//e.printStackTrace();
			}

		}
	}

}
