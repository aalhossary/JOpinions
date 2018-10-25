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
		switch (quadrant) {
		case 0:
//			return data[quadrant][y][x];
			return getEffectWithinQuadrant(quadrantCC, y, x);
		case 1:
		case 2:
			return 0;
		case 3:
//			return data[quadrant][y-n][x-n];
			return getEffectWithinQuadrant(quadrantPP, y-n, x-n);
		default:
			throw new IllegalArgumentException("unexpeccted numbers ("+y+", "+x+")");
		}
		// TODO validate it
	}

	/* (non-Javadoc)
	 * @see sg.edu.ntu.opinions.models.EffectMatrix#normalize()
	 */
	@Override
	public void normalize() {
		float sum = getQuadrantSum(quadrantCC);
		//NO PC quadrant in this implementation
		float scale = 1.0f / sum;
		scaleQuadrant(quadrantCC, scale);

		sum = getQuadrantSum(quadrantPP);
		//NO CP quadrant in this implementation
		scale = 1.0f / sum;
		scaleQuadrant(quadrantPP, scale);
	}

	@Override
	protected void initQuadrants(int n) {
		quadrantCC = new float[n][n];	quadrantCP= null;
		quadrantPC = null;				quadrantPP= new float[n][n];
	}
	
	
	@Override
	public float[][] multiply(OpinionsMatrix x) {
		float[][] ret = new float[x.points.length][x.d];
		
		// multiply into ret
		for (int row = 0; row < x.data.length; row++) {
			for (int col = 0; col < x.d; col++) {

				float sum = 0;

//				//ideally, this is how it should be
//				for (int i = 0; i < 2* n ; i++) {
//					sum += this.data[getQuadrant(rows, cols)][rows][i] * x.data[i][rows];
//				}
				
				if (row < n) {
					// CC and CP
					for (int i = 0; i < n ; i++) {
						sum += this.quadrantCC[row][i] * x.data[i][col];
					}
					//NO C --> P in this matrix
				} else {
					//PC and PP
					//NO P --> C in this matrix
					for (int i = 0; i < n ; i++) {
						sum += this.quadrantPP[row - n][i] * x.data[i + n][col];
					}
				}
				
				ret[row][col] = sum;
			}
		}
		
		return ret;
	}
	
}
