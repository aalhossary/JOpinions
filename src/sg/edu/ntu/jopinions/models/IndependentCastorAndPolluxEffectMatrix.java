/**
 * 
 */
package sg.edu.ntu.jopinions.models;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.control.Simulation;

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
	
//	public static void main(String[] args) {
//		IndependentCastorAndPolluxEffectMatrix transition = new IndependentCastorAndPolluxEffectMatrix(2);
//		transition.quadrantCC= new float[][]{{1,2},{3,4}};
//		transition.quadrantPP= new float[][]{{10,20},{30,40}};
//
//		OpinionsMatrix x = new OpinionsMatrix(2, 2, true);
//		x.set(new float[][] {{0,1}, {10, 11}, {20, 21}, {30,31}});
//		
//		x.printTransposed(System.out);
//		System.out.println();
//		
//		x.match(transition.multiply(x));
//		x.printTransposed(System.out);
//		
////should be
////   0.00000  10.00000  20.00000  30.00000
////   1.00000  11.00000  21.00000  31.00000
////
////  20.00000  40.00000 800.000001800.00000
////  23.00000  47.00000 830.000001870.00000
//	}
	
	
}
