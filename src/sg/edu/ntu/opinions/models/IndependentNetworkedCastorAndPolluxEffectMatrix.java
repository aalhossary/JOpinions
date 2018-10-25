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
public class IndependentNetworkedCastorAndPolluxEffectMatrix extends IndependentCastorAndPolluxEffectMatrix{

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
		Graph<PointND, DefaultEdge> gCC = graphs[0];
		Graph<PointND, DefaultEdge> gPP = graphs[3];
		double nominator;
		//calculate cc
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				PointND ci= x.points[i];
				PointND cj= x.points[j];
				//Nominator is 1 if there is edge
				nominator = gCC.containsEdge(ci, cj) ? 1 : 0;
				if(nominator == 0) {
					quadrantCC[i][j] = 0;
				}else {
					double denom= Simulation.EPSILON + ci.getDist(cj);
					quadrantCC[i][j] = (float) (nominator/denom); //TODO revise
				}
			}
		}
		//calculate pp
		for (int i = n; i < 2*n; i++) {
			for (int j = n; j < 2*n; j++) {
				PointND pi= x.points[i];
				PointND pj= x.points[j];
				//Nominator is 1 if there is edge
				nominator = gPP.containsEdge(pi, pj) ? 1 : 0;
				if(nominator == 0) {
					quadrantPP[i][j] = 0;
				}else {
					double denom= Simulation.EPSILON + pi.getDist(pj);
					quadrantPP[i][j] = (float) (nominator/denom); //TODO revise
				}
			}
		}
	}
}
