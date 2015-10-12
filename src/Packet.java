import java.io.Serializable;
import java.util.Hashtable;

/**
 * Custom structure to hold packet information in 
 * 
 * @author Harsh Patil
 *
 */
class Packet implements Serializable {

	private static final long serialVersionUID = 1L;
	String source;
	String sourceName;
	Hashtable<String, RoutingInfo> routingTable;

	/**
	 * Constructor to hold the source and routing table
	 * 
	 * @param neighbors
	 * @param source
	 */
	public Packet(Hashtable<String, RoutingInfo> neighbors, String source, String sourceName) {
		this.routingTable = neighbors;
		this.source = source;
		this.sourceName = sourceName;
	}
}