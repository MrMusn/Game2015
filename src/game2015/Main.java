package game2015;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application {

	private static final String NAME = "Saftevandsbælleren";
	private static final int X_POS = 6;
	private static final int Y_POS = 4;

	private static final int S_PORT = 55552;
	private static final int C_PORT = 55552;
	private static final int SCAN_PORT = 55559;
	private static final String IP_RANGE = "192.168.0.0/16";

	/** If null, scan ips automatically using range {@link Main#IP_RANGE} */
	private final static String[] ipArr = { "192.168.5.128" };

	private final static List<String> ips = new ArrayList<String>();

	/** Server socket listening for connections. Saved for destruction on exit. */
	private ServerSocket srvSock;

	/** Server socket for accepting incoming scans from other clients */
	private ServerSocket scanSrvSock;

	public static final int size = 20;
	public static final int scene_height = size * 20 + 100;
	public static final int scene_width = size * 20 + 200;

	public static Image image_floor;
	public static Image image_wall;
	public static Image hero_right, hero_left, hero_up, hero_down;
	public static Image fireHor, fireVer;
	public static Image fireDown, fireLeft, fireRight, fireUp;
	public static Image fireWallE, fireWallN, fireWallS, fireWallW;

	public static Player me;
	public List<Player> players = new ArrayList<Player>();

	private Queue<String> okQueue = new ConcurrentLinkedQueue<String>();
	private AtomicBoolean listenFlag = new AtomicBoolean(true);

	private Label[][] fields;
	private TextArea scoreList;

	private String[] board = { // 20x20
			"wwwwwwwwwwwwwwwwwwww",
			"w        ww        w",
			"w w  w  www w  w  ww",
			"w w  w   ww w  w  ww",
			"w  w               w",
			"w w w w w w w  w  ww",
			"w w     www w  w  ww",
			"w w     w w w  w  ww",
			"w   w w  w  w  w   w",
			"w     w  w  w  w   w",
			"w ww ww        w  ww",
			"w  w w    w    w  ww",
			"w        ww w  w  ww",
			"w         w w  w  ww",
			"w        w     w  ww",
			"w  w              ww",
			"w  w www  w w  ww ww",
			"w w      ww w     ww",
			"w   w   ww  w      w",
	"wwwwwwwwwwwwwwwwwwww" };

	/**
	 * Initializes connection to all other known clients (known ==
	 * {@link Main#ips} ) by sending the first name message
	 */
	private void initConnect() {
		new Thread(() -> {
			synchronized (Main.me) {
				Main.me.incTime();
			}
			for (String ip : ips) {
				final String nameStr = "name " + Main.me.getName() + " "
						+ Main.me.getXpos() + " " + Main.me.getYpos();
				Main.writeMsg(nameStr, ip);
			}
		}).start();
	}

	/**
	 * Write a message to a specified IP.<br>
	 * Note that all exceptions are discarded for convenience.<br>
	 * This is a static method to allow access from the Player class through
	 * {@link Main#broadcastPoints(Player, int)}.
	 *
	 * @param msg
	 *            The string message to send to an IP
	 * @param ip
	 *            The IP in string format, note that a bad IP will result in a
	 *            discarded exception
	 */
	private synchronized static void writeMsg(String msg, final String ip) {
		msg += " " + me.getTime() + "\n";

		Socket sock = null;
		try {
			sock = new Socket(ip, C_PORT);
			java.io.OutputStream os = sock.getOutputStream();
			os.write(msg.getBytes("UTF-8"));
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (sock != null)
					sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initializes the listening server thread, responsible for request
	 * delegation. Requests are separated by newline ('\n'), and data is
	 * separated by spaces (' ')<br>
	 * Note, writes must be made to a new socket connection and therefore the
	 * connections in this thread has nothing to do with writing.<br>
	 *
	 * @throws IOException
	 *             See {@link ServerSocket#accept()},
	 *             {@link Socket#getInputStream()},
	 *             {@link BufferedReader#readLine()}
	 */
	private void initListen() throws IOException {
		@SuppressWarnings("resource") // Closed on GUI shutdown
		ServerSocket srvSocket = new ServerSocket(S_PORT);
		srvSocket.setSoTimeout(2000);

		new Thread(new Runnable() {
			@Override
			public void run() {
				// Run until stopped. This handles request delegation
				while (listenFlag.get()) {
					Socket sock = null;
					try {
						sock = srvSocket.accept();
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(sock.getInputStream()));

						final String line = reader.readLine();
						final String[] lineArr = line.split(" ");

						int otherTime = Integer.parseInt(lineArr[lineArr.length - 1]);

						final InetAddress InetAdr = sock.getInetAddress();
						final Player player = getPlayerBySockAdr(InetAdr);

						if (line.toLowerCase().startsWith("name"))
							regPlayer(lineArr, InetAdr);
						else if (line.toLowerCase().startsWith("move"))
							regPlayerMove(lineArr, player);
						else if (line.toLowerCase().startsWith("point"))
							// TODO muligvis synkroniseringsproblem ved logisk
							// tid her? Mangler en "done" fra klienter, der
							// træder ud af critical region state
							regPlayerPoints(lineArr);
						else if (line.toLowerCase().startsWith("wait")) {
							if (Main.me.getTime() <= otherTime) {
								Main.me.setTime(otherTime);

								while (Main.me.getPLAYER_STATE().equals(Player.STATE.CR)) {
									//Wait while in STATE.CR
								}

								synchronized (Main.me) {
									Main.me.incTime();
								}

								Main.writeMsg("ok", InetAdr.toString().replace("/", ""));
							} else
								Main.this.okQueue.add(InetAdr.toString().replace("/", ""));
						} else if (line.toLowerCase().startsWith("ok"))
							synchronized(Main.me){
								Main.me.setOkCounter(Main.me.getOkCounter() + 1);
							}
					} catch (SocketTimeoutException e) {
						//Expected, do nothing
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							if (sock != null)
								sock.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

			}
		}).start();

		// IP scan server - accepts client queries
		if (ipArr == null)
			new Thread(() -> {
				try {
					this.scanSrvSock = new ServerSocket(SCAN_PORT);
					this.scanSrvSock.setSoTimeout(2000);
				} catch (Exception e) {
					e.printStackTrace();
				}

				final long start = System.currentTimeMillis();

				//Listen for 180 seconds
				while (start + (180 * 1000) > System.currentTimeMillis() && listenFlag.get()) {
					try {
						Socket sock = this.scanSrvSock.accept();

						final byte[] buffer = new byte[64];
						if (sock.getInputStream().read(buffer) > 0
								&& new String(buffer).startsWith("Knock knock")) {
							sock.getOutputStream().write(
									"Who is it?".getBytes("UTF-8"));
							System.out.println("Accepted connection");
						}
					} catch (Exception e) {
						// ignore
						// e.printStackTrace();
					}
					try {
						this.scanSrvSock.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
	}

	/**
	 * Register an opponent to the game, expects the request format:<br>
	 * name [NAME] [xPos] [yPos]
	 *
	 * @param reqLine
	 *            The request line, delimited by spaces into a String array
	 */
	private void regPlayer(final String[] reqLineArr, final InetAddress ip) {
		final Player p = new Player(reqLineArr[1],
				Integer.parseInt(reqLineArr[2]),
				Integer.parseInt(reqLineArr[3]), "up", ip);
		synchronized (this) {
			this.players.add(p);
		}

		Platform.runLater(() -> {
			// Add player to board
			this.fields[p.getXpos()][p.getYpos()].setGraphic(new ImageView(
					hero_up));
			// Update score list with new player
			this.scoreList.setText(getScoreList());
		});
	}

	/**
	 * Register a player moving and change the board accordingly to this<br>
	 * Format:<br>
	 * move [xPos] [yPos]
	 *
	 * @param reqLine
	 *            The request line, delimited by spaces into a String array
	 * @param ip
	 *            A SocketAddress from the requesting socket.
	 */
	private void regPlayerMove(final String[] reqLineArr, final Player player) {
		final int x = Integer.parseInt(reqLineArr[1]);
		final int y = Integer.parseInt(reqLineArr[2]);

		if (player == null)
			throw new RuntimeException(
					"Unknown player address for position change");

		Platform.runLater(() -> {
			if (player.getXpos() - x < 0)
				playerMoved(player, 1, 0, "right"); // right
			else if (player.getXpos() - x > 0)
				playerMoved(player, -1, 0, "left"); // left
			else if (player.getYpos() - y < 0)
				playerMoved(player, 0, 1, "down"); // down
			else if (player.getYpos() - y > 0)
				playerMoved(player, 0, -1, "up"); // up
		});
	}

	/**
	 * Register a change in player points. Blindly trusting the client.<br>
	 * Format:<br>
	 * point [NAME] [newScore]
	 *
	 * @param reqLineArr
	 *            The point line separated by spaces into an array
	 */
	private void regPlayerPoints(final String[] reqLineArr) {
		final Player player = getPlayerByName(reqLineArr[1]);

		if (player == null)
			throw new RuntimeException(
					"Unknown name received for point change.");

		final int pointsChange = Integer.parseInt(reqLineArr[2])
				- player.getPoints();
		player.addPoints(pointsChange, false);

		Platform.runLater(() -> {
			// Update score list with player's new score
			this.scoreList.setText(getScoreList());
		});
	}

	/**
	 * Get a player by a SocketAddress
	 *
	 * @param ip
	 *            A SocketAddress matching a player's
	 * @return A known player object or null if not found
	 */
	private synchronized Player getPlayerBySockAdr(final InetAddress ip) {
		for (Player p : this.players)
			if (p.getIp() != null && p.getIp().toString().equals(ip.toString()))
				return p;

		return null;
	}

	private synchronized Player getPlayerByName(final String name) {
		for (Player p : this.players)
			if (p.getName().equalsIgnoreCase(name))
				return p;

		return null;
	}

	// -------------------------------------------
	// | Maze: (0,0) | Score: (1,0) |
	// |-----------------------------------------|
	// | boardGrid (0,1) | scorelist |
	// | | (1,1) |
	// -------------------------------------------

	@Override
	public void start(Stage primaryStage) {
		try {
			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(0, 10, 0, 10));

			Text mazeLabel = new Text("Maze:");
			mazeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

			Text scoreLabel = new Text("Score:");
			scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

			this.scoreList = new TextArea();

			GridPane boardGrid = new GridPane();

			image_wall = new Image(getClass().getResourceAsStream(
					"Image/wall4.png"), size, size, false, false);
			image_floor = new Image(getClass().getResourceAsStream(
					"Image/floor1.png"), size, size, false, false);

			hero_right = new Image(getClass().getResourceAsStream(
					"Image/heroRight.png"), size, size, false, false);
			hero_left = new Image(getClass().getResourceAsStream(
					"Image/heroLeft.png"), size, size, false, false);
			hero_up = new Image(getClass().getResourceAsStream(
					"Image/heroUp.png"), size, size, false, false);
			hero_down = new Image(getClass().getResourceAsStream(
					"Image/heroDown.png"), size, size, false, false);

			fireHor = new Image(getClass().getResourceAsStream("Image/fireHorizontal.png"), size, size, false, false);
			fireVer = new Image(getClass().getResourceAsStream("Image/fireVertical.png"), size, size, false, false);
			fireDown = new Image(getClass().getResourceAsStream("Image/fireDown.png"), size, size, false, false);
			fireLeft = new Image(getClass().getResourceAsStream("Image/fireLeft.png"), size, size, false, false);
			fireRight = new Image(getClass().getResourceAsStream("Image/fireRight.png"), size, size, false, false);
			fireUp = new Image(getClass().getResourceAsStream("Image/fireUp.png"), size, size, false, false);
			fireWallE = new Image(getClass().getResourceAsStream("Image/fireWallEast.png"), size, size, false, false);
			fireWallN = new Image(getClass().getResourceAsStream("Image/fireWallNorth.png"), size, size, false, false);
			fireWallS = new Image(getClass().getResourceAsStream("Image/fireWallSouth.png"), size, size, false, false);
			fireWallW = new Image(getClass().getResourceAsStream("Image/fireWallWest.png"), size, size, false, false);

			this.fields = new Label[20][20];
			for (int j = 0; j < 20; j++)
				for (int i = 0; i < 20; i++) {
					switch (this.board[j].charAt(i)) {
					case 'w':
						this.fields[i][j] = new Label("", new ImageView(
								image_wall));
						break;
					case ' ':
						this.fields[i][j] = new Label("", new ImageView(
								image_floor));
						break;
					default:
						throw new Exception("Illegal field value: "
								+ this.board[j].charAt(i));
					}
					boardGrid.add(this.fields[i][j], i, j);
				}
			this.scoreList.setEditable(false);

			grid.add(mazeLabel, 0, 0);
			grid.add(scoreLabel, 1, 0);
			grid.add(boardGrid, 0, 1);
			grid.add(this.scoreList, 1, 1);

			Scene scene = new Scene(grid, scene_width, scene_height);
			primaryStage.setScene(scene);
			primaryStage.show();

			scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
				switch (event.getCode()) {
				case UP:
					playerMoved(0, -1, "up");
					break;
				case DOWN:
					playerMoved(0, +1, "down");
					break;
				case LEFT:
					playerMoved(-1, 0, "left");
					break;
				case RIGHT:
					playerMoved(+1, 0, "right");
					break;
				case SPACE:
					try {
						fire(me);
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case L:
					if (ipArr == null)
						ips.addAll(new IPScan(IP_RANGE, SCAN_PORT).scan());
					else
						for (String ip : ipArr)
							ips.add(ip);
					initConnect();
					break;
				default:
					break;
				}
			});

			// Setting up me
			Main.me = new Player(NAME, X_POS, Y_POS, "up");
			randomizePos(me);
			synchronized (this) {
				this.players.add(Main.me);
			}
			this.fields[Main.me.getXpos()][Main.me.getYpos()].setGraphic(new ImageView(hero_up));

			this.scoreList.setText(getScoreList());

			initListen();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void stop() throws IOException {
		if (this.srvSock != null)
			this.srvSock.close();
		if (this.scanSrvSock != null && !this.scanSrvSock.isClosed())
			this.scanSrvSock.close();

		listenFlag.set(false);
	}

	private void randomizePos(final Player player) {
		// 0-19
		final Random rnd = new Random();

		int x, y;
		do {
			x = rnd.nextInt(20);
			y = rnd.nextInt(20);
		} while (this.board[y].charAt(x) == 'w' || getPlayerAt(x, y) != null);

		player.setXpos(x);
		player.setYpos(y);
	}

	public void playerMoved(int delta_x, int delta_y, String direction) {
		Main.me.setPLAYER_STATE(Player.STATE.VENTER_OK);

		synchronized (Main.me) {
			Main.me.incTime();
		}
		for (String ip : ips)
			Main.writeMsg("WAIT", ip);

		while (Main.me.getOkCounter() < this.players.size() - 1) {

		}

		playerMoved(Main.me, delta_x, delta_y, direction);
		Main.me.setOkCounter(0);
		Main.me.setPLAYER_STATE(Player.STATE.IDLE);

		for (String ip : this.okQueue)
			Main.writeMsg("ok", ip);
	}

	public void playerMoved(Player player, int delta_x, int delta_y,
			String direction) {
		player.setDirection(direction);
		int x = player.getXpos(), y = player.getYpos();

		if (this.board[y + delta_y].charAt(x + delta_x) == 'w') {
			if (player.equals(Main.me))
				player.addPoints(-1);
		} else {
			Player p = getPlayerAt(x + delta_x, y + delta_y);
			if (p != null) {
				if (player.equals(Main.me)) {
					player.addPoints(10);
					p.addPoints(-10);
				}
			} else {
				if (player.equals(Main.me))
					player.addPoints(1);

				this.fields[x][y].setGraphic(new ImageView(image_floor));
				x += delta_x;
				y += delta_y;

				if (direction.equals("right"))
					this.fields[x][y].setGraphic(new ImageView(hero_right));

				if (direction.equals("left"))
					this.fields[x][y].setGraphic(new ImageView(hero_left));

				if (direction.equals("up"))
					this.fields[x][y].setGraphic(new ImageView(hero_up));

				if (direction.equals("down"))
					this.fields[x][y].setGraphic(new ImageView(hero_down));


				player.setXpos(x);
				player.setYpos(y);

				if (player.equals(Main.me))
					broadcastMove();
			}
		}
		this.scoreList.setText(getScoreList());

	}

	/**
	 * Broadcast point changes to all ips
	 *
	 * @param player
	 *            The player whose points has changed
	 * @param points
	 *            The new amount of points for this player
	 */
	public static void broadcastPoints(final Player player, final int points) {
		synchronized (Main.me) {
			Main.me.incTime();
		}
		for (final String ip : ips)
			Main.writeMsg("POINT " + player.getName() + " " + points, ip);
	}

	/**
	 * Broadcast to all ips that you just moved to a new position
	 */
	public void broadcastMove() {
		synchronized (Main.me) {
			Main.me.incTime();
		}
		for (final String ip : ips)
			Main.writeMsg("move " + Main.me.getXpos() + " " + Main.me.getYpos(), ip);
	}

	public synchronized String getScoreList() {
		StringBuffer b = new StringBuffer(100);
		for (Player p : this.players)
			b.append(p + "\r\n");
		return b.toString();
	}

	public synchronized Player getPlayerAt(int x, int y) {
		for (Player p : this.players)
			if (p.getXpos() == x && p.getYpos() == y)
				return p;
		return null;
	}

	/*
	 * Game weapon logic
	 */

	public void fire(final Player player) throws IllegalArgumentException, IllegalAccessException {
		final String dir = player.getDirection();
		final int x = player.getXpos();
		final int y = player.getYpos();

		//Collect coordinates until wall or player hit
		final List<Point> cords = new ArrayList<Point>();
		int curX = x, curY = y;
		Point p;
		Player playerHit = null;

		switch (dir) {
		case "up":
			while (!isWall((p = new Point(curX, --curY))) && (playerHit = getPlayerAt(p.x, p.y)) == null)
				cords.add(p);
			break;
		case "down":
			while (!isWall((p = new Point(curX, ++curY))) && (playerHit = getPlayerAt(p.x, p.y)) == null)
				cords.add(p);
			break;
		case "right":
			while (!isWall((p = new Point(++curX, curY))) && (playerHit = getPlayerAt(p.x, p.y)) == null)
				cords.add(p);
			break;
		case "left":
			while (!isWall((p = new Point(--curX, curY))) && (playerHit = getPlayerAt(p.x, p.y)) == null)
				cords.add(p);
			break;
		default:
			final RuntimeException up = new RuntimeException("Invalid direction for player " + player.getName());
			throw up;
		}

		// In case we stopped iterating above because of encountering a player,
		// add that Point. We need to be able to get the position of this player
		// for removal.
		if (playerHit != null)
			cords.add(p);

		if (cords.size() > 0)
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					//Draw directional start of fire
					try {
						fields[cords.get(0).x][cords.get(0).y].setGraphic(getFireImgViewByDir(dir));
					} catch (IllegalArgumentException | IllegalAccessException e1) {
						e1.printStackTrace();
					}

					//Draw vertical or horizontal fire
					if (cords.size() > 1)
						for (int iPoint = 1; iPoint < cords.size() - 1; ++iPoint)
							switch (dir) {
							case "up":
							case "down":
								fields[cords.get(iPoint).x][cords.get(iPoint).y].setGraphic(new ImageView(fireVer));
								break;
							case "right":
							case "left":
								fields[cords.get(iPoint).x][cords.get(iPoint).y].setGraphic(new ImageView(fireHor));
								break;
							}

					//Draw wall collision if no player hit
					final Point lastCord = cords.get(cords.size() - 1);

					final Player playerKilled;

					if ((playerKilled = getPlayerAt(lastCord.x, lastCord.y)) == null)
						fields[lastCord.x][lastCord.y].setGraphic(getFireWallImgViewByDir(dir));
					else {
						fields[lastCord.x][lastCord.y].setGraphic(getFireWallImgViewByDir(dir));
						killPlayer(playerKilled, player);
					}

					String debugStr = playerKilled == null ? "Wall hit" : "Player hit";
					System.out.println(debugStr);
				}
			});
	}

	private void killPlayer(final Player killed, final Player killer) {
		if (killer.equals(me)) {
			killed.addPoints(-100);
			killer.addPoints(100);
		}

		//Redraw killed player
		Platform.runLater(() -> {
			fields[killed.getXpos()][killed.getYpos()].setGraphic(new ImageView(image_floor));

			randomizePos(killed);
			try {
				fields[killed.getXpos()][killed.getYpos()].setGraphic(getHeroImgViewByDir(killed.getDirection()));
			} catch (Exception e) {e.printStackTrace();}
		});


		Platform.runLater(() -> {
			this.scoreList.setText(getScoreList());
		});
	}

	//hero_up, hero_down...
	private ImageView getHeroImgViewByDir(final String dir) throws IllegalArgumentException, IllegalAccessException {
		for (Field classField : this.getClass().getFields())
			if (classField.getName().equalsIgnoreCase("hero_" + dir))
				return new ImageView((Image) classField.get(null));

		return null;
	}

	/**
	 * Iterates through all fields from this class to get the appropriate fire Image
	 *
	 * @param dir A String direction
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private ImageView getFireImgViewByDir(final String dir) throws IllegalArgumentException, IllegalAccessException {
		for (Field classField : this.getClass().getFields())
			if (classField.getName().equalsIgnoreCase("fire" + dir))
				return new ImageView((Image) classField.get(null));

		return null;
	}

	private ImageView getFireWallImgViewByDir(final String dir) {
		switch (dir) {
		case "up":
			return new ImageView(fireWallN);
		case "down":
			return new ImageView(fireWallS);
		case "right":
			return new ImageView(fireWallE);
		case "left":
			return new ImageView(fireWallW);
		default:
			return null;
		}
	}

	private boolean isWall(final int x, final int y) {
		return this.board[y].charAt(x) == 'w';
	}

	private boolean isWall(Point point) {
		return isWall(point.x, point.y);
	}

	/** @Immutable */
	private class Point {
		public final int x;
		public final int y;

		public Point(int x, int y) { this.x = x; this.y = y; }
	}

	public static void main(String[] args) {
		Application.launch(args);
	}
}
