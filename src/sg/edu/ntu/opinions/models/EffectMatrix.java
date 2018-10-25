/**
 * 
 */
package sg.edu.ntu.opinions.models;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

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

	/**Number of P-C couples in the system. */
	protected static int n;
	
//	public static final float EPSILON = 1.0E-20f;//Float.MIN_VALUE;
	
	protected float[][] quadrantCC = null;
	protected float[][] quadrantCP = null;
	protected float[][] quadrantPC = null;
	protected float[][] quadrantPP = null;
	
	protected float[][][] data;

	/**
	 * @param n number of entities, usually couples. May be singles if all Pulluxes are not connected to all Castors.
	 */
	public EffectMatrix(int n) {
		EffectMatrix.n = n;
		//initialize matrix quadrants
		initQuadrants(n);
		data = new float[][][] {quadrantCC,quadrantCP,
								quadrantPC,quadrantPP};
	}
	protected abstract void initQuadrants(int n);
	/**This function does not calculate nor update anything. It is just a getter.
	 * @param y the affecter point
	 * @param x the affected point
	 * @return the effect that point y exerts on point x.
	 */
	public abstract float getEffect(int y, int x);
	
	/**Updates the values of all individual vlaues of Effects, based on distances
	 * between points and connects as defined in the graph.
	 * @param x The {@link OpinionsMatrix}. it represents collective opinions at current time
	 * @param graphs the relations between vertices
	 */
	public abstract void updateUsing(OpinionsMatrix x, Graph<PointND, DefaultEdge>[] graphs);


	public abstract void normalize();
	
	protected float getQuadrantSum(float[][] quadrant) {
		int n = EffectMatrix.n;
		float sum = 0;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				sum += quadrant[i][j];
			}
		}
		return sum;
	}
	protected void scaleQuadrant(float[][] quadrant, float scale) {
		int n = EffectMatrix.n;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				quadrant[i][j] *= scale;
			}
		}
	}
	
	protected float getEffectWithinQuadrant(float[][] quad, int y, int x) {
		return quad[y][x];
	}
	/**The order may be changed later
	 * <pre>
	 * (  0  |  1
	 * ------+------
	 *    2  |  3
	 * </pre>
	 */
	public static int getQuadrant(int row, int col) {
		return ((row/n)*2) + (col / n);
	}
	
	public abstract float[][] multiply(OpinionsMatrix x);
	
}
