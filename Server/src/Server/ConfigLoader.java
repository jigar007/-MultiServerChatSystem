package Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConfigLoader {

	public static List<Config> loadConfig(String path) {
		List<Config> configList=new ArrayList<Config>();
		File inputFile = new File(path);
		Scanner inputStream;
		try {
			inputStream = new Scanner(new FileInputStream(inputFile));

			while (inputStream.hasNextLine()) {
				String configLine = inputStream.nextLine();
				if (configLine != null) {
					String[] configParams = configLine.split("\t");
					String serverid = configParams[0];
					String serverAddress = configParams[1];
					int clientPort = Integer.parseInt(configParams[2]);
					int coordinationPort = Integer.parseInt(configParams[3]);
					String serverStatus = configParams[4];
					configList.add(new Config(serverid, serverAddress, clientPort, coordinationPort,serverStatus));
					//System.out.println(config[configNum].equals(null));
				}
			}
			inputStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return configList;
	}
}