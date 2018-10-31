package sg.edu.ntu.jopinions.models;

import java.io.PrintStream;
import java.util.Random;

import sg.edu.ntu.jopinions.models.PointND.PointNDSupplier;

/**This matrix is referenced in the text as x or x[t].<br>
 * The data is stored in one colum in the form of (c<sub>1</sub>c<sub>2</sub> · · · c<sub>n</sub>p<sub>1</sub>p<sub>2</sub> · · · p<sub>n</sub>).
 * Every row is a point of opinion in d dimensions.
 * @author Amr
 *
 */
public class OpinionsMatrix {
	
	private static final String outputFotmat = "%10.5f";

	/**Number of dimensions for each opinion (e.g. duration, expense). Must be a positive int.*/
	int d;
	
	/**Number of P-C couples in the system. <b>This is <i>half</i> of the number of points</b>*/
	int n;
	
	public PointND[] points;
	float[][] data;
	
	/**Generates opinions matrix assuming that the opinions are unidimensional
	 * @param n
	 */
	public OpinionsMatrix(int n) {
		this(n,1, true);
	}
	/**Initialize the matrix with number of vertices x number of dimensions.<br> 
	 * Note that the actual number of rows will be 2n not only n.
	 * @param n number of couples (half number of vertices)
	 * @param d number of dimensions.
	 * @param initialize TODO
	 */
	public OpinionsMatrix(int n, int d, boolean initialize) {
		this.n = n;
		this.d = d;
		
		if (initialize) {
			//now create the matrix itself
			PointND.d = d;
			PointND[] points = new PointND[2 * n];
			//share the same data variables between the 2D array and individual points (vertices)
			for (int i = 0; i < points.length; i++) {
				float[] data = new float[d];
				String name = i < n ? PointNDSupplier.CASTOR : PointNDSupplier.PULLOX;
				points[i] = new PointND(name, data);
			}
			set(points);
		}
	}
	
	/**@deprecated use {@link #set(PointND[])}
	 * @param data
	 */
	public void set(float[][] data) {
		this.data = data;
		for (int i = 0; i < points.length; i++) {
			points[i].setX(data[i]);
		}
	}
	
	public void match(float[][] data) {
		for (int i = 0; i < points.length; i++) {
			PointND pointND = points[i];
			pointND.matchValues(data[i]);
		}
	}
	
	public int getD() {
		return d;
	}
	public int getN() {
		return n;
	}
	
	public void print(PrintStream out) {
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				out.format(OpinionsMatrix.outputFotmat, data[i][j]);
			}
			out.println();
		}
	}
	public void printTransposed(PrintStream out) {
		int d = this.d;
		for (int i = 0; i < d; i++) {
			for (int j = 0; j < points.length; j++) {
				out.format(OpinionsMatrix.outputFotmat, data[j][i]);
			}
			out.println();
		}
	}
	
	
	public static void main(String[] args) {
		//just a dummy code to test matrix maltiplication
		
		OpinionsMatrix mat1 = new OpinionsMatrix(7, 3, true);
		for (int i = 0; i < mat1.points.length; i++) {
			PointND p = mat1.points[i];
			for (int j = 0; j < p.x.length; j++) {
				p.x[j] = i*10 + j;
			}
		}
		mat1.print(System.out);
		System.out.println();
		mat1.printTransposed(System.out);
		System.out.println();
		
		
		OpinionsMatrix mat2 = new OpinionsMatrix(7, 3, true);
		for (int i = 0; i < mat2.points.length; i++) {
			PointND p = mat2.points[i];
			for (int j = 0; j < p.x.length; j++) {
				p.x[j] = i*110 + j* 1.11111f;
			}
		}
		
		mat2.printTransposed(System.out);
		
		System.out.println();
		System.out.println();
		
//		mat2.printTransposed(System.out);
		
		for (int i = 0; i < mat2.points.length; i++) {
			PointND p2 = mat2.points[i];
			double dist = p2.getDist(mat1.points[i]);
			System.out.format(OpinionsMatrix.outputFotmat, dist);
		}
		System.out.println();
		
	}
	public float calculateTotalDifference(float[][] tempX) {
		float totalDiff=0;
		for (int i = 0; i < points.length; i++) {
			totalDiff += PointND.getDistRow(points[i].x, tempX[i]);
			
//			float[] point_i_x = points[i].x;
//			for (int j = 0; j < point_i_x.length; j++) {
//				totalDiff += Math.abs(point_i_x[j] - tempX[i][j]);
//			}

		}
		return totalDiff;
	}
	
	public void normalize() {
		for (PointND pointND : points) {
			pointND.normalize();
		}
	}
	public void set(PointND[] points) {
		//TODO validate d and n
		this.points = points;
		this.data= new float[points.length][];
		for (int i = 0; i < data.length; i++) {
			data[i] = points[i].x;
		}
	}
	public void randomize(long seed) {
		Random random = new Random(seed);
		for (PointND pointND : points) {
			float[] x = pointND.x;
			for (int i = 0; i < x.length; i++) {
				x[i] = random.nextFloat();//already from 0.0 to 1.0
			}
		}
	}
}
