/**
 * 
 */
package sg.edu.ntu.jopinions.models;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.Defaults;

/**
 * @author Amr
 *
 */
public class FullyCoupledNetworkedCastorAndPolluxEffectMatrix extends AbstractCoupledNetworkedCastorAndPolluxEffectMatrix {
	
	/**The coupling factor between corresponding Castors and pulloxes.*/
	private float beta = Defaults.DEFAULT_BETA;

	/**
	 * @param n
	 */
	public FullyCoupledNetworkedCastorAndPolluxEffectMatrix(int n) {
		super(n);
	}

	public FullyCoupledNetworkedCastorAndPolluxEffectMatrix(int n, float beta) {
		this(n);
		this.beta = beta;
	}
	
	/* (non-Javadoc)
	 * @see sg.edu.ntu.jopinions.models.EffectMatrix#updateUsing(sg.edu.ntu.jopinions.models.OpinionsMatrix, org.jgrapht.Graph[])
	 */
	@Override
	public void updateUsing(OpinionsMatrix x, Graph<PointND, DefaultEdge>[] graphs) {
		int n = EffectMatrix.n;
		Graph<PointND, DefaultEdge> graphCC = graphs[0];
//		Graph<PointND, DefaultEdge> graphPC = graphs[1];
//		Graph<PointND, DefaultEdge> graphCP = graphs[2];
		Graph<PointND, DefaultEdge> graphPP = graphs[3];
		float alpha, beta = this.beta;
		float oneMinusAlpha, oneMinusBeta = 1-beta;
		float nominator, denominator;
		PointND yC, yP;
		
		PointND[] points = x.points;
		/**just a temporary point to avoid repetitive object allocation. use with caution*/
		PointND temp;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i == j) {//i.e myself (CC and PP) or my couple (PC and CP)
					//CC and PP (myself)
//					alpha = 1; yC = pointC, yP = pointP;
//					float dist = 0;
					nominator = oneMinusBeta;
					denominator = Constants.EPSILON; // + 0
					quadrantCC[i][i] = nominator / denominator;
					quadrantPP[i][i] = nominator / denominator;

					// PC and CP (my couple)
					nominator = beta;
					denominator = Constants.EPSILON + PointND.getDistRawData(points[i].x, points[i+n].x);
					quadrantPC[i][i] = nominator / denominator;
					quadrantCP[i][i] = nominator / denominator;
				} else {//other fellow Cs, other fellow Ps, or influencing others
					PointND pointCi= x.points[i];
					PointND pointCj= x.points[j];
					PointND pointPi= x.points[i + n];
					PointND pointPj= x.points[j + n];
					
					boolean hasEdgeCC = graphCC.containsEdge(pointCi, pointCj);
					boolean hasEdgePP = graphPP.containsEdge(pointPi, pointPj);
					if(!(hasEdgeCC || hasEdgePP))
						continue;

					float[] vectorV1 = pointCi.minus(pointPi);
					//equals |v1|^2
					float v1v1DotProduct = PointND.dotProductRawData(vectorV1, vectorV1);
					
					if (hasEdgeCC) {
						//effect of pair (Ci, Pi) on point Pj
						float[] vectorV2 = pointCj.minus(pointPi);
						//the dot product is |v1| * |v2| * cos (theta)
						float v1v2DotProduct = PointND.dotProductRawData(vectorV1, vectorV2);
						if (v1v2DotProduct <= 0) {
							//point Pj projection is before or on Pi.
							alpha = 0; oneMinusAlpha = 1;
							yC = pointPi;
						} else if(v1v2DotProduct >= v1v1DotProduct){
							//in the line above, notice that v1v1DotProduct actually equals vectorV1LenSqr |v1| * |v1| * cos(0)

							//point Cj projection is on or after Ci.
							alpha = 1; oneMinusAlpha = 0;
							yC = pointCi;
						} else {
							alpha = (float) Math.sqrt(v1v2DotProduct / v1v1DotProduct);
							oneMinusAlpha = 1 - alpha;
							yC = new PointND("yCj", pointCi.copyX_i(), j).scale(alpha);
							temp = new PointND("Temp", pointPi.copyX_i(), -1).scale(oneMinusAlpha);
							yC.add(temp);
						}
						
						//update PC
						nominator = oneMinusBeta * oneMinusAlpha;
						denominator = Constants.EPSILON + yC.getDist(pointCj);
						quadrantPC[j][i] = nominator / denominator;
						
						//update CC
						nominator = oneMinusBeta * alpha;
//						denominator = Simulation.EPSILON + yC.getDist(pointCj);//No need to recalculate
						//notice that the x dimension comes first in this implementation
						quadrantCC[j][i] = nominator / denominator;
					}
					
					if (hasEdgePP) {
						//effect of pair (Ci, Pi) on point Pj
						float[] vectorV2 = pointPj.minus(pointPi);
						//the dot product is |v1| * |v2| * cos (theta)
						float v1v2DotProduct = PointND.dotProductRawData(vectorV1, vectorV2);
						if (v1v2DotProduct <= 0) {
							//point Pj projection is before or on Pi.
							alpha = 0; oneMinusAlpha = 1;
							yP = pointPi;
						} else if(v1v2DotProduct >= v1v1DotProduct){
							//in the line above, notice that v1v1DotProduct actually equals vectorV1LenSqr |v1| * |v1| * cos(0)

							//point Pj projection is on or after Ci.
							alpha = 1; oneMinusAlpha = 0;
							yP = pointCi;
						} else {
							alpha = (float) Math.sqrt(v1v2DotProduct / v1v1DotProduct);
							oneMinusAlpha = 1 - alpha;
							yP = new PointND("yPj", pointCi.copyX_i(), j).scale(alpha);
							temp = new PointND("Temp", pointPi.copyX_i(), -1).scale(oneMinusAlpha);
							yP.add(temp);
						}
						
						//update CP
						nominator = oneMinusBeta * oneMinusAlpha;
						denominator = Constants.EPSILON + yP.getDist(pointPj);
						quadrantCP[j][i] = nominator / denominator;
						
						//update PP
						nominator = oneMinusBeta * alpha;
//						denominator = Simulation.EPSILON + yP.getDist(pointPj);//No need to recalculate
						//notice that the x dimension comes first in this implementation
						quadrantPP[j][i] = nominator / denominator;
					}
				}
			}
		}
	}
}
