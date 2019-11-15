/**
 * 
 */
package sg.edu.ntu.jopinions.models;

import java.util.Iterator;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.Defaults;

/**
 * @author Amr
 *
 */
public class CoupledNetworkedCastorAndPolluxBetaEffectMatrix extends AbstractCoupledNetworkedCastorAndPolluxEffectMatrix {
	
	/**The coupling factor between corresponding Castors and pulloxes.*/
	private float beta = Defaults.DEFAULT_BETA;

	//=======cached variables============
	float oneMinusBeta;
	float[] oneMinusBeta_Times_OnePlusEgo;
	//=======cached variables============

	public CoupledNetworkedCastorAndPolluxBetaEffectMatrix(int n, float beta) {
		super(n);
		this.beta = beta;
	}
	
	@Override
	protected void cacheVariables(OpinionsMatrix x, Graph<PointND,DefaultEdge>[] graphs) {
		oneMinusBeta = 1 - beta;
		oneMinusBeta_Times_OnePlusEgo = new float[x.points.length];
		for (int i = 0; i < oneMinusBeta_Times_OnePlusEgo.length; i++) {
			oneMinusBeta_Times_OnePlusEgo[i] = oneMinusBeta * (1 + x.points[i].ego[0]);
		}
		variablesNotCached = false;
	}

	/* (non-Javadoc)
	 * @see sg.edu.ntu.jopinions.models.EffectMatrix#updateUsing(sg.edu.ntu.jopinions.models.OpinionsMatrix, org.jgrapht.Graph[])
	 */
	@Override
	public void updateUsing(OpinionsMatrix x, Graph<PointND, DefaultEdge>[] graphs) {
		if(variablesNotCached)
			cacheVariables(x, graphs);

		int n = EffectMatrix.n;
		Graph<PointND, DefaultEdge> graphCC = graphs[0];
//		Graph<PointND, DefaultEdge> graphPC = graphs[1];
//		Graph<PointND, DefaultEdge> graphCP = graphs[2];
		Graph<PointND, DefaultEdge> graphPP = graphs[3];
//		float beta = this.beta;
		float nominator, denominator;
		
		//fill PC and CP
		nominator = beta;
		for (int i = 0; i < n; i++) {
			float[][] pointX = x.data;
			denominator =  Constants.EPSILON + PointND.getDistRawData(pointX[i], pointX[i+n]);
			quadrantPC[i][i] = quadrantCP[i][i] = nominator / denominator;
		}
		
		//We will iterate on graph edges better than on n^2

		//fill CC
		final float oneMinusBeta = this.oneMinusBeta;
		final float[] oneMinusBeta_Times_OnePlusEgo = this.oneMinusBeta_Times_OnePlusEgo;
		Iterator<DefaultEdge> edgesCCIerator = graphCC.edgeSet().iterator();
		while (edgesCCIerator.hasNext()) {
			DefaultEdge edge = (DefaultEdge) edgesCCIerator.next();
			PointND edgeSource = graphCC.getEdgeSource(edge);
			PointND edgeTarget = graphCC.getEdgeTarget(edge);
			nominator = (edgeSource == edgeTarget) ? oneMinusBeta_Times_OnePlusEgo[edgeSource.getId()] : oneMinusBeta;
			float dist = edgeSource.getDist(edgeTarget);
			denominator = Constants.EPSILON + dist;
			//notice that the x dimension comes first in this implementation
			quadrantCC[edgeTarget.getId()][edgeSource.getId()] = nominator / denominator;
		}
		//fill PP
		Iterator<DefaultEdge> edgesPPIerator = graphPP.edgeSet().iterator();
		while (edgesPPIerator.hasNext()) {
			DefaultEdge edge = (DefaultEdge) edgesPPIerator.next();
			PointND edgeSource = graphPP.getEdgeSource(edge);
			PointND edgeTarget = graphPP.getEdgeTarget(edge);
			nominator = (edgeSource == edgeTarget) ? oneMinusBeta_Times_OnePlusEgo[edgeSource.getId()] : oneMinusBeta; //TODO +n?
			float dist = edgeSource.getDist(edgeTarget);
			denominator = Constants.EPSILON + dist;
			//notice that the x dimension comes first in this implementation
			quadrantPP[edgeTarget.getId()][edgeSource.getId()] = nominator / denominator;
		}
	}
}
