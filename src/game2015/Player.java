package game2015;

import java.net.InetAddress;

public class Player {
	private String name;
	private int xpos;
	private int ypos;
	private int point;
	private String direction;
	private final InetAddress ip;
	private volatile int okCounter = 0;

	public enum STATE {
		IDLE, VENTER_OK, CR;
	}

	private STATE PLAYER_STATE = STATE.IDLE;
	private int time = 0;

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
	public Player(String name, int xpos, int ypos, String direction,
			InetAddress ip) {
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
		return this.xpos;
	}

	public void setXpos(int xpos) {
		this.xpos = xpos;
	}

	public int getYpos() {
		return this.ypos;
	}

	public void setYpos(int ypos) {
		this.ypos = ypos;
	}

	public String getDirection() {
		return this.direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	/**
	 * Update points for this player and broadcast points to all ips by default.
	 *
	 * @param p
	 */
	public void addPoints(int p) {
		addPoints(p, true);
	}

	/**
	 * Update points for this player and optionally broadcast new score to all
	 * ips
	 *
	 * @param p
	 * @param broadcast
	 */
	public void addPoints(int p, final boolean broadcast) {
		this.point += p;
		if (broadcast) {
			Main.broadcastPoints(this, this.point);
		}
	}

	public final int getPoints() {
		return this.point;
	}

	@Override
	public String toString() {
		return this.name + ":   " + this.point;
	}

	public STATE getPLAYER_STATE() {
		return this.PLAYER_STATE;
	}

	public void setPLAYER_STATE(STATE state) {
		this.PLAYER_STATE = state;
	}

	public synchronized int getOkCounter() {
		return this.okCounter;
	}

	public synchronized void setOkCounter(int okCounter) {
		this.okCounter = okCounter;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public void incTime() {
		this.time++;
		System.out.println("My time is: " + this.time);
	}

	public int getTime() {
		return this.time;
	}
}
