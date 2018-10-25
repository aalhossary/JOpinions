package sg.edu.ntu.opinions.control.cli;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.opinions.control.Simulation;
import sg.edu.ntu.opinions.models.IndependentCastorAndPolluxEffectMatrix;
import sg.edu.ntu.opinions.models.OpinionsMatrix;
import sg.edu.ntu.opinions.models.PointND;

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
		int dimensions = 4;
		int pairs=9;
		Simulation simulation = new Simulation();
		Graph<PointND, DefaultEdge> gCC = new DefaultDirectedGraph<>(DefaultEdge.class);
		Graph<PointND, DefaultEdge> gPP = new DefaultDirectedGraph<>(DefaultEdge.class);
		@SuppressWarnings("unchecked")
		Graph<PointND, DefaultEdge>[] graphs = (Graph<PointND, DefaultEdge>[]) new Graph[]{gCC, null,null,gPP};
		simulation.setGraphs(graphs);
		simulation.setD(new IndependentCastorAndPolluxEffectMatrix(pairs));
		
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
		simulation.start();
	}

}
