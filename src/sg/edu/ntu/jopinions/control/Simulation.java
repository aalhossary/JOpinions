/**
 * 
 */
package sg.edu.ntu.jopinions.control;

import java.io.PrintStream;

import javax.swing.JFrame;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.Defaults;
import sg.edu.ntu.jopinions.models.EffectMatrix;
import sg.edu.ntu.jopinions.models.NaNException;
import sg.edu.ntu.jopinions.models.OpinionsMatrix;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.views.GraphPanel;

/**I chose to implement {@link Runnable} instead of extending {@link Thread} to have better control on
 * the running simulation, as well as to avoid exposing harmful thread methods.
 * @author Amr
 *
 */
public class Simulation implements Runnable {
	
	private String modelNameString;
	private String topology;
	private int dimensions = Defaults.DEFAULT_NUM_DIMENSIONS;
	
	
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
		OpinionsMatrix x = this.getX();
		x.normalize();
		EffectMatrix D = this.D;
		Graph<PointND, DefaultEdge>[] graphs = this.graphs;

		float oneOverNSquare;
		boolean converged = false;
		boolean nan = false;
		step = 0;
		oneOverNSquare= 1.0f / x.getD() / x.getD();
		
		D.updateUsing(x, graphs);
		D.normalize();

		if (verbose ) {
			printXAndD(x, D, System.out, System.out);
		}
		
		//TODO fix the temp JFrame
		JFrame frame = new JFrame("JOpinions Simulation ["+topology+", "+modelNameString+"]");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GraphPanel<PointND, DefaultEdge> panel = new GraphPanel<>();
		panel.setGraphs(graphs);
		frame.setContentPane(panel);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		
		try {
			while (++step <= Defaults.DEFAULT_MAX_STEPS) {
				//now I have transformation (effects) matrix and x (opinions) matrix

				//update opinions
				//opinionsMatrix = opinionsMatrix x effectMatrix
				float[][] tempX = x.multiply(D);

				//calculate the total system update (total absolute distance)
				//TODO is it the update of X only, or X and D?
				float totalAbsDist = x.calculateTotalDifference(tempX);
				converged = totalAbsDist < (oneOverNSquare / step);

				//x = tempX;
				x.match(tempX);
				x.normalize();

				//TODO save tempX if you want

				D.updateUsing(x, graphs);
				D.normalize();
				
				//==============simulation step proper ends here ================
				System.out.format("Step = %d, Total Diff = %8.5E, Converged = %b\n", step, totalAbsDist, converged);

				//Show updates on GUI
				panel.repaint();

				//output opinionsMatrix and EffectMatrix
				if (verbose ) {
					printXAndD(x, D, System.out, System.out);
				}

				//delay
				try {
					//TODO adjust it later
					Thread.sleep(Defaults.DEFAULT_STEP_DELAY_MILLIS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (converged) {
					break;
				}
			}//End of loop

		}catch (NaNException e) {
			nan = true;
			System.err.println(e.getMessage());
		}
		
		panel.finishedSimulation();

		if (converged) {
			System.out.format("stopped because system converged after %d steps", step);
		} else if (nan){
			System.out.format("stopped because an error occured in step %d.", step);
		} else {
			step--;//because it exceeded the target by 1 already
			System.out.format("stopped because system reached maximum mumber of steps (%d).", step);
		}
		outputFinalStats();
	}


	private void printXAndD(OpinionsMatrix opinionsMatrix, EffectMatrix effectMatrix, PrintStream out1, PrintStream out2) {
		opinionsMatrix.print(out1);
		out1.println();
		effectMatrix.print(out2);
		out2.println();
		out1.println();
		if (out1 != out2) {
			out2.println();
		}
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
