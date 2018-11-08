/**
 * 
 */
package sg.edu.ntu.jopinions;

import java.awt.Color;

/**
 * @author Amr
 *
 */
public interface Defaults extends Constants {

	int DEFAULT_NUM_DIMENSIONS = 3;
	long DEFAULT_MAX_STEPS = 5_000;
	float DEFAULT_STEP_DELAY_SECS = 0;//1_000;
	
	float DEFAULT_PHI = 0.75f;
	float DEFAULT_BETA = 0.75f;
	
	String DEFAULT_TOPOLOGY = TOPOLOGY_WATTS_STROGATZ_GRAPH;
	String DEFAULT_MODEL = MODEL_INDEPENDENT_NETWORKED_CASTOR_AND_POLLUX;
	
	/**it already includes one extra space to separate entries*/
	public String OUTPUT_FORMAT = "%10.7f";
	
	Color COLOR_CASTOR = Color.RED;
	Color COLOR_PULLOX = Color.BLUE;
	Color COLOR_CONNECTION = Color.BLACK;

	/** percentage stubborn nodes that remain fixed after mobilization (out of all population)*/
	float RHO = 0.05f;
	
	/** value[0.0, 1.0] to determine the polar area size. If \nu = 1, they would touch each other. */
	float NU = 0.25f;

}
