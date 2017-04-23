/*
Author:Chi Zhang
This is a chatroom server main class
Date: 24/9/2016
*/

package Server;

import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import javax.net.ssl.SSLServerSocket;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server {

	@SuppressWarnings("static-access")
	public static Scanner keyboard = new Scanner(System.in);
	public static PrintWriter outputStream = null;

	public static void main(String[] args) throws InterruptedException, IOException {
		/*---------------------------------------------------------------*/
		// security ssl serversocket prepare
		System.setProperty("javax.net.ssl.keyStore", "mykeystore");
		// Password to access the private key from the keystore file
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");

		// Enable debugging to view the handshake and communication which
		// happens between the SSLClient and the SSLServer


		System.setProperty("javax.net.ssl.trustStore", "mykeystore");
		// TODO Auto-generated method stub
		SSLServerSocket listeningClient = null;
		SSLServerSocket listeningServer = null;

		// the server name which input in argument
		String serverid = "";
		// the server address which input in argument
		String configPath = "";

		List<Config> configList = new ArrayList<Config>();

		ComLineValue clv = new ComLineValue();

		CmdLineParser clp = new CmdLineParser(clv);

		try {
			clp.parseArgument(args);
			// get the serverid from arguments
			serverid = clv.getServerid();
			// get the config.txt path from arguments
			configPath = clv.getServerConfPath();
			ConfigLoader cl = new ConfigLoader();
			configList = cl.loadConfig(configPath);
			boolean addServerSucc = false;
			/// if serverid exist

				for (Config cf : configList) {
				if (serverid.equals(cf.getServerid()) && cf.getServerStatus().equals("off")) {
					System.out.println("server"+cf.getServerid()+" connected.");
					ServerInfo si = new ServerInfo();
					si.setServerName(cf.getServerid());
					si.setAddress(cf.getServerAddress());
					si.setClientPort(cf.getClientPort());
					si.setCoordinationPort(cf.getCoordinationPort());
					ServerInfoPool.getInstance().setLocalServerInfo(si);
					Thread clientThread = new hearClient(listeningClient, cf.getClientPort(), configPath);
					Thread serverThread = new hearServer(listeningServer, cf.getCoordinationPort(),configPath);
					Runtime.getRuntime().addShutdownHook(clientThread);

					clientThread.start();
					serverThread.start();
					JSONObject tellMeYourChatrooms = new JSONObject();
					tellMeYourChatrooms.put("type", "tellMeYourChatrooms");
					tellMeYourChatrooms.put("serverid",ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
					String sendToServer = tellMeYourChatrooms.toJSONString();
					for (Config i : configList) {
						if (!i.getServerid().equals(ServerInfoPool.getInstance().getLocalServerInfo().getServerName())
								&& i.getServerStatus().equals("on")) {
							serverToServer sts = new serverToServer(i.getServerAddress(), i.getCoordinationPort());
							sts.openSocket(sendToServer);

						}
					}
					cf.setServerStatus("on");
					outputStream = new PrintWriter(new FileOutputStream(configPath));
					for (Config cf2 : configList) {
						outputStream
								.write(cf2.getServerid() + "\t" + cf2.getServerAddress() + "\t" + cf2.getClientPort()
										+ "\t" + cf2.getCoordinationPort() + "\t" + cf2.getServerStatus() + "\n");
						outputStream.flush();
					}
					outputStream.close();

					addServerSucc = true;
					break;
				} else if (serverid.equals(cf.getServerid()) && cf.getServerStatus().equals("on")) {
					System.out.println("Warning: This server is running, please change another one!");
					System.exit(0);
				}

			}
			// serverid not exist
			if (addServerSucc == false) {
				String newServerAddress = "";
				int newServerClientPort = -1;
				int newServerCoordiationPort = -1;
				String newServerStatus = "off";
				boolean hasSamePort = false;
				System.out.println(
						"Warning:Server ID not exist, please input address,client port and coordination port!");
				while (true) {
					String otherServerInfo = keyboard.nextLine();
					String[] otherServerInfoArray = otherServerInfo.split(" ");
					for (Config cf : configList) {
						if (cf.getClientPort() == Integer.parseInt(otherServerInfoArray[1])
								|| cf.getCoordinationPort() == Integer.parseInt(otherServerInfoArray[1])) {
							System.out.println("Warning: This client port has existed,please input again!");
							hasSamePort = true;
							break;
						} else if (cf.getClientPort() == Integer.parseInt(otherServerInfoArray[2])
								|| cf.getCoordinationPort() == Integer.parseInt(otherServerInfoArray[2])) {
							System.out.println("Warning: This coordination port has existed,please input again!");
							hasSamePort = true;
							break;
						}
					}
					if (hasSamePort == false) {
						ServerInfo si = new ServerInfo();
						addServerSucc = true;
						newServerAddress = otherServerInfoArray[0];
						newServerClientPort = Integer.parseInt(otherServerInfoArray[1]);
						newServerCoordiationPort = Integer.parseInt(otherServerInfoArray[2]);
						newServerStatus = "on";
						// set new server info into serverinfo instance
						si.setServerName(serverid);
						si.setAddress(newServerAddress);
						si.setClientPort(newServerClientPort);
						si.setCoordinationPort(newServerCoordiationPort);
						ServerInfoPool.getInstance().setLocalServerInfo(si);

						Thread clientThread = new hearClient(listeningClient, newServerClientPort, configPath);
						Thread serverThread = new hearServer(listeningServer, newServerCoordiationPort,configPath);

						clientThread.start();
						serverThread.start();
						JSONObject tellMeYourChatrooms = new JSONObject();
						tellMeYourChatrooms.put("type", "tellMeYourChatrooms");
						tellMeYourChatrooms.put("serverid",ServerInfoPool.getInstance().getLocalServerInfo().getServerName());
						String sendToServer = tellMeYourChatrooms.toJSONString();
						
						for (Config i : configList) {
							if (!i.getServerid()
									.equals(ServerInfoPool.getInstance().getLocalServerInfo().getServerName())
									&& i.getServerStatus().equals("on")) {
								serverToServer sts = new serverToServer(i.getServerAddress(), i.getCoordinationPort());
								sts.openSocket(sendToServer);

							}
						}
						Config newcf = new Config(serverid, newServerAddress, newServerClientPort,
								newServerCoordiationPort, newServerStatus);

						configList.add(newcf);
						outputStream = new PrintWriter(new FileOutputStream(configPath));
						for (Config cf : configList) {
							outputStream
									.write(cf.getServerid() + "\t" + cf.getServerAddress() + "\t" + cf.getClientPort()
											+ "\t" + cf.getCoordinationPort() + "\t" + cf.getServerStatus() + "\n");
						}
						outputStream.flush();
						outputStream.close();
						break;
					}

				}
			}

			while (true) {
				Thread.sleep(2000);
				heartBeat hb = new heartBeat(configPath);
			}

		} catch (CmdLineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
