package sg.edu.ntu.jopinions.models;

import java.util.Arrays;
import java.util.function.Supplier;

import sg.edu.ntu.jopinions.Defaults;

/**A point in N-Dimensions.
 * The number of dimensions is an arbitrary number.
 * @author Amr
 *
 */
public class PointND {
	static int d = 0;
	
	String name;
	float[] x;
	/**caching the ID to find its index faster*/
	private int id = -1;

	public static float DEFAULT_EGO = Defaults.DEFAULT_EGO;
	float[] ego = {-1.0f};
	private int inDegree;
	private int outDegree;
	
	
	public PointND(String name, float[] x, int id) {
		this(name, x, id, DEFAULT_EGO);
	}
	public PointND(String name, float[] x, int id, float ego) {
		checkDimensions(x);
		this.name = name;
		this.x = x;
		this.id = id;
		this.ego[0] = ego;
	}
	
	/**
	 * @return a copy of the data
	 */
	public float[] copyX_i() {
		return x.clone();
	}
	
	/** accessor to the internal data of the point (<code>x_i</code>).
	 * @return a reference to <code>x_i</code> itself.
	 */
	public float[] getX_i() {
		return x;
	}
	
	/**@deprecated dangerous. use {@link #match(float[])} instead.
	 * @param x the float array to use instead of its float array.
	 */
	void setX(float[] x) {
		checkDimensions(x);
		this.x = x;
	}

	public void match(float[] x) {
		checkDimensions(x);
		System.arraycopy(x, 0, this.x, 0, d);
	}
	
	private void checkDimensions(float[] x) {
		if(d != 0 && x.length != d)
			throw new IllegalArgumentException("non mtching dimensions");
	}
	
	public float getDist(PointND other) {
		return this == other ? 0 : getDistRawData(this.x, other.x);
	}

	public static float getDistRawData(float[] x1, float[] x2) {
		double ret = 0;
		for (int i = 0; i < x1.length; i++) {
			float deltaX_i = x1[i]-x2[i];
			ret += (deltaX_i*deltaX_i);
		}
		ret /= x1.length;
		return (float) Math.sqrt(ret);
	}
	
	public float[] minus(PointND other) {
		return minusRawData(this.x, other.x);
	}
	public static float[] minusRawData(float[] p1, float[] p2) {
		float[] ret = new float[p1.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i]= p1[i] - p2[i];
		}
		return ret;
	}
	public static float[] plusRawData(float[] p1, float[] p2) {
		float[] ret = new float[p1.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i]= p1[i] + p2[i];
		}
		return ret;
	}

	public void normalize() {
		float sum = Utils.getSum(this.x);
		final float scale = 1.0f / sum;
		scale(scale);
		if (Float.isNaN(scale)) {
			throw new NaNException("NAN detected while normalizing opinion point "+ toString());
		}
	}
	public PointND scale(float scale) {
		Utils.scaleLine(x, scale);
		return this;
	}
	public PointND add(PointND other) {
		float[] x = this.x;
		float[] otherX = other.x;
		for (int i = 0; i < x.length; i++) {
			x[i] +=otherX[i];
		}
		return this;
	}
	public static void setNumDimensions(int d) {
		PointND.d = d;
	}
	public static void setDefaultEgo(float defaultEgo) {
		PointND.DEFAULT_EGO = defaultEgo;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return this.name+this.id+Arrays.toString(this.x);
	}

	public static class PointNDSupplier implements Supplier<PointND>{
		private int next=0;
		String name;
		public PointNDSupplier(int numDim, String name) {
			PointND.d = numDim;
			this.name = name;
		}
		@Override
		public PointND get() {
			return new PointND(name, new float[PointND.d], next++);
		}
	}

	public static float dotProductRawData(float[] e1, float[] e2) {
		float ret = 0;
		for (int i = 0; i < e2.length; i++) {
			ret += e1[i]*e2[i];
		}
		return ret;
	}
	
	public static float[] crossProductRawData(float[] e1, float[] e2) {
		if(e1.length != 3)
			throw new UnsupportedOperationException("Number of Dimension not yet implemented");
		float[] ret = {0.0f, 0.0f, 0.0f};
		ret[0] = (e1[1] * e2[2]) - (e1[2] * e2[1]);
		ret[1] = (e1[2] * e2[0]) - (e1[0] * e2[2]);
		ret[2] = (e1[0] * e2[1]) - (e1[1] * e2[0]);
		return ret;
	}
	
	/**returns a <code>positive</code> value if the cross product is to the same side as the reference
	 * positive norm vector (i.e. the angle between the reference vector and the query vector clock wise,
	 * a <code>negative</code> value if on the other side, <code>zero</code> if on the boundary vector itself;
	 * @param refVector
	 * @param refVectorTail 
	 * @param queryPoint
	 * @param refPositiveSide
	 * @return
	 */
	public static float side(PointND refVector, PointND refVectorTail, float[] queryPoint, PointND refPositiveSide) {
		float[] queryVector = PointND.minusRawData(queryPoint, refVectorTail.x);
		float[] crossProductRawData = crossProductRawData(refVector.getX_i(), queryVector);
		return dotProductRawData(crossProductRawData, refPositiveSide.getX_i());
	}
	
	public static float side(PointND refVector, float[] queryVectorRawData, PointND refPositiveSide) {
		float[] crossProductRawData = crossProductRawData(refVector.getX_i(), queryVectorRawData);
		return dotProductRawData(crossProductRawData, refPositiveSide.getX_i());
	}
	
	
//	===================================================================
	/**	 Given three colinear points p, q, r, the function checks if point q lies on line segment 'pr' 
	 * @param p
	 * @param q
	 * @param r
	 * @return true if point p lies on line segment pr, false otherwise.
	 */
	static boolean onSegment(PointND p, PointND q, PointND r) {
		return 	q.x[0] <= Math.max(p.x[0], r.x[0]) && q.x[0] >= Math.min(p.x[0], r.x[0]) && 
				q.x[1] <= Math.max(p.x[1], r.x[1]) && q.x[1] >= Math.min(p.x[1], r.x[1]);
	}

	/**
	 * To find orientation of ordered triplet (p, q, r). <br>
	 * The function returns following values
	 * 0 --> p, q and r are colinear 
	 * 1 --> Clockwise
	 * 2 --> Counterclockwise 
	 * @param p
	 * @param q
	 * @param r
	 * @return
	 */
	static int orientation(PointND p, PointND q, PointND r) {
		// See https://www.geeksforgeeks.org/orientation-3-ordered-points/
		// for details of below formula.
		int val = (int) ((q.x[1] - p.x[1]) * (r.x[0] - q.x[0]) - (q.x[0] - p.x[0]) * (r.x[1] - q.x[1]));

		if (val == 0)
			return 0; // colinear

		return (val > 0) ? 1 : 2; // clock or counterclock wise
	} 

	/**
	 * The main function that returns true if line segment 'p1q1' and 'p2q2' intersect. 
	 * Based on code from {@link http://geeksforgeeks.org/check-if-two-given-line-segments-intersect/}
	 * 
	 * @param p1 segment 1 point 1 (e.g. Castor1)
	 * @param q1 segment 1 point 2 (e.g. Pullox1)
	 * @param p2 segment 2 point 1 (e.g. Castor2)
	 * @param q2 segment 2 point 2 (e.g. Pullox2)
	 * @return
	 */
	public static boolean doIntersect(PointND p1, PointND q1, PointND p2, PointND q2) {
		// Find the four orientations needed for general and
		// special cases
		int o1 = orientation(p1, q1, p2);
		int o2 = orientation(p1, q1, q2);
		int o3 = orientation(p2, q2, p1);
		int o4 = orientation(p2, q2, q1);

		// General case
		if (o1 != o2 && o3 != o4)
			return true;

		// Special Cases
		// p1, q1 and p2 are colinear and p2 lies on segment p1q1
		if (o1 == 0 && onSegment(p1, p2, q1))
			return true;

		// p1, q1 and q2 are colinear and q2 lies on segment p1q1
		if (o2 == 0 && onSegment(p1, q2, q1))
			return true;

		// p2, q2 and p1 are colinear and p1 lies on segment p2q2
		if (o3 == 0 && onSegment(p2, p1, q2))
			return true;

		// p2, q2 and q1 are colinear and q1 lies on segment p2q2
		if (o4 == 0 && onSegment(p2, q1, q2))
			return true;

		return false; // Doesn't fall in any of the above cases
	} 
	
	
	
	
	
	
	
	
//	===================================================================
	
	public String getName() {
		return name;
	}
	public int getInDegree() {
		return inDegree;
	}

	public void setInDegree(int inDegree) {
		this.inDegree = inDegree;
	}

	public int getOutDegree() {
		return outDegree;
	}

	public void setOutDegree(int outDegree) {
		this.outDegree = outDegree;
	}
	
	public void setEgo(float ego) {
		this.ego[0] = ego;
	}
	public float getEgo() {
		return ego[0];
	}
}