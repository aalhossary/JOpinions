/**
 * 
 */
package sg.edu.ntu.jopinions;

/**
 * @author Amr
 *
 */
public interface Constants {

	String MODEL_INDEPENDENT_CASTOR_AND_POLLUX				= "IndependentCastorAndPollux";
	String MODEL_INDEPENDENT_NETWORKED_CASTOR_AND_POLLUX	= "IndependentNetworkedCastorAndPollux";
	String MODEL_COUPLED_NETWORK_CASTOR_AND_POLLUX_PHI		= "CoupledNetworkCastorAndPollux-Phi";
	String MODEL_COUPLED_NETWORK_CASTOR_AND_POLLUX_BETA		= "CoupledNetworkCastorAndPollux-Beta";
	String MODEL_FULLY_COUPLED_NETWORKED_CASTOR_AND_POLLUX	= "FullyCoupledNetworkedCastorAndPollux";
	
	String TOPOLOGY_WATTS_STROGATZ_GRAPH 		= "WattsStrogatzGraph";
	String TOPOLOGY_BARABASI_ALBERT_GRAPH 		= "BarabasiAlbertGraph";
	String TOPOLOGY_ERDOS_RENYI_GNP_RANDOM_GRAPH= "GnpRandomGraph";
	String TOPOLOGY_KLEINBERG_SMALL_WORLD_GRAPH	= "KleinbergSmallWorldGraph";
	
	float EPSILON= 1E-1f; //Float.MIN_VALUE;

	String PARAM_MANAGE_STUBBORN = "-manageStubborn";
	public String MOBILIZE = "mobilize";
	public String POLARIZE_SINGLE = "polarizeSingle";
	public String POLARIZE_COUPLE = "polarizeCouple";
	public String NONE = "none";

}
