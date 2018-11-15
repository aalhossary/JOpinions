/**
 * 
 */
package sg.edu.ntu.jopinions;

/**
 * @author Amr
 *
 */
public interface Constants {

	String MODEL_INDEPENDENT_CASTOR_AND_POLLUX				= "ICAP";
	String MODEL_INDEPENDENT_NETWORKED_CASTOR_AND_POLLUX	= "INCAP";
	String MODEL_COUPLED_NETWORK_CASTOR_AND_POLLUX_PHI		= "CONCAP-Phi";
	String MODEL_COUPLED_NETWORK_CASTOR_AND_POLLUX_BETA		= "CONCAP-Beta";
	String MODEL_FULLY_COUPLED_NETWORKED_CASTOR_AND_POLLUX	= "FCONCAP";
	
	String TOPOLOGY_WATTS_STROGATZ_GRAPH 		= "WSG";//"WattsStrogatzGraph";
	String TOPOLOGY_BARABASI_ALBERT_GRAPH 		= "BAG";//"BarabasiAlbertGraph";
	String TOPOLOGY_ERDOS_RENYI_GNP_RANDOM_GRAPH= "ERG";//"GnpRandomGraph";
	String TOPOLOGY_KLEINBERG_SMALL_WORLD_GRAPH	= "KSWG";//"KleinbergSmallWorldGraph";
	
	float EPSILON= 1E-1f; //Float.MIN_VALUE;

	String PARAM_STUBBORN = "-stubborn";
	String PARAM_MANAGE_STUBBORN = "-manageStubborn";
	public String POLARIZE_SINGLE = "polarizeSingle";
	public String POLARIZE_COUPLE = "polarizeCouple";
	public String NONE = "none";
	String PULLOX = "P";
	String CASTOR = "C";

}
