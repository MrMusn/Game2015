package game2015;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

	private static final String NAME = "Henning5000";
	private static final int X_POS = 9;
	private static final int Y_POS = 4;

	private static final int S_PORT = 55552;
	private static final int C_PORT = 55552;
	private static final int SCAN_PORT = 55559;
	private static final String IP_RANGE = "192.168.0.0/16";

	/** If null, scan ips automatically using range {@link Main#IP_RANGE} */
	private final static String[] ipArr = { "10.10.139.229"};
	/**
	 * { "10.10.133.157", "10.10.140.154", "10.10.140.228", "10.10.149.132" };
	 * // Anders, Muddz, Simon, Mr // Adem
	 **/
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

	public static Player me;
	public List<Player> players = new ArrayList<Player>();

	private Label[][] fields;
	private TextArea scoreList;

	private String[] board = { // 20x20
			"wwwwwwwwwwwwwwwwwwww", "w        ww        w", "w w  w  www w  w  ww",
			"w w  w   ww w  w  ww", "w  w               w",
			"w w w w w w w  w  ww", "w w     www w  w  ww",
			"w w     w w w  w  ww", "w   w w  w  w  w   w",
			"w     w  w  w  w   w", "w ww ww        w  ww",
			"w  w w    w    w  ww", "w        ww w  w  ww",
			"w         w w  w  ww", "w        w     w  ww",
			"w  w              ww", "w  w www  w w  ww ww",
			"w w      ww w     ww", "w   w   ww  w      w",
	"wwwwwwwwwwwwwwwwwwww" };

	/**
	 * Initializes connection to all other known clients (known ==
	 * {@link Main#ips} ) by sending the first name message
	 */
	private void initConnect() {
		new Thread(() -> {
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
		@SuppressWarnings("resource")
		// Closed on GUI shutdown
		ServerSocket srvSocket = new ServerSocket(S_PORT);
		// final long start = System.currentTimeMillis();

		new Thread(
				new Runnable() {
					@Override
					public void run() {
						// Run forever as this handles request delegation
						while (true) { // (start + (30 * 1000)) >
							// System.currentTimeMillis()) { //30 seconds to
							// connect
							Socket sock = null;
							try {
								sock = srvSocket.accept();
								BufferedReader reader = new BufferedReader(
										new InputStreamReader(sock.getInputStream()));

								final String line = reader.readLine();
								final String[] lineArr = line.split(" ");
								System.out.println(line);

								final InetAddress InetAdr = sock.getInetAddress();
								final Player player = getPlayerBySockAdr(InetAdr);

								if (line.toLowerCase().startsWith("name")) {
									regPlayer(lineArr, InetAdr);
								} else if (line.toLowerCase().startsWith("move")) {
									regPlayerMove(lineArr, player);
								} else if (line.toLowerCase().startsWith("point")) {
									regPlayerPoints(lineArr);
								} else if (line.toLowerCase().startsWith("wait")) {
									if (Main.me.getTime() <= player.getTime()) {
										System.out.println("asjdlk");
										while (!Main.me.getPLAYER_STATE().equals(
												Player.STATE.IDLE)) {

										}

										Main.writeMsg("ok",
												InetAdr.toString().replace("/", ""));
									}
								} else if (line.toLowerCase().startsWith("ok")) {
									synchronized (Main.me) {
										Main.me.setOkCounter(Main.me.getOkCounter() + 1);
									}
								}
								int otherTime = Integer
										.parseInt(lineArr[lineArr.length - 1]);
								if (otherTime > me.getTime()) {
									me.setTime(otherTime);
								}
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								try {
									sock.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}

					}
				}).start();

		// IP scan server - accepts client queries
		new Thread(() -> {
			try {
				this.scanSrvSock = new ServerSocket(SCAN_PORT);
			} catch (Exception e) {
				e.printStackTrace();
			}

			final long start = System.currentTimeMillis();

			while (start + (180 * 1000) > System.currentTimeMillis()) { // 180
				// seconds
				// to
				// connect
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
			if (player.getXpos() - x < 0) {
				playerMoved(player, 1, 0, "right"); // right
			} else if (player.getXpos() - x > 0) {
				playerMoved(player, -1, 0, "left"); // left
			} else if (player.getYpos() - y < 0) {
				playerMoved(player, 0, 1, "down"); // down
			} else if (player.getYpos() - y > 0) {
				playerMoved(player, 0, -1, "up"); // up
			}
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
		for (Player p : this.players) {
			if (p.getIp() != null && p.getIp().toString().equals(ip.toString()))
				return p;
		}

		return null;
	}

	private synchronized Player getPlayerByName(final String name) {
		for (Player p : this.players) {
			if (p.getName().equalsIgnoreCase(name))
				return p;
		}

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

			this.fields = new Label[20][20];
			for (int j = 0; j < 20; j++) {
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
				case L:
					if (ipArr == null) {
						ips.addAll(new IPScan(IP_RANGE, SCAN_PORT).scan());
					} else {
						for (String ip : ipArr) {
							ips.add(ip);
						}
					}
					initConnect();
					break;
				default:
					break;
				}
			});

			// Setting up standard players
			Main.me = new Player(NAME, X_POS, Y_POS, "up");
			// randomizePos(me);
			synchronized (this) {
				this.players.add(Main.me);
			}
			this.fields[Main.me.getXpos()][Main.me.getYpos()]
					.setGraphic(new ImageView(hero_up));

			this.scoreList.setText(getScoreList());

			initListen();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void stop() throws IOException {
		if (this.srvSock != null) {
			this.srvSock.close();
		}
		if (this.scanSrvSock != null && !this.scanSrvSock.isClosed()) {
			this.scanSrvSock.close();
		}
	}

	private void randomizePos(Player player) {
		// 0-19
		final Random rnd = new Random();

		do {
			player.setXpos(rnd.nextInt(20));
			player.setYpos(rnd.nextInt(20));
		} while (this.board[player.getXpos()].charAt(player.getYpos()) != 'w');
	}

	public void playerMoved(int delta_x, int delta_y, String direction) {
		Main.me.setPLAYER_STATE(Player.STATE.VENTER_OK);
		Main.me.incTime();

		for (String ip : ips) {
			Main.writeMsg("WAIT", ip);
		}
		while (Main.me.getOkCounter() < this.players.size() - 1) {

		}
		playerMoved(Main.me, delta_x, delta_y, direction);
	}

	public void playerMoved(Player player, int delta_x, int delta_y,
			String direction) {
		player.setDirection(direction);
		int x = player.getXpos(), y = player.getYpos();

		if (this.board[y + delta_y].charAt(x + delta_x) == 'w') {
			if (player.equals(Main.me)) {
				player.addPoints(-1);
			}
		} else {
			Player p = getPlayerAt(x + delta_x, y + delta_y);
			if (p != null) {
				if (player.equals(Main.me)) {
					player.addPoints(10);
					p.addPoints(-10);
				}
			} else {
				if (player.equals(Main.me)) {
					player.addPoints(1);
				}

				this.fields[x][y].setGraphic(new ImageView(image_floor));
				x += delta_x;
				y += delta_y;

				if (direction.equals("right")) {
					this.fields[x][y].setGraphic(new ImageView(hero_right));
				}
				;
				if (direction.equals("left")) {
					this.fields[x][y].setGraphic(new ImageView(hero_left));
				}
				;
				if (direction.equals("up")) {
					this.fields[x][y].setGraphic(new ImageView(hero_up));
				}
				;
				if (direction.equals("down")) {
					this.fields[x][y].setGraphic(new ImageView(hero_down));
				}
				;

				player.setXpos(x);
				player.setYpos(y);

				if (player.equals(Main.me)) {
					broadcastMove();
				}
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
		for (final String ip : ips) {
			Main.writeMsg("POINT " + player.getName() + " " + points, ip);
		}
	}

	/**
	 * Broadcast to all ips that you just moved to a new position
	 */
	public void broadcastMove() {
		for (final String ip : ips) {
			Main.writeMsg("move " + Main.me.getXpos() + " " + Main.me.getYpos(), ip);
		}
	}

	public synchronized String getScoreList() {
		StringBuffer b = new StringBuffer(100);
		for (Player p : this.players) {
			b.append(p + "\r\n");
		}
		return b.toString();
	}

	public synchronized Player getPlayerAt(int x, int y) {
		for (Player p : this.players) {
			if (p.getXpos() == x && p.getYpos() == y)
				return p;
		}
		return null;
	}

	public static void main(String[] args) {
		Application.launch(args);
	}
}
