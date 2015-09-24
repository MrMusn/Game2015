package game2015;

import java.io.BufferedReader;
import java.io.OutputStream;

public class Player {
	String name;
	int xpos;
	int ypos;
	int point;
	String direction;
	private BufferedReader reader;
	private OutputStream os;

	public Player(String name, int xpos, int ypos, String direction) {
		this.name = name;
		this.xpos = xpos;
		this.ypos = ypos;
		this.direction = direction;
		this.point = 0;
	}

	public BufferedReader getReader() {
		return this.reader;
	}

	public void setReader(BufferedReader reader) {
		this.reader = reader;
	}

	public OutputStream getOs() {
		return this.os;
	}

	public void setOs(OutputStream os) {
		this.os = os;
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
