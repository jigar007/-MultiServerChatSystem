package au.edu.unimelb.tcp.client;

import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.UnknownHostException;

public class Client {

	public static void main(String[] args) throws IOException, ParseException {
		/*---------------------------------------------------------------*/
		// Set the key store to use for validating the server cert.  
		System.setProperty("javax.net.ssl.trustStore", "mykeystore");
		System.setProperty("javax.net.debug","all");

		SSLSocket socket = null;
		String identity = null;
		/*-----------------------------------------*/
		//initilize the password as null

		String password = null;
		boolean debug = false;

		try {
			//load command line args
			ComLineValues values = new ComLineValues();
			CmdLineParser parser = new CmdLineParser(values);
			try {
				parser.parseArgument(args);
				String hostname = values.getHost();
				identity = values.getIdeneity();
				/*------------------------------------*/
				password= values.getPassword();
				int port = values.getPort();
				debug = values.isDebug();
				
				/*---------------------------------------------------------------*/
				//ssl socket
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				socket = (SSLSocket) sslsocketfactory.createSocket(hostname, port);


			} catch (CmdLineException e) {
				e.printStackTrace();
			}
			
			State state = new State(identity, "",password);
			
			// start sending thread
			MessageSendThread messageSendThread = new MessageSendThread(socket, state, debug);
			Thread sendThread = new Thread(messageSendThread);
			sendThread.start();
			
			// start receiving thread
			Thread receiveThread = new Thread(new MessageReceiveThread(socket, state, messageSendThread, debug));
			receiveThread.start();
			
		} catch (UnknownHostException e) {
			System.out.println("Unknown host");
		} catch (IOException e) {
			System.out.println("Communication Error: " + e.getMessage());
		}
	}
}
