import java.io.Serializable;

/**
 * Routing information class, maintains the destination port number,
 * next port number and the associated cost
 * 
 * @author Harsh Patil
 *
 */
public class RoutingInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	String destPort, nextPort;
	int hopCount;

	public RoutingInfo(String destPort, String nextPort, int hopCount) {
		this.destPort = destPort;
		this.nextPort = nextPort;
		this.hopCount = hopCount;
	}

	public String toString() {
		return destPort;
	}
}