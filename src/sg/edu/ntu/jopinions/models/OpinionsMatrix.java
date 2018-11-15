package sg.edu.ntu.jopinions.models;

import java.io.PrintStream;
import java.util.Random;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.Defaults;

/**This matrix is referenced in the text as x or x[t].<br>
 * The data is stored in one row in the form of (c<sub>1</sub>c<sub>2</sub> � � � c<sub>n</sub>p<sub>1</sub>p<sub>2</sub> � � � p<sub>n</sub>).
 * Every column is a point of opinion in d dimensions.
 * @author Amr
 */
public class OpinionsMatrix {
	
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
		this(1,n, true);
	}
	/**Initialize the matrix with number of dimensions x number of vertices.<br>
	 * Note that the actual number of rows will be 2n not only n.
	 * @param d number of dimensions.
	 * @param n number of couples (half number of vertices)
	 * @param initialize fill the matrix with any dummy data
	 */
	public OpinionsMatrix(int d, int n, boolean initialize) {
		this.n = n;
		this.d = d;
		
		if (initialize) {
			//now create the matrix itself
			PointND.d = d;
			PointND[] points = new PointND[2 * n];
			//share the same data variables between the 2D array and individual points (vertices)
			for (int i = 0; i < n; i++) {
				points[i] 		= new PointND(Constants.CASTOR, new float[d], i);
				points[i + n]	= new PointND(Constants.PULLOX, new float[d], i);
			}
			set(points);
		}
	}
	
	public void match(float[][] data) {
		for (int i = 0; i < points.length; i++) {
			PointND pointND = points[i];
			pointND.match(data[i]);
		}
	}
	
	public int getD() {
		return d;
	}
	public int getN() {
		return n;
	}
	
	public void printTransposed(PrintStream out) {
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				out.format(Defaults.OUTPUT_FORMAT, data[i][j]);
			}
			out.println();
		}
	}
	public void print(PrintStream out) {
		int d = this.d;
		for (int i = 0; i < d; i++) {
			for (int j = 0; j < points.length; j++) {
				out.format(Defaults.OUTPUT_FORMAT, data[j][i]);
			}
			out.println();
		}
	}
	
	
//	public static void main(String[] args) {
//		//just a dummy code to test matrix maltiplication
//		
//		OpinionsMatrix mat1 = new OpinionsMatrix(3, 7, true);
//		for (int i = 0; i < mat1.points.length; i++) {
//			PointND p = mat1.points[i];
//			for (int j = 0; j < p.x.length; j++) {
//				p.x[j] = i*10 + j;
//			}
//		}
//		mat1.printTransposed(System.out);
//		System.out.println();
//		mat1.print(System.out);
//		System.out.println();
//		
//		
//		OpinionsMatrix mat2 = new OpinionsMatrix(3, 7, true);
//		for (int i = 0; i < mat2.points.length; i++) {
//			PointND p = mat2.points[i];
//			for (int j = 0; j < p.x.length; j++) {
//				p.x[j] = i*110 + j* 1.11111f;
//			}
//		}
//		
//		mat2.print(System.out);
//		
//		System.out.println();
//		System.out.println();
//		
////		mat2.printTransposed(System.out);
//		
//		for (int i = 0; i < mat2.points.length; i++) {
//			PointND p2 = mat2.points[i];
//			double dist = p2.getDist(mat1.points[i]);
//			System.out.format(Defaults.OUTPUT_FORMAT, dist);
//		}
//		System.out.println();
//		
//	}
	public float calculateTotalDifference(float[][] tempX) {
		float totalDiff=0;
		for (int i = 0; i < points.length; i++) {
			totalDiff += PointND.getDistRawData(points[i].x, tempX[i]);
		}
		return totalDiff;
	}
	
	public float calculateMaxTotalDifference(float[][] tempX) {
		float totalDiff=0;
		float localDiff = 0;
		for (int i = 0; i < points.length; i++) {
			localDiff = PointND.getDistRawData(points[i].x, tempX[i]);
			totalDiff = (localDiff>totalDiff)?localDiff:totalDiff;
		}
		return totalDiff;
	}
	
	
	public void normalize() {
		for (PointND pointND : points) {
			pointND.normalize();
		}
	}
	public void set(PointND[] points) {
		n=points.length/2;
		d=PointND.d;
		this.points = points;
		this.data= new float[points.length][];
		for (int i = 0; i < data.length; i++) {
			data[i] = points[i].x;
		}
	}
	public void randomize(Random random) {
		for (PointND pointND : points) {
			float[] x = pointND.x;
			for (int i = 0; i < x.length; i++) {
				x[i] = random.nextFloat();//already from 0.0 to 1.0
			}
		}
	}
	
	public float[][] multiply(EffectMatrix matrix) {
		//Implement
		float[][] ret = new float[points.length][d];
		int n = EffectMatrix.n;
		// multiply into ret
		for (int row = 0; row < d; row++) {
			for (int col = 0; col < points.length; col++) {
				float sum = 0;
				float[][] line = matrix.getLine(col);
				float[] halfLine = line[0];
				if (halfLine != null) {
					for (int i = 0; i < halfLine.length; i++) {
						sum += (halfLine[i] * data[i][row]);
					}
				}
				halfLine = line[1];
				if (halfLine != null) {
					for (int i = 0; i < halfLine.length; i++) {
						sum += (halfLine[i] * data[i+n][row]);
					}
				}
				if (Float.isNaN(sum)) {
					throw new NaNException("NAN detected while multiplying X x D in position ("+ (row + n) + ", " + col + ")");
				}
				ret[col][row] = sum;
			}
		}
		return ret;
	}
}
