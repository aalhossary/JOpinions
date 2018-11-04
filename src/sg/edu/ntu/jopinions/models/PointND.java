package sg.edu.ntu.jopinions.models;

import java.util.Arrays;
import java.util.function.Supplier;

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
	
	
	public PointND(String name, float[] x, int id) {
		checkDimensions(x);
		this.name = name;
		this.x = x;
		this.id = id;
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
	
	public void setX(float[] x) {
		checkDimensions(x);
		this.x = x;
	}

	public void matchValues(float[] x) {
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

	static float getDistRawData(float[] x1, float[] x2) {
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
			ret[i]= p1[i]- p2[i];
		}
		return ret;
	}

	public void normalize() {
		for (int i = 0; i < this.x.length; i++) {
			float sum = Utils.getSum(this.x);
			final float scale = 1.0f / sum;
			scale(scale);
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
		public static final String PULLOX = "P";
		public static final String CASTOR = "C";
//		private int numDim;
		private int next=0;
		String name;
		public PointNDSupplier(int numDim, String name) {
			PointND.d = numDim;
//			this.numDim = numDim;
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
}