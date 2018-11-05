/**
 * 
 */
package sg.edu.ntu.jopinions.models;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.Constants;

/**
 * @author Amr
 *
 */
public class IndependentNetworkedCastorAndPolluxEffectMatrix extends AbstractIndependentCastorAndPolluxEffectMatrix{

	/**
	 * @param n
	 */
	public IndependentNetworkedCastorAndPolluxEffectMatrix(int n) {
		super(n);
	}


	/* (non-Javadoc)
	 * @see sg.edu.ntu.opinions.models.EffectMatrix#updateUsing(sg.edu.ntu.opinions.models.OpinionsMatrix, org.jgrapht.Graph)
	 */
	@Override
	public void updateUsing(OpinionsMatrix x, Graph<PointND, DefaultEdge>[] graphs) {
		int n = EffectMatrix.n;
		Graph<PointND, DefaultEdge> graphCC = graphs[0];
		Graph<PointND, DefaultEdge> graphPP = graphs[3];
		double nominator;
		//calculate cc
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				PointND ci= x.points[i];
				PointND cj= x.points[j];
				//Nominator is 1 if there is edge
				nominator = graphCC.containsEdge(ci, cj) ? 1 : 0;
				if(nominator == 0) {
					quadrantCC[j][i] = 0;
				}else {
					double denom= Constants.EPSILON + ci.getDist(cj);
					quadrantCC[j][i] = (float) (nominator/denom); //TODO revise
				}
			}
		}
		//calculate pp
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				PointND pi= x.points[i+n];
				PointND pj= x.points[j+n];
				//Nominator is 1 if there is edge
				nominator = graphPP.containsEdge(pi, pj) ? 1 : 0;
				if(nominator == 0) {
					quadrantPP[j][i] = 0;
				}else {
					double denom= Constants.EPSILON + pi.getDist(pj);
					quadrantPP[j][i] = (float) (nominator/denom); //TODO revise
				}
			}
		}
	}
}
