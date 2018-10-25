/**
 * 
 */
package sg.edu.ntu.jopinions.control;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.models.EffectMatrix;
import sg.edu.ntu.jopinions.models.OpinionsMatrix;
import sg.edu.ntu.jopinions.models.PointND;

/**I chose to implement {@link Runnable} instead of extending {@link Thread} to have better control on
 * the running simulation, as well as to avoid exposing harmful thread methods.
 * @author Amr
 *
 */
public class Simulation implements Runnable {
	
	public static final float EPSILON= 1e-15f; //Float.MIN_VALUE;

	private static final long MAX_STEPS = 10000000;
	
	Thread runner= null;
	long step = -1;

	private boolean verbose = true;
	
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
		//TODO fill initial x values
		EffectMatrix D = this.D;
		Graph<PointND, DefaultEdge>[] graphs = this.graphs;

		float oneOverNSquare;
		initVariables();
		boolean converged = false;
		step = 0;
		oneOverNSquare= 1.0f / x.getD() / x.getD();
		
		D.updateUsing(x, graphs);
		D.normalize();

		//TODO show initial state
		if (verbose ) {
			x.printTransposed(System.out);
			System.out.println();
		}
		
		while (++step <= MAX_STEPS) {
			
			//now I have transformation (effects) matrix and x (opinions) matrix
			

			//update opinions
			//opinionsMatrix = effectMatrix x opinionsMatrix
			float[][] tempX = D.multiply(x);
			//TODO normalize opinionsMatrix ???
			//tempX.normalize();
			
			//TODO calculate the total system update (total absolute distance)
			// is it the update of X only?
			float totalAbsDist = x.calculateTotalDifference(tempX);
			converged = totalAbsDist < (oneOverNSquare / step);
			System.out.format("Total Diff = %8.5Ed, Converged = %b\n", totalAbsDist, converged);

			//x = tempX;
			x.match(tempX);

			//TODO save tempX if you want
			if (verbose ) {
				x.printTransposed(System.out);
				System.out.println();
			}

			
			D.updateUsing(x, graphs);
			D.normalize();

			//TODO Show updates on GUI
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
	
	/**@deprecated no use.*/
	private void initVariables() {
		//May need to be populated
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

	
}
