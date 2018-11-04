/**
 * 
 */
package sg.edu.ntu.jopinions.control;

import javax.swing.JFrame;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.control.gui.GraphPanel;
import sg.edu.ntu.jopinions.models.EffectMatrix;
import sg.edu.ntu.jopinions.models.OpinionsMatrix;
import sg.edu.ntu.jopinions.models.PointND;

/**I chose to implement {@link Runnable} instead of extending {@link Thread} to have better control on
 * the running simulation, as well as to avoid exposing harmful thread methods.
 * @author Amr
 *
 */
public class Simulation implements Runnable {
	
	public static final String MODEL_INDEPENDENT_CASTOR_AND_POLLUX				= "IndependentCastorAndPollux";
	public static final String MODEL_INDEPENDENT_NETWORKED_CASTOR_AND_POLLUX	= "IndependentNetworkedCastorAndPollux";
	public static final String MODEL_COUPLED_NETWORK_CASTOR_AND_POLLUX_PHI		= "CoupledNetworkCastorAndPollux-Phi";
	public static final String MODEL_COUPLED_NETWORK_CASTOR_AND_POLLUX_Beta		= "CoupledNetworkCastorAndPollux-Beta";
	public static final String MODEL_FULLY_COUPLED_NETWORKED_CASTOR_AND_POLLUX	= "FullyCoupledNetworkedCastorAndPollux";

	public static final String TOPOLOGY_WATTS_STROGATZ_GRAPH 		= "WattsStrogatzGraph";
	public static final String TOPOLOGY_BARABASI_ALBERT_GRAPH 		= "BarabasiAlbertGraph";
	public static final String TOPOLOGY_ERDOS_RENYI_GNP_RANDOM_GRAPH= "GnpRandomGraph";
	public static final String TOPOLOGY_KLEINBERG_SMALL_WORLD_GRAPH	= "KleinbergSmallWorldGraph";

	public static final int DEFAULT_NUM_DIMENSIONS = 3;

	public static final float EPSILON= 1E-1f; //Float.MIN_VALUE;

	private static final long MAX_STEPS = 10_000_000;
	private String modelNameString = MODEL_INDEPENDENT_NETWORKED_CASTOR_AND_POLLUX;
	private String topology = TOPOLOGY_WATTS_STROGATZ_GRAPH;
	private int dimensions = DEFAULT_NUM_DIMENSIONS;
	
	private static final long stepTimeMillis = 1;//1_000;
	
	Thread runner= null;
	long step = -1;

	private boolean verbose = false;
	
	/**They are in order
	 * <pre>
	 * {graphCC, graphCP,
	 *  graphPC, graphPP}
	 *  </pre>
	 */
	private Graph<PointND, DefaultEdge>[] graphs;

	private OpinionsMatrix x;

	private EffectMatrix D;

		
	@SuppressWarnings("unchecked")
	public Simulation() {
		this.graphs =  (Graph<PointND, DefaultEdge>[]) new Graph[4] ;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		//FIXME change this later
		OpinionsMatrix x = this.getX();
		x.normalize();
		//TODO fill initial x values
		EffectMatrix D = this.D;
		Graph<PointND, DefaultEdge>[] graphs = this.graphs;

		float oneOverNSquare;
		boolean converged = false;
		step = 0;
		oneOverNSquare= 1.0f / x.getD() / x.getD();
		
		D.updateUsing(x, graphs);
		D.normalize();

		//TODO show initial state
		if (verbose ) {
			x.print(System.out);
			System.out.println();
		}
		
		//TODO fix the temp JFrame
		JFrame frame = new JFrame("JOpinions Simulation ["+topology+", "+modelNameString+"]");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GraphPanel<PointND, DefaultEdge> panel = new GraphPanel<>();
		panel.setGraphs(graphs);
		frame.setContentPane(panel);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		
		while (++step <= MAX_STEPS) {
			
			//now I have transformation (effects) matrix and x (opinions) matrix
			

			//update opinions
			//opinionsMatrix = effectMatrix x opinionsMatrix
			float[][] tempX = x.multiply(D);
			
			//calculate the total system update (total absolute distance)
			//TODO is it the update of X only, or X and D?
			float totalAbsDist = x.calculateTotalDifference(tempX);
			converged = totalAbsDist < (oneOverNSquare / step);
			System.out.format("Step = %d, Total Diff = %8.5Ed, Converged = %b\n", step, totalAbsDist, converged);

			//x = tempX;
			x.match(tempX);
			x.normalize();

			//TODO save tempX if you want
			if (verbose ) {
				x.print(System.out);
				System.out.println();
			}

			
			D.updateUsing(x, graphs);
			D.normalize();
			

			//Show updates on GUI
			panel.repaint();

			//delay
			try {
				//TODO adjust ti later
				Thread.sleep(stepTimeMillis);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//TODO output opinionsMatrix and EffectMatrix
			if (converged) {
				break;
			}
//			step++;
		}

		if (converged) {
			System.out.println("stopped because system converged after "+step+" step(s)");
		} else {
			System.out.println("stopped because system reached maximum mumber of steps ("+step+").");
		}
		outputFinalStats();

	}

	private void outputFinalStats() {
		// TODO Auto-generated method stub
	}

	public void start() {
		if(runner == null) {
			runner=new Thread(this);
			runner.start();
		}else {
			throw new IllegalStateException("Thread did not shutdown peacefully.");
		}
	}

	/**
	 * @param graphs ordered as 
	 * <pre>
	 * {graphCC, graphCP,
	 *  graphPC, graphPP}
	 *  </pre>
	 */
	public void setGraphs(Graph<PointND, DefaultEdge>[] graphs) {
		this.graphs = graphs;
	}
//	public long updateMAX_STEPS() {
//		this.MAX_STEPS=
//	}


	public OpinionsMatrix getX() {
		return x;
	}


	public boolean isVerbose() {
		return verbose;
	}


	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}


	public EffectMatrix getD() {
		return D;
	}


	public void setD(EffectMatrix d) {
		D = d;
	}


	public void setX(OpinionsMatrix x) {
		this.x = x;
	}


	public String getTopology() {
		return topology;
	}


	public void setTopology(String topology) {
		this.topology = topology;
	}


	public String getModelNameString() {
		return modelNameString;
	}


	public void setModelNameString(String model) {
		this.modelNameString = model;
	}

	
}
