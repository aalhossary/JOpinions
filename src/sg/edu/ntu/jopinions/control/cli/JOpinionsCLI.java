package sg.edu.ntu.jopinions.control.cli;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.control.Simulation;
import sg.edu.ntu.jopinions.models.IndependentCastorAndPolluxEffectMatrix;
import sg.edu.ntu.jopinions.models.IndependentNetworkedCastorAndPolluxEffectMatrix;
import sg.edu.ntu.jopinions.models.OpinionsMatrix;
import sg.edu.ntu.jopinions.models.PointND;

public class JOpinionsCLI {

	public JOpinionsCLI() {
		
	}

	public static void main(String[] args) {
		JOpinionsCLI cli = new JOpinionsCLI();
		if (args.length == 0) {
			cli.demo();
		} else {
			//Manage :)
		}
	}

	private void demo() {
		int dimensions = 3;
		int pairs=5;
		Simulation simulation = new Simulation();
		Graph<PointND, DefaultEdge> gCC = new DefaultDirectedGraph<>(DefaultEdge.class);
		Graph<PointND, DefaultEdge> gPP = new DefaultDirectedGraph<>(DefaultEdge.class);
		@SuppressWarnings("unchecked")
		Graph<PointND, DefaultEdge>[] graphs = (Graph<PointND, DefaultEdge>[]) new Graph[]{gCC, null,null,gPP};
		simulation.setGraphs(graphs);
//		simulation.setD(new IndependentCastorAndPolluxEffectMatrix(pairs));
		simulation.setD(new IndependentNetworkedCastorAndPolluxEffectMatrix(pairs));
		
		OpinionsMatrix x = new OpinionsMatrix(pairs, dimensions);
		float[][] data = new float[2*pairs][dimensions]; 
		for (int i = 0; i < data.length; i++) {
			float[] tempX = new float[dimensions];
			for (int j = 0; j < dimensions; j++) {
				tempX[j]= i * 11.1111f + j;
			}
			data[i]= tempX;
		}
		x.set(data);
		simulation.setX(x);
		
		List<PointND> cPoints = new ArrayList<>(pairs);
		List<PointND> pPoints = new ArrayList<>(pairs);
		
		for (int i = 0; i < pairs; i++) {
			cPoints.add(x.points[i]);
			pPoints.add(x.points[i+pairs]);
		}
		Graphs.addAllVertices(gCC, cPoints);
		Graphs.addAllVertices(gPP, pPoints);
		//add self edges
		for (int i = 0; i < pairs; i++) {
			gCC.addEdge(cPoints.get(i), cPoints.get(i));
			gPP.addEdge(pPoints.get(i), pPoints.get(i));
		}
		Graphs.addOutgoingEdges(gCC,cPoints.get(0), cPoints);
		Graphs.addOutgoingEdges(gPP,pPoints.get(pairs-1), pPoints);//pairs-1
		
		
		simulation.start();
	}

}
