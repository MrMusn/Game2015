package game2015;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * IP Scanner used for discovering IPs.<br>
 * Should only be used on local networks with permission, as this will scan the
 * entire IPv4 range if allowed, and is probably viewed as port scanning by an
 * ISP.
 * 
 * @author Anders Grøn
 *
 */
public class IPScan {
	public static void main(String[] args) {
		IPScan ipScan = new IPScan("192.168.0.0/16", 55559);
		ipScan.scan();
	}
	
	private final int scanPort;
	private static final int CONN_TIMEOUT = 100;

	private final String ipRange;// = "192.168.0.0/16";
	private final List<String> myIPs;
	
	/**
	 * Construct an IPScan object.<br>
	 * <br>
	 * An IP range must follow this format:
	 * <b>"byte.byte.byte.byte/variableBits"</b><br>
	 * Example: 192.168.0.0/16, scans 192.168.0.0 to 192.168.255.255 on port
	 * {@link IPScan#scanPort}
	 * 
	 * @param ipRange
	 *            A String representation of the IP range, using the format
	 *            described above
	 * @param scanPort
	 *            The port that target servers are listening on.
	 */
	public IPScan(final String ipRange, final int scanPort) {
		this.ipRange = ipRange;
		this.scanPort = scanPort;
		this.myIPs = getLocalAddress();
		System.out.println("My IP: " + this.myIPs);
	}
	
	/**
	 * Scans {@link IPScan#ipRange} of IPs for clients listening on
	 * {@link IPScan#scanPort} with a timeout of {@link IPScan#CONN_TIMEOUT}<br>
	 * <br>
	 * Any IO exceptions that doesn't have to do with timeout and connecting are
	 * ignored within {@link IPScan#queryServer(String)}.<br>
	 * <br>
	 * Note, this method potentially spawns hundreds of millions of threads.
	 * Shouldn't cause any resource trouble as each thread will idly wait on
	 * blocking call(s) for the majority of runtime. Amount of threads spawned
	 * are more precisely 256^(variableBits / 8)<br>
	 * <br>
	 * Does not work with 32 variable bits. Because of two's complement, the first bit out of a 32 bit integer will be incorrect
	 * 
	 * @return An ArrayList containing all ips successfully queried.
	 */
	public List<String> scan() {	
		final List<String> ips = new ArrayList<String>();
		
		final String[] ipArr = ipRange.substring(0, ipRange.indexOf('/')).split("\\.");
		final int varBits = Integer.parseInt(ipRange.substring(ipRange.indexOf('/') + 1)); //(significantBits?)
		
		final int ipBytes[] = new int[4];
		for (int i = 0; i < (varBits / 8); ++i)
			ipBytes[i] = Integer.parseInt(ipArr[i], 10);
		
		final ThreadGroup queryThreads = new ThreadGroup("IP query thread group");
		
		int startVal = 0;
		//Flip first (32 - varBits) bits to 1s, ensures we're only touching significant bits in the main loop
		for (int i = varBits; i < 32; i += 8)
			startVal |= 0xFF << i;
		
		//Stop when all significant bits are 1s
		final int endVal = 0xFFFFFFFF;
		
		for (int i = startVal; i <= endVal; ++i) {
			for (int j = 0; j < varBits; j += 8) {
				ipBytes[4 - (varBits / 8) + (j / 8)] = (i >>> j & 0xFF);
			}
		
			final String ip = (ipBytes[0] + "." + ipBytes[1] + "." + ipBytes[2] + "." + ipBytes[3]);
			
			new Thread(queryThreads, () -> { //Lambda expression for Runnable implementation
				if (queryServer(ip)) {
					synchronized (ip) {
						if (!myIPs.contains(ip)) {
							ips.add(ip);
							System.out.println("IP " + ip + " queried succesfully on port " + scanPort);
						}
					}
				}
			}).start();
		}
		
		//First return when all threads finish
		while (queryThreads.activeCount() != 0)
			;
		
		System.out.println("IP scan complete! Found " + ips.size() + " ips.");
		return ips;
	}
	
	/**
	 * Helper method for this class' primary method. Handles all IO and buries any unexpected exceptions.
	 * @param ip
	 * @return
	 */
	private boolean queryServer(final String ip) {
		Socket s = new Socket();
		
		try {
//			System.out.println("Querying IP " + ip);
			s.connect(new InetSocketAddress(ip, scanPort), CONN_TIMEOUT);
			s.setKeepAlive(true);
			
			s.getOutputStream().write("Knock knock".getBytes("UTF-8"));
			
			//64 bytes are overkill as any response larger than "who is it?" is invalid
			final byte[] buffer = new byte[64];
			if (s.getInputStream().read(buffer) > 0 &&
					new String(buffer, "UTF-8").startsWith("Who is it?"))
				return true;
			else
				//EOF, 0 bytes read or bad response
				return false;
			
		} catch (SocketTimeoutException | ConnectException e) {
			//No listening server
			return false;
		}  catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Exception on IP " + ip);
			return false;
		} finally {
			try {
				s.close();
			} catch (IOException e) {e.printStackTrace();}
		}
	}
	
	//Possibly not entirely secure, but does the job for now
	private List<String> getLocalAddress() {
		List<String> ips = new ArrayList<String>();

		try {
			Enumeration<NetworkInterface> b = NetworkInterface
					.getNetworkInterfaces();
			while (b.hasMoreElements()) {
				for (InterfaceAddress f : b.nextElement().getInterfaceAddresses())
					if (f.getAddress().isSiteLocalAddress())
						ips.add(f.getAddress().getHostAddress());
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return ips;
    }
}