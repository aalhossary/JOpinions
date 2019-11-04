/**
 * 
 */
package sg.edu.ntu.jopinions.models;

import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.Defaults;

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
//		int n = EffectMatrix.n;
		Graph<PointND, DefaultEdge> graphCC = graphs[0];
		Graph<PointND, DefaultEdge> graphPP = graphs[3];
		float nominator = 1, denominator;
		final float onePlusEgo_Over_Epsilon = (1 + ego) / Defaults.EPSILON/* + 0 */;
		Set<DefaultEdge> edgeSet;
		//calculate cc
		edgeSet = graphCC.edgeSet();
		for (DefaultEdge edge : edgeSet) {
			PointND edgeSource = graphCC.getEdgeSource(edge);
			PointND edgeTarget = graphCC.getEdgeTarget(edge);
			if (edgeSource == edgeTarget) {
				quadrantCC[edgeTarget.getId()][edgeSource.getId()] = onePlusEgo_Over_Epsilon;
			} else {
				denominator = Constants.EPSILON + edgeSource.getDist(edgeTarget);
				quadrantCC[edgeTarget.getId()][edgeSource.getId()] = nominator/denominator;
			}
		}
		//calculate pp
		edgeSet = graphPP.edgeSet();
		for (DefaultEdge edge : edgeSet) {
			PointND edgeSource = graphPP.getEdgeSource(edge);
			PointND edgeTarget = graphPP.getEdgeTarget(edge);
			if (edgeSource == edgeTarget) {
				quadrantPP[edgeTarget.getId()][edgeSource.getId()] = onePlusEgo_Over_Epsilon;
			} else {
				denominator = Constants.EPSILON + edgeSource.getDist(edgeTarget);
				quadrantPP[edgeTarget.getId()][edgeSource.getId()] = nominator / denominator;
			}
		}
	}
}
