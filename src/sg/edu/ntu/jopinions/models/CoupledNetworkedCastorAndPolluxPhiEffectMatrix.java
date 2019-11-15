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
public class CoupledNetworkedCastorAndPolluxPhiEffectMatrix extends AbstractCoupledNetworkedCastorAndPolluxEffectMatrix {
	
	/**The coupling factor between corresponding Castors and pulloxes.*/
	private float phi = Defaults.DEFAULT_PHI;

	float oneMinusPhi;
	float[] oneMinusPhi_Times_onePlusEgo;

	/**
	 * @param n
	 */
	public CoupledNetworkedCastorAndPolluxPhiEffectMatrix(int n) {
		super(n);
	}

	public CoupledNetworkedCastorAndPolluxPhiEffectMatrix(int n, float phi) {
		this(n);
		this.phi = phi;
	}
	
	@Override
	public void normalize() {
		int n = EffectMatrix.n;
		float sum, scale;
		float[] line1, line2;
		float excludedPhi ;
		for (int i = 0; i < n; i++) {
			line1 = quadrantCC[i];
			sum = Utils.getSum(line1);
			line2 = quadrantPC[i];
			sum += Utils.getSum(line2);
			
			excludedPhi = quadrantPC[i][i];
			quadrantPC[i][i]=0;
			scale = (1.0f - excludedPhi) / (sum - excludedPhi);

			if (Float.isNaN(scale)) {
				throw new NaNException("NAN detected while normalizing D matrix in column " + i);
			}
			Utils.scaleLine(line1, scale);
			Utils.scaleLine(line2, scale);
			quadrantPC[i][i] = excludedPhi;
			
			line1 = quadrantCP[i];
			sum = Utils.getSum(line1);
			line2 = quadrantPP[i];
			sum += Utils.getSum(line2);

//			phi = quadrantCP[i][i];
			quadrantCP[i][i]=0;
			scale = (1.0f - excludedPhi) / (sum - excludedPhi);
			
			if (Float.isNaN(scale)) {
				throw new NaNException("NAN detected while normalizing D matrix in column " + (i+n));
			}
			Utils.scaleLine(line1, scale);
			Utils.scaleLine(line2, scale);
			quadrantCP[i][i] = excludedPhi;
		}
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
		float phi = this.phi;
		float nominator, denominator;
		final float oneMinusPhi = this.oneMinusPhi;
		final float[] oneMinusPhi_Times_onePlusEgo = this.oneMinusPhi_Times_onePlusEgo;

		
		//fill PC and CP
		for (int i = 0; i < n; i++) {
			quadrantPC[i][i] = phi;
			quadrantCP[i][i] = phi;
		}
		//We will iterate on graph edges better than on n^2

		//fill CC
		Iterator<DefaultEdge> edgesCCIerator = graphCC.edgeSet().iterator();
		while (edgesCCIerator.hasNext()) {
			DefaultEdge edge = (DefaultEdge) edgesCCIerator.next();
			PointND edgeSource = graphCC.getEdgeSource(edge);
			PointND edgeTarget = graphCC.getEdgeTarget(edge);
			float dist = edgeSource.getDist(edgeTarget);
			denominator = Constants.EPSILON + dist;
			final int targetId = edgeTarget.getId();
			final int sourceId = edgeSource.getId();
			nominator = (sourceId == targetId) ? oneMinusPhi_Times_onePlusEgo[edgeSource.getId()] : oneMinusPhi;
			//notice that the x dimension comes first in this implementation
			quadrantCC[targetId][sourceId] = nominator / denominator;
		}
		//fill PP
		Iterator<DefaultEdge> edgesPPIerator = graphPP.edgeSet().iterator();
		while (edgesPPIerator.hasNext()) {
			DefaultEdge edge = (DefaultEdge) edgesPPIerator.next();
			PointND edgeSource = graphPP.getEdgeSource(edge);
			PointND edgeTarget = graphPP.getEdgeTarget(edge);
			float dist = edgeSource.getDist(edgeTarget);
			final int targetId = edgeTarget.getId();
			final int sourceId = edgeSource.getId();
			nominator = (sourceId == targetId) ? oneMinusPhi_Times_onePlusEgo[edgeSource.getId()] : oneMinusPhi;
			denominator = Constants.EPSILON + dist;
			//notice that the x dimension comes first in this implementation
			quadrantPP[targetId][sourceId] = nominator / denominator;
		}
	}

	@Override
	protected void cacheVariables(OpinionsMatrix x, Graph<PointND, DefaultEdge>[] graphs) {
		this.oneMinusPhi = 1 - phi;
		final PointND[] points = x.points;
		this.oneMinusPhi_Times_onePlusEgo = new float[points.length];
		for (int i = 0; i < oneMinusPhi_Times_onePlusEgo.length; i++) {
			this.oneMinusPhi_Times_onePlusEgo[i] = oneMinusPhi * (1+ points[i].ego[0]);
		}
		variablesNotCached = false;
	}
}
