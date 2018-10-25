/**
 * 
 */
package sg.edu.ntu.opinions.models;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.opinions.control.Simulation;

/**
 * @author Amr
 *
 */
public class IndependentCastorAndPolluxEffectMatrix extends AbstractIndependentCastorAndPolluxEffectMatrix {

	/**
	 * @param n
	 */
	public IndependentCastorAndPolluxEffectMatrix(int n) {
		super(n);
	}

	/* (non-Javadoc)
	 * @see sg.edu.ntu.opinions.models.EffectMatrix#updateUsing(sg.edu.ntu.opinions.models.OpinionsMatrix, org.jgrapht.Graph)
	 */
	@Override
	public void updateUsing(OpinionsMatrix x, Graph<PointND, DefaultEdge>[] graphs) {
		int n = EffectMatrix.n;
		//Nominator is always = 1
		double nominator=1;
		//calculate cc
		for (int i = 0; i < n; i++) {
			for (int j = 0; j <= i; j++) { //TODO < or <= ?
				PointND ci= x.points[i];
				PointND cj= x.points[j];
				float denom= Simulation.EPSILON + ci.getDist(cj);
				quadrantCC[i][j] = quadrantCC[j][i] = (float) (nominator/denom); //TODO revise
			}
		}
		//calculate pp
		for (int i = n; i < 2*n; i++) {
			for (int j = n; j <= i; j++) { //TODO < or <= ?
				PointND pi= x.points[i];
				PointND pj= x.points[j];
				double denom= Simulation.EPSILON + pi.getDist(pj);
				int ii=i-n, jj=j-n;;
				quadrantPP[ii][jj] = quadrantPP[jj][ii] = (float) (nominator/denom); //TODO revise
			}
		}
	}
	
}
