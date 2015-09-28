package game2015;

import java.net.InetAddress;

public class Player {
	private String name;
	private int xpos;
	private int ypos;
	private int point;
	private String direction;
	private final InetAddress ip;

	/**
	 * Construct the 'me' player with a null ip, indicating the resulting object
	 * should not have IO operations performed.
	 * 
	 * @param name
	 * @param xpos
	 * @param ypos
	 * @param direction
	 */
	public Player(String name, int xpos, int ypos, String direction) {
		this(name, xpos, ypos, direction, null);
	}
	
	/**
	 * Construct an opponent player with a specific IP for IO operations
	 * 
	 * @param name
	 * @param xpos
	 * @param ypos
	 * @param direction
	 * @param ip
	 */
	public Player(String name, int xpos, int ypos, String direction, InetAddress ip) {
		this.name = name;
		this.xpos = xpos;
		this.ypos = ypos;
		this.direction = direction;
		this.point = 0;
		this.ip = ip;
	}
	
	public final String getName() {
		return this.name;
	}

	public final InetAddress getIp() {
		return this.ip;
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
	
	/**
	 * Update points for this player and broadcast points to all ips by default.
	 * @param p
	 */
	public void addPoints(int p) {
		addPoints(p, true);
	}
	
	/**
	 * Update points for this player and optionally broadcast new score to all ips
	 * @param p
	 * @param broadcast
	 */
	public void addPoints(int p, final boolean broadcast) {
		point+=p;
		if (broadcast)
			Main.broadcastPoints(this, this.point);
	}
	
	public final int getPoints() {
		return this.point;
	}
	
	public String toString() {
		return name+":   "+point;
	}
}
