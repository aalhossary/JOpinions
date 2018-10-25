package sg.edu.ntu.jopinions.models;

/**A point in N-Dimensions.
 * The number of dimensions is an arbitrary number.
 * @author Amr
 *
 */
public class PointND {
	static int d = 0;
	
	float[] x;
	
	
	public PointND(float[] x) {
		if(d != 0 && x.length != d)
			throw new IllegalArgumentException("non mtching dimensions");
		this.x = x;
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
		if(d != 0 && x.length != d)
			throw new IllegalArgumentException("non mtching dimensions");
		this.x = x;
	}
	
	public void matchValues(float[] x) {
		if(d != 0 && x.length != d)
			throw new IllegalArgumentException("non mtching dimensions");
		System.arraycopy(x, 0, this.x, 0, d);
	}
	
	public float getDist(PointND other) {
		return getDistRow(this.x, other.x);
	}

	static float getDistRow(float[] x1, float[] x2) {
		double ret = 0;
		for (int i = 0; i < x1.length; i++) {
			float deltaX_i = x1[i]-x2[i];
			ret += (deltaX_i*deltaX_i);
		}
		ret /= x1.length;
		return (float) Math.sqrt(ret);
	}

	public void normalize() {
		for (int i = 0; i < this.x.length; i++) {
			float sum = Utils.getSum(this.x);
			//NO PC quadrant in this implementation
			float scale = 1.0f / sum;
			Utils.scaleLine(this.x, scale);
		}
	}
}