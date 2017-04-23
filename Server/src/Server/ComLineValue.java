package Server;

import org.kohsuke.args4j.Option;

public class ComLineValue {
	@Option(required=true, name="-n", aliases={"--serverid"} ,usage="serverid is the name of the server")
	private String serverid;
	@Option(required=true,name="-l",aliases={"--server_conf"},usage=" servers_conf is the path to a text file containing the configuration of servers")
	private String serverConfPath;
	
	public String getServerid() {
		return serverid;
	}
	public String getServerConfPath() {
		return serverConfPath;
	}

}
