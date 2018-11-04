/**
 * 
 */
package sg.edu.ntu.jopinions.models;

/**This is the base for {@link CoupledNetworkedCastorAndPolluxPhiEffectMatrix}, {@link CoupledNetworkedCastorAndPolluxBetaEffectMatrix},
 *  and {@link FullyCoupledNetworkedCastorAndPolluxEffectMatrix}
 * @author Amr
 */
public abstract class AbstractCoupledNetworkedCastorAndPolluxEffectMatrix extends EffectMatrix {

	public static final float DEFAULT_PHI = 0.75f;
	public static final float DEFAULT_BETA = 0.75f;

	/**
	 * @param n
	 */
	public AbstractCoupledNetworkedCastorAndPolluxEffectMatrix(int n) {
		super(n);
	}

	/* (non-Javadoc)
	 * @see sg.edu.ntu.opinions.models.EffectMatrix#getEffect(int, int)
	 */
	@Override
	public float getEffect(int y, int x) {
		int quadrant = getQuadrant(y, x);
//		return data[quadrant][y][x];
		switch (quadrant) {
		case 0:
			return getEffectWithinQuadrant(quadrantCC, y, x);
		case 1:
			return getEffectWithinQuadrant(quadrantPC, y - n, x);
		case 2:
			return getEffectWithinQuadrant(quadrantCP, y, x - n);
		case 3:
			return getEffectWithinQuadrant(quadrantPP, y - n, x - n);
		default:
			throw new IllegalArgumentException("unexpeccted numbers ("+y+", "+x+")");
		}
	}

	/* (non-Javadoc)
	 * @see sg.edu.ntu.opinions.models.EffectMatrix#normalize()
	 */
	@Override
	public void normalize() {
		int n = EffectMatrix.n;
		float sum, scale;
		float[] line1, line2;
		for (int i = 0; i < n; i++) {
			line1 = quadrantCC[i];
			sum = Utils.getSum(line1);
			line2 = quadrantPC[i];
			sum += Utils.getSum(line2);

			scale = 1.0f / sum;
			Utils.scaleLine(line1, scale);
			Utils.scaleLine(line2, scale);
			
			line1 = quadrantCP[i];
			sum = Utils.getSum(line1);
			line2 = quadrantPP[i];
			sum += Utils.getSum(line2);
			scale = 1.0f / sum;
			Utils.scaleLine(line1, scale);
			Utils.scaleLine(line2, scale);
		}
	}

	@Override
	protected void initQuadrants(int n) {
		quadrantCC = new float[n][n];	quadrantCP= new float[n][n];
		quadrantPC = new float[n][n];	quadrantPP= new float[n][n];
	}
	
	@Override
	float[][] getLine(int col) {
		if (col < n) {
			return new float[][] {quadrantCC[col], quadrantPC[col]};
		} else {
			return new float[][] {quadrantCP[col - n], quadrantPP[col - n]};
		}
	}
}
