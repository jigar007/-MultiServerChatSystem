package Server;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.UnknownHostException;


public class serverToServer {
	
	String hostName = "";
	int port = 0;
	ClientConnection clientConnection;
	SSLSocket socket = null;
	
	public serverToServer(String hostName,int port, ClientConnection clientConnection){
		this.hostName = hostName;
		this.port = port;
		this.clientConnection = clientConnection;
	}
	
	public serverToServer(String hostName,int port) {
		// TODO Auto-generated constructor stub
		this.hostName = hostName;
		this.port = port;
	}
	
	public void openSocket(String command) throws InterruptedException{
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket socket = (SSLSocket) sslsocketfactory.createSocket(hostName, port);
			serverToServerThread newThread = new serverToServerThread(socket, command, clientConnection);
			newThread.start();
			newThread.join();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
