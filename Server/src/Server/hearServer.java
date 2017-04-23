package Server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class hearServer extends Thread {
	
	private SSLServerSocket listeningServer;
	
	private int coordinationPort = 0;
	
	String configPath = "";

	public hearServer(SSLServerSocket listeningServer,int coordinationPort,String configPath){
		this.listeningServer = listeningServer;
		this.coordinationPort = coordinationPort;
		this.configPath = configPath;
	}
	
	@Override
	public void run(){
		try {
			//Create a server socket listening on port 4444
			 SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
						.getDefault();
			 listeningServer = (SSLServerSocket) sslserversocketfactory.createServerSocket(coordinationPort);
			
			int serverNum = 0;
			
			//Listen for incoming connections for ever 
			while (true) {
				Socket serverSocket = listeningServer.accept();
				serverNum++;
				ServerConnection serverConnection = new ServerConnection(serverSocket,serverNum,configPath);
				serverConnection.setName("server Thread"+ serverNum);
				serverConnection.start();
				System.out.println("server enter");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(listeningServer != null) {
				try {
					listeningServer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public ServerSocket getListeningServer() {
		return listeningServer;
	}

	public int getCoordinationPort() {
		return coordinationPort;
	}

}
