package sg.edu.ntu.jopinions.control.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.KleinbergSmallWorldGraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.util.SupplierUtil;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.Defaults;
import sg.edu.ntu.jopinions.control.Simulation;
import sg.edu.ntu.jopinions.models.CoupledNetworkedCastorAndPolluxBetaEffectMatrix;
import sg.edu.ntu.jopinions.models.CoupledNetworkedCastorAndPolluxPhiEffectMatrix;
import sg.edu.ntu.jopinions.models.EffectMatrix;
import sg.edu.ntu.jopinions.models.FullyCoupledNetworkedCastorAndPolluxEffectMatrix;
import sg.edu.ntu.jopinions.models.IndependentCastorAndPolluxEffectMatrix;
import sg.edu.ntu.jopinions.models.IndependentNetworkedCastorAndPolluxEffectMatrix;
import sg.edu.ntu.jopinions.models.OpinionsMatrix;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.PointND.PointNDSupplier;
import sg.edu.ntu.jopinions.models.Utils;

public class JOpinionsCLI {
	
	private static JOpinionsCLI instance=null;
	
	private JOpinionsCLI() {}
	public static JOpinionsCLI getInstance() {
		if (instance == null) {
			instance = new JOpinionsCLI();
		}
		return instance;
	}

	public static void main(String[] args) {
		JOpinionsCLI cli = JOpinionsCLI.getInstance();
		if (args.length == 0) {
			printUsage();
		}
		
		boolean help = Boolean.valueOf(Utils.getParameter(args, "-h", "true", "false")) || Boolean.valueOf(Utils.getParameter(args, "-help", "true", "false"));
		if (help) {
			printUsage();
			return;
		}
		boolean demo = Boolean.valueOf(Utils.getParameter(args, "-demo", "true", "false"));
		if(demo) {
			cli.demo(args);
			return;
		}
		Simulation simulation = new Simulation();
		boolean verbose = Boolean.valueOf(Utils.getParameter(args, "-v", "true", "false"));
		simulation.setVerbose(verbose);
		int numCouples = Integer.valueOf(Utils.getParameter(args, "-numCouples", "-1", "1000"));
		int numDimensions = Integer.valueOf(Utils.getParameter(args, "-dimensions", "", String.valueOf(Defaults.DEFAULT_NUM_DIMENSIONS)));
		
		PointND.setNumDimensions(numDimensions);
		Graph<PointND,DefaultEdge> graphCC = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, PointNDSupplier.CASTOR),SupplierUtil.createDefaultEdgeSupplier(),false);
//		Graph<PointND,DefaultEdge> graphCP = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, PointNDSupplier.PULLOX),SupplierUtil.createDefaultEdgeSupplier(),false);
//		Graph<PointND,DefaultEdge> graphPC = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, PointNDSupplier.PULLOX),SupplierUtil.createDefaultEdgeSupplier(),false);
		Graph<PointND,DefaultEdge> graphPP = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, PointNDSupplier.PULLOX),SupplierUtil.createDefaultEdgeSupplier(),false);

		//add Topology Random generators
		GraphGenerator<PointND, DefaultEdge, PointND> generator = createTopologyGenerator(args, simulation, numCouples);
		generator.generateGraph(graphCC);
		generator.generateGraph(graphPP);
		graphCC.vertexSet().stream().forEach(vertix -> graphCC.addEdge(vertix, vertix));
		graphPP.vertexSet().stream().forEach(vertix -> graphPP.addEdge(vertix, vertix));

		@SuppressWarnings("unchecked")
		Graph<PointND, DefaultEdge>[] graphs = (Graph<PointND, DefaultEdge>[]) new Graph[]{graphCC, null, null,graphPP};
		simulation.setGraphs(graphs);
		
		//get model from parameters
		EffectMatrix model = createDynamicsModel(args, simulation, numCouples);
		
		simulation.setD(model);

		OpinionsMatrix x = createOpinionsMatrix(numCouples, numDimensions, graphCC, graphPP);
		//TODO remove the local variable seed later
		long seed = Long.valueOf(Utils.getParameter(args, "-seed", "0", ""+System.currentTimeMillis()));// "123456789"
		x.randomize(seed);
		simulation.setX(x);
		simulation.start();
	}
	private static GraphGenerator<PointND, DefaultEdge, PointND> createTopologyGenerator(String[] args,
			Simulation simulation, int numCouples) {
		GraphGenerator<PointND, DefaultEdge, PointND> generator;
		long seed = Long.valueOf(Utils.getParameter(args, "-seed", "0", ""+System.currentTimeMillis())); // "123456789"
		String topology = Utils.getParameter(args, "-topology", null, Defaults.DEFAULT_TOPOLOGY);
		switch (topology) {
		case Constants.TOPOLOGY_BARABASI_ALBERT_GRAPH:
			int m0	= Integer.valueOf(Utils.getParameter(args, "-m0", "10", "10"));
			int m 	= Integer.valueOf(Utils.getParameter(args, "-m",  "", "2"));
			generator = new BarabasiAlbertGraphGenerator<>(m0, m, numCouples, seed);
			break;

		case Constants.TOPOLOGY_ERDOS_RENYI_GNP_RANDOM_GRAPH:
			int numEdges = Integer.valueOf(Utils.getParameter(args, "-edges", "0", "-1"));
			if(numEdges == -1) {
//				numEdges = numCouples * 3;
				numEdges = (numCouples / 20) * (numCouples-1);
			}
			generator = new GnmRandomGraphGenerator<>(numCouples, numEdges, seed);
			break;

		case Constants.TOPOLOGY_KLEINBERG_SMALL_WORLD_GRAPH:
			double sqrt = Math.sqrt(numCouples);
			if (sqrt != (int)sqrt) {
				throw new RuntimeException("numCouples must have a square root for Kleinberg model");
			}
			int propabilityDistripution = Integer.valueOf(Utils.getParameter(args, "-r", "", "2"));
			generator = new KleinbergSmallWorldGraphGenerator<>((int)sqrt, 1, (int)Math.ceil(sqrt / 100.0), propabilityDistripution, seed);
			break;

		case Constants.TOPOLOGY_WATTS_STROGATZ_GRAPH:
			double propabilityRewiring = Double.valueOf(Utils.getParameter(args, "-p", "", "0.5"));
			int connectToKNN = Integer.valueOf(Utils.getParameter(args, "-k", "", "6")); //must be even
			generator = new WattsStrogatzGraphGenerator<>(numCouples, connectToKNN, propabilityRewiring, seed);
			break;

		default:
			printUsage();
			throw new IllegalArgumentException("Unknown topology: "+topology);
		}
		simulation.setTopology(topology);
		return generator;
	}
	private static OpinionsMatrix createOpinionsMatrix(int numCouples, int numDimensions,
			Graph<PointND, DefaultEdge> graphCC, Graph<PointND, DefaultEdge> graphPP) {
		OpinionsMatrix x = new OpinionsMatrix(numDimensions, numCouples, false);
		Set<PointND> vertexSet;
		PointND[] temp, points;
		vertexSet = graphCC.vertexSet();
		temp = new PointND[vertexSet.size()];
		points = new PointND[temp.length*2];
		vertexSet.toArray(temp);
		System.arraycopy(temp, 0, points, 0, temp.length);
		vertexSet =  graphPP.vertexSet();
		vertexSet.toArray(temp);
		System.arraycopy(temp, 0, points, vertexSet.size(), temp.length);
		x.set(points);
		return x;
	}
	private static EffectMatrix createDynamicsModel(String[] args, Simulation simulation, int numCouples) {
		String dynamicsModelString = Utils.getParameter(args, "-model", null, Defaults.DEFAULT_MODEL);
		EffectMatrix model;
		float phi, beta;
		switch (dynamicsModelString) {
		case Constants.MODEL_INDEPENDENT_CASTOR_AND_POLLUX:
			model = new IndependentCastorAndPolluxEffectMatrix(numCouples);
			break;
		case Constants.MODEL_INDEPENDENT_NETWORKED_CASTOR_AND_POLLUX:
			model = new IndependentNetworkedCastorAndPolluxEffectMatrix(numCouples);
			break;
		case Constants.MODEL_COUPLED_NETWORK_CASTOR_AND_POLLUX_PHI:
			phi = Float.valueOf(Utils.getParameter(args, "-phi", "", ""+Defaults.DEFAULT_PHI));
			model = new CoupledNetworkedCastorAndPolluxPhiEffectMatrix(numCouples,phi);
			break;
		case Constants.MODEL_COUPLED_NETWORK_CASTOR_AND_POLLUX_BETA:
			beta = Float.valueOf(Utils.getParameter(args, "-beta", "", ""+Defaults.DEFAULT_BETA));
			model = new CoupledNetworkedCastorAndPolluxBetaEffectMatrix(numCouples,beta);
			break;
		case Constants.MODEL_FULLY_COUPLED_NETWORKED_CASTOR_AND_POLLUX:
			beta = Float.valueOf(Utils.getParameter(args, "-beta", "", ""+Defaults.DEFAULT_BETA));
			model = new FullyCoupledNetworkedCastorAndPolluxEffectMatrix(numCouples, beta);
			break;
		default:
			throw new IllegalArgumentException("Unknown model parameter: "+dynamicsModelString);
		}
		simulation.setModelNameString(dynamicsModelString);
		return model;
	}

	
	private static void printUsage() {
		String message = ""+
	"JOpinions\n"
	+ "Usage:\n"
	+ "jopinions [parameter ...]"
	+ "Parameters:\n"
	+ "-demo	shows a demo\n"
	+ "\n"
	+ "\n"
	+ "\n"
	+ "\n"
	+ "\n"
	+ ""; //TODO complete later
		System.out.println(message);
	}
	public void doIndependentNetworkedCastorAndPollux() {
		
	}
	
	
	private void demo(String[] args) {
		int dimensions = 3;
		int numCouples=Integer.parseInt(Utils.getParameter(args, "-numCouples", "", "10"));
		Simulation simulation = new Simulation();
		boolean verbose = Boolean.valueOf(Utils.getParameter(args, "-v", "true", ""+simulation.isVerbose()));
		simulation.setVerbose(verbose);

		Graph<PointND, DefaultEdge> gCC = new DefaultDirectedGraph<>(DefaultEdge.class);
		Graph<PointND, DefaultEdge> gPP = new DefaultDirectedGraph<>(DefaultEdge.class);
		@SuppressWarnings("unchecked")
		Graph<PointND, DefaultEdge>[] graphs = (Graph<PointND, DefaultEdge>[]) new Graph[]{gCC, null,null,gPP};
		simulation.setGraphs(graphs);
//		simulation.setD(new IndependentCastorAndPolluxEffectMatrix(pairs));
		EffectMatrix model = createDynamicsModel(args, simulation, numCouples);
		simulation.setD(model);

		OpinionsMatrix x = new OpinionsMatrix(dimensions, numCouples, true);
		float[][] data = new float[2*numCouples][dimensions];

		data[0] = 			new float[] {1,0,0};
		data[numCouples] = 	new float[] {0,1,0};
		float delta = 1.0f / ((data.length/2)-1-1);
		for (int i = 1; i < data.length/2; i++) {
			float alpha = (i-1) * delta;
			//castors
			data[i] = new float[] { ((0.5f * alpha) + (0.5f * (1-alpha))), (0+(0.5f * (1-alpha))), ((0.5f *alpha)+0)};
			//pulloxes
			data[data.length-i] = new float[] { ((0.5f *alpha)+0), ((0.5f * alpha) + (0.5f * (1-alpha))), (0+(0.5f * (1-alpha)))};
		}
		
		x.match(data);
		simulation.setX(x);
		
		List<PointND> cPoints = new ArrayList<>(numCouples);
		List<PointND> pPoints = new ArrayList<>(numCouples);
		
		for (int i = 0; i < numCouples; i++) {
			cPoints.add(x.points[i]);
			pPoints.add(x.points[i+numCouples]);
		}
		Graphs.addAllVertices(gCC, cPoints);
		Graphs.addAllVertices(gPP, pPoints);
		//add self edges
		for (int i = 0; i < numCouples; i++) {
			gCC.addEdge(cPoints.get(i), cPoints.get(i));
			gPP.addEdge(pPoints.get(i), pPoints.get(i));
		}
		Graphs.addOutgoingEdges(gCC,cPoints.get(0), cPoints);
		Graphs.addOutgoingEdges(gPP,pPoints.get(0), pPoints);
		simulation.start();
	}

}
