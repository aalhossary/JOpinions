/**
 * 
 */
package sg.edu.ntu.jopinions.models;

import java.util.Iterator;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.control.Simulation;

/**
 * @author Amr
 *
 */
public class CoupledNetworkedCastorAndPolluxBetaEffectMatrix extends AbstractCoupledNetworkedCastorAndPolluxEffectMatrix {
	
	/**The coupling factor between corresponding Castors and pulloxes.*/
	private float beta = DEFAULT_BETA;

	/**
	 * @param n
	 */
	public CoupledNetworkedCastorAndPolluxBetaEffectMatrix(int n) {
		super(n);
	}

	public CoupledNetworkedCastorAndPolluxBetaEffectMatrix(int n, float beta) {
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
		float beta = this.beta;
		float nominator = 1 - beta, denominator;
		
		//fill PC and CP
		for (int i = 0; i < n; i++) {
			float[][] pointX = x.data;
			denominator =  PointND.getDistRawData(pointX[i], pointX[i+n]);
			quadrantPC[i][i] = beta / denominator;
			quadrantCP[i][i] = beta / denominator;
		}
		
		//We will iterate on graph edges better than on n^2

		//fill CC
		Iterator<DefaultEdge> edgesCCIerator = graphCC.edgeSet().iterator();
		while (edgesCCIerator.hasNext()) {
			DefaultEdge edge = (DefaultEdge) edgesCCIerator.next();
			PointND edgeSource = graphCC.getEdgeSource(edge);
			PointND edgeTarget = graphCC.getEdgeTarget(edge);
			float dist = edgeSource.getDist(edgeTarget);
			denominator = Simulation.EPSILON + dist;
			//notice that the x dimension comes first in this implementation
			quadrantCC[edgeTarget.getId()][edgeSource.getId()] = nominator / denominator;
		}
		//fill PP
		Iterator<DefaultEdge> edgesPPIerator = graphPP.edgeSet().iterator();
		while (edgesPPIerator.hasNext()) {
			DefaultEdge edge = (DefaultEdge) edgesPPIerator.next();
			PointND edgeSource = graphPP.getEdgeSource(edge);
			PointND edgeTarget = graphPP.getEdgeTarget(edge);
			float dist = edgeSource.getDist(edgeTarget);
			denominator = Simulation.EPSILON + dist;
			//notice that the x dimension comes first in this implementation
			quadrantPP[edgeTarget.getId()][edgeSource.getId()] = nominator / denominator;
		}
	}
}
