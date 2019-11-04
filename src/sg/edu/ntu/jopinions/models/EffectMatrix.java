/**
 * 
 */
package sg.edu.ntu.jopinions.models;

import java.io.PrintStream;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.Defaults;

/**This matrix is described in the text as D.<br>
 * It contains 4 submatrices.
 * <pre>
 * (D^cc,| D^cp,
 * ------+------
 *  D^pc,| D^pp)
 * </pre>
 * 
 * 
 * @author Amr
 *
 */
public abstract class EffectMatrix {

//	public static final String SPACE_AND_OUTPUT_FORMAT = " " + Defaults.OUTPUT_FORMAT;

	/**Number of P-C couples in the system. */
	protected static int n;
	
	//1st dimension is column number. 2nd dimension is the column itself (it runs along the row number).
	protected float[][] quadrantCC = null;
	protected float[][] quadrantPC = null;
	protected float[][] quadrantCP = null;
	protected float[][] quadrantPP = null;
	
	/**first dimension is quadrant (columns wise). 
	 * second dimension is column number. third dimension is row number.
	 * This implementation is chosen to facilitate matrix multiplication as well as normalization.*/
	protected float[][][] data;

	private static String ZEROS;

	protected float ego = Defaults.DEFAULT_EGO;

	/**
	 * @param n number of entities, usually couples. May be singles if all Pulluxes are not connected to all Castors.
	 */
	public EffectMatrix(int n) {
		EffectMatrix.n = n;
		//initialize matrix quadrants
		initQuadrants(n);
		data = new float[][][] {quadrantCC, quadrantPC, quadrantCP, quadrantPP};

		StringBuilder temp = new StringBuilder();
//		String format = SPACE_AND_OUTPUT_FORMAT;
		for (int i = 0; i < n; i++) {
			temp.append(String.format(Defaults.OUTPUT_FORMAT, 0.0));
		}
		ZEROS = temp.toString();
	}
	protected abstract void initQuadrants(int n);
	/**This function does not calculate nor update anything. It is just a getter.
	 * TODO validate
	 * @param y the affecter point
	 * @param x the affected point
	 * @return the effect that point y exerts on point x.
	 */
	public abstract float getEffect(int y, int x);
	
	/**Updates the values of all individual values of Effects, based on distances
	 * between points and connects as defined in the graph.
	 * @param x The {@link OpinionsMatrix}. it represents collective opinions at current time
	 * @param graphs the relations between vertices
	 */
	public abstract void updateUsing(OpinionsMatrix x, Graph<PointND, DefaultEdge>[] graphs);


	/**Normalizes the matrix <i><b>column</b> wise</i> (including lines from one or two quadrants)*/
	public abstract void normalize();
	
	protected float getEffectWithinQuadrant(float[][] quad, int y, int x) {
		return quad[x][y];
	}
	/**The order may be changed later
	 * <pre>
	 * (  0  |  2
	 * ------+------
	 *    1  |  3  )
	 * </pre>
	 */
	public static int getQuadrant(int row, int col) {
		return ((col/n)*2) + (row / n);
	}
	
	/**This function returns the corresponding column from the matrix.
	 * <p>
	 * It may return one or two arrays in the first dimension. Two arrays 
	 * are returned in case of coupled pair. But in case of Independent couple, 
	 * the output is one array and one null pointer.
	 * @param col the column number to return.
	 * @return A two dimensional array of <code>float</code>. The first is
	 * the quadrant and the second is the row number
	 */
	abstract float[][] getLine(int col);
	
	public void print(PrintStream out) {
		printQuadrants(out, new float[][][] {quadrantCC,quadrantCP});
		printQuadrants(out, new float[][][] {quadrantPC,quadrantPP});
	}
	private void printQuadrants(PrintStream out, float[][][] targetQuadrants) {
		int n = EffectMatrix.n;
		float[][] targetQuadrant0 = targetQuadrants[0];
		float[][] targetQuadrant1 = targetQuadrants[1];
		String ZEROS = EffectMatrix.ZEROS;
		for (int i = 0; i < n; i++) {
			if (targetQuadrant0 != null) {
				for (int j = 0; j < n; j++) {
					out.format(Defaults.OUTPUT_FORMAT, targetQuadrant0[j][i]);
				}
			} else {
				out.print(ZEROS);
			}
			if (targetQuadrant1 != null) {
				for (int j = 0; j < n; j++) {
					out.format(Defaults.OUTPUT_FORMAT, targetQuadrant1[j][i]);
				}
			} else {
				out.print(ZEROS);
			}
			out.println();
		}
	}
	
	public void setEgo(float ego) {
		this.ego = ego;
	}
}
