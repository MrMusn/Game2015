package game2015;

import java.net.Socket;

public class Player {
	String name;
	int xpos;
	int ypos;
	int point;
	String direction;
	private Socket socket;

	public Player(String name, int xpos, int ypos, String direction) {
		this.name = name;
		this.xpos = xpos;
		this.ypos = ypos;
		this.direction = direction;
		this.point = 0;
	}

	public void setSocket(Socket sock) {
		this.socket = sock;
	}

	public String getName() {
		return this.name;
	}

	public int getXpos() {
		return xpos;
	}
	public void setXpos(int xpos) {
		this.xpos = xpos;
	}
	public int getYpos() {
		return ypos;
	}
	public void setYpos(int ypos) {
		this.ypos = ypos;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public void addPoints(int p) {
		point+=p;
	}
	@Override
	public String toString() {
		return name+":   "+point;
	}
}
