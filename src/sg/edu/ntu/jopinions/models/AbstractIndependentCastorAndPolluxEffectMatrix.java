/**
 * 
 */
package sg.edu.ntu.jopinions.models;

/**This is the base for both {@link IndependentCastorAndPolluxEffectMatrix} and {@link IndependentNetworkedCastorAndPolluxEffectMatrix}
 * @author Amr
 *
 */
public abstract class AbstractIndependentCastorAndPolluxEffectMatrix extends EffectMatrix {

	/**
	 * @param n
	 */
	public AbstractIndependentCastorAndPolluxEffectMatrix(int n) {
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
		case 2:
			return 0;
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
		float[] line;
		for (int i = 0; i < n; i++) {
			line = quadrantCC[i];
			sum = Utils.getSum(line);
			//NO PC quadrant in this implementation
			scale = 1.0f / sum;
			if (Float.isNaN(scale)) {
				throw new NaNException("NAN detected while normalizing D matrix in column " + i);
			}
			Utils.scaleLine(line, scale);
			
			line = quadrantPP[i];
			sum = Utils.getSum(line);
			//NO CP quadrant in this implementation
			scale = 1.0f / sum;
			if (Float.isNaN(scale)) {
				throw new NaNException("NAN detected while normalizing D matrix in column " + (i+n));
			}
			Utils.scaleLine(line, scale);
		}
	}

	@Override
	protected void initQuadrants(int n) {
		quadrantCC = new float[n][n];	quadrantCP= null;
		quadrantPC = null;				quadrantPP= new float[n][n];
	}
	
	@Override
	float[][] getLine(int col) {
		if (col < n) {
			return new float[][] {quadrantCC[col], null};
		} else {
			return new float[][] {null, quadrantPP[col - n]};
		}
	}
}
