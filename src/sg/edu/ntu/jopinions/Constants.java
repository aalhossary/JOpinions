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
	String TOPOLOGY_BOLLOBAS_DIRECTED_SCALEFREE_GRAPH	= "DSFG";//"BollobasGraphGenerator";
	float EPSILON= 1E-1f; //Float.MIN_VALUE;

//	String PARAM_STUBBORN = "-stubborn";
	String PARAM_EGO_RATIO = "-egoRatio";
	String PARAM_MANAGE_STUBBORN = "-manageStubborn";
	String PARAM_IN_FOLDER = "-inFolder";
	String PARAM_OUT_FOLDER = "-outFolder";
	public String POLARIZE_SINGLE = "polarizeSingle";
	public String POLARIZE_COUPLE = "polarizeCouple";
	public String NONE = "none";
	String PULLOX = "P";
	String CASTOR = "C";
	String PATTERN_LOG_FILE_GRAPH_CC = "gcc-%s.log";
	String PATTERN_LOG_FILE_GRAPH_PP = "gpp-%s.log";
	String PATTERN_LOG_FILE_DETAILS_CP = "dcp-%s.log";
	String PATTERN_LOG_FILE_D = "D-%s.log";
	String PATTERN_LOG_FILE_X = "x-%s.log";

}
