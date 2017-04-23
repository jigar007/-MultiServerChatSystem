package Server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.ServerSocket;

public class hearClient extends Thread {
	
	private SSLServerSocket client;

	private int clientPort=0;
	
	private String configPath = "";
	
	
	public hearClient(SSLServerSocket client,int clientPort,String configPath){
		this.client = client;
		this.clientPort = clientPort;
		this.configPath = configPath;
	}
	
	@Override
	public void run(){
		try {
			//Create a server socket listening on port 4444
			 SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
						.getDefault();
			client = (SSLServerSocket) sslserversocketfactory.createServerSocket(clientPort);

			int clientNum = 0;
			//Listen for incoming connections for ever 
			while (true) {

				SSLSocket clientSocket=(SSLSocket) client.accept();
				clientNum++;
				ClientConnection clientConnection = new ClientConnection(clientSocket,clientNum,configPath);
				clientConnection.setName("client Thread" + clientNum);
				clientConnection.start();
				System.out.println("client enter");
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(client != null) {
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public ServerSocket getClient() {
		return client;
	}

	public int getClientPort() {
		return clientPort;
	}

}

