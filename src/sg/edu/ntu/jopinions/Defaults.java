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
	long DEFAULT_MAX_STEPS = 10_000_000;
	long DEFAULT_STEP_DELAY_MILLIS = 1;//1_000;
	
	float DEFAULT_PHI = 0.75f;
	float DEFAULT_BETA = 0.75f;
	
	String DEFAULT_TOPOLOGY = TOPOLOGY_WATTS_STROGATZ_GRAPH;
	String DEFAULT_MODEL = MODEL_INDEPENDENT_NETWORKED_CASTOR_AND_POLLUX;
	
	/**it already includes one extra space to separate entries*/
	public String OUTPUT_FORMAT = "%10.7f";
	
	Color COLOR_CASTOR = Color.RED;
	Color COLOR_PULLOX = Color.BLUE;
	Color COLOR_CONNECTION = Color.BLACK;
	
}
