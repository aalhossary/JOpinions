package sg.edu.ntu.jopinions.control.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.KleinbergSmallWorldGraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.util.SupplierUtil;

import sg.edu.ntu.jopinions.BollobasGraphGenerator;
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
	
	private static Random randomGenerator;
	private static JOpinionsCLI instance=null;
	private static boolean verbose;
	
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
		long seed = Long.valueOf(Utils.getParameter(args, "-seed", "0", ""+System.currentTimeMillis()));
		randomGenerator = new Random(seed);
		boolean demo = Boolean.valueOf(Utils.getParameter(args, "-demo", "true", "false"));
		if(demo) {
			cli.demo(args);
			return;
		}

		String id = Utils.getParameter(args, "-id", "", "");
		String outFolderString = Utils.getParameter(args, "-outFolder", "./", null);
		File ggFile = null, ppFile = null;
		PrintStream xOut = null, DOut = null;
		if (outFolderString != null) {
			File outFolder = new File(outFolderString);
			if ( ! outFolder.exists()) {
				outFolder.mkdirs();
			}
			boolean effectMatrix = Boolean.valueOf(Utils.getParameter(args, "-effectsMatrix", "true", "false"));
			try {
				ggFile = new File(outFolder, String.format("gg-%s.log", id));
				ppFile = new File(outFolder, String.format("pp-%s.log", id));
				File xFile = new File(outFolder, String.format("x-%s.log", id));
				File DFile = new File(outFolder, String.format("D-%s.log", id));
				xOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(xFile), 8 * 1024), false);
				if (effectMatrix) {
					DOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(DFile), 8 * 1024), false);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		Simulation simulation = new Simulation();
		verbose = Boolean.valueOf(Utils.getParameter(args, "-v", "true", "false"));
		simulation.setVerbose(verbose);
		simulation.setPrintStreams(xOut, DOut);
		boolean showGUI = Boolean.valueOf(Utils.getParameter(args, "-showGUI", "true", "false"));
		simulation.setShowGUI(showGUI);
		int numCouples = Integer.valueOf(Utils.getParameter(args, "-numCouples", "-1", "1000"));
		int numDimensions = Integer.valueOf(Utils.getParameter(args, "-dimensions", "", String.valueOf(Defaults.DEFAULT_NUM_DIMENSIONS)));
		
		PointND.setNumDimensions(numDimensions);
		Graph<PointND, DefaultEdge> graphCC/* , graphCP, graphPC */, graphPP;

		//add Topology Random generators
		GraphGenerator<PointND, DefaultEdge, PointND> generator = createTopologyGenerator(args, simulation, numCouples, randomGenerator);

		Float rho = Float.valueOf(Utils.getParameter(args, Constants.PARAM_STUBBORN, "", ""+Defaults.RHO));
		final int MAX_MOBILIZATION_ATTEMPTS = 1000;
		boolean success;
		int attempts = 0;
		do {
			graphCC = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, Constants.CASTOR),SupplierUtil.createDefaultEdgeSupplier(),false);
			generator.generateGraph(graphCC);
			addSelfLoops(graphCC);
			Utils.cacheVerticesDegrees(graphCC);
			success = mobilize(graphCC, rho, randomGenerator);
		}while (! success & ++attempts < MAX_MOBILIZATION_ATTEMPTS); //This is & (not &&) on purpose; to increment attempts.
		if (verbose) {
			System.out.println("Attempted " + attempts + " times to manipulate graph.");
		}
		if(! success) {
			System.err.println("ERROR: Failed to obtain a graph with desired distribution after "+MAX_MOBILIZATION_ATTEMPTS+" attempts. Exiting.");
			System.exit(-3);
		}

		attempts = 0;
		do {
			graphPP = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, Constants.PULLOX),SupplierUtil.createDefaultEdgeSupplier(),false);
			generator.generateGraph(graphPP);
			addSelfLoops(graphPP);
			Utils.cacheVerticesDegrees(graphPP);
			success = mobilize(graphPP, rho, randomGenerator);
		}while (! success & ++attempts < MAX_MOBILIZATION_ATTEMPTS); //This is & (not &&) on purpose; to increment attempts.
		if (verbose) {
			System.out.println("Attempted " + attempts + " times to manipulate graph.");
		}
		if(! success) {
			System.err.println("ERROR: Failed to obtain a graph with desired distribution after "+MAX_MOBILIZATION_ATTEMPTS+" attempts. Exiting.");
			System.exit(-3);
		}
		
		boolean flip = Boolean.parseBoolean(Utils.getParameter(args, "-flip", "true", "false"));
		if (flip) {
			graphCC = new EdgeReversedGraph<PointND, DefaultEdge>(graphCC);
			graphPP = new EdgeReversedGraph<PointND, DefaultEdge>(graphPP);
			Utils.cacheVerticesDegrees(graphCC);
			Utils.cacheVerticesDegrees(graphPP);
		}

		@SuppressWarnings("unchecked")
		Graph<PointND, DefaultEdge>[] graphs = (Graph<PointND, DefaultEdge>[]) new Graph[]{graphCC, null, null,graphPP};
		simulation.setGraphs(graphs);
		
		//get model from parameters
		EffectMatrix model = createDynamicsModel(args, simulation, numCouples);

		simulation.setD(model);

		OpinionsMatrix x = createOpinionsMatrix(numCouples, numDimensions, graphCC, graphPP);
		x.randomize(randomGenerator);
		x.normalize();
		
		//=========== Manage stubborn start======================================
		String[] manageStubborn = Utils.getParameters(args, Constants.PARAM_MANAGE_STUBBORN, (String[])null, new String[]{Constants.NONE});
		if (manageStubborn == null) {
			throw new IllegalArgumentException("If parameter " + Constants.PARAM_MANAGE_STUBBORN + " is introduced, it must be given a value.");
		} else {
			String command = manageStubborn[0];//there is at least {"none"}
			if (command.equals(Constants.POLARIZE_SINGLE)) {
				float nu = Defaults.NU;
				try { nu = Float.valueOf(manageStubborn[1]); } catch (Exception e) {}
				polarizeSingle(graphCC, nu);
				polarizeSingle(graphPP, nu);
			} else if (command.equals(Constants.POLARIZE_COUPLE)) {
				float nu = Defaults.NU;
				try { nu = Float.valueOf(manageStubborn[1]); } catch (Exception e) {}
				polarizeCouple(graphCC, x, nu, randomGenerator);
				polarizeCouple(graphPP, x, nu, randomGenerator);
			} else if (command.equals(Constants.NONE)) {
				//do nothing
			} else {
				throw new IllegalArgumentException("Unknown stubborn management command " + command);
			}
		}
		//=========== Manage stubborn end======================================

		//=========== Output graphs ===========================================
		if (verbose) {
			System.out.println("============GG Graph=============");
			GraphsIO.export(graphCC, System.out);
			System.out.println("============PP Graph=============");
			GraphsIO.export(graphPP, System.out);
		}
		if (ggFile != null && ppFile != null) {
			GraphsIO.export(graphCC, ggFile);
			GraphsIO.export(graphPP, ppFile);
		}
		//=========== Output graphs End ===========================================

		simulation.setX(x);
		
		long stepDelayMillis = (long)(1000 * Float.valueOf(Utils.getParameter(args, "-dt", "", "" + Defaults.DEFAULT_STEP_DELAY_SECS)));
		simulation.setStepDelayMillis(stepDelayMillis);
		simulation.start();
		
	}
	private static void addSelfLoops(Graph<PointND, DefaultEdge> graph) {
		graph.vertexSet().stream().forEach(vertix -> graph.addEdge(vertix, vertix));
	}
	
	private static void polarizeSingle(Graph<PointND, DefaultEdge> graph, float nu) {
		Iterator<PointND> subjectPointsIterator = graph.vertexSet().stream()
				.filter(vertex -> vertex.getInDegree() == 1).iterator();
		while (subjectPointsIterator.hasNext()) {
			PointND point = (PointND) subjectPointsIterator.next();
//			System.out.println(point);
			moveToPole(point, Constants.CASTOR.equals(point.getName()), nu);
			if (verbose) {
				System.out.println(point);
			}
		}
	}

	/**
	 * One potential bug in this implementation is that the pair would go to an
	 * arbitrary pole regardless to the place where they were originally in.
	 * @param graph
	 * @param x
	 * @param nu
	 * @param random
	 */
	private static void polarizeCouple(Graph<PointND, DefaultEdge> graph, OpinionsMatrix x, float nu, Random random) {
		Iterator<PointND> subjectPointsIterator = graph.vertexSet().stream()
				.filter(vertex -> vertex.getInDegree() == 1).iterator();
		int n = x.getN();
		while (subjectPointsIterator.hasNext()) {
			PointND point = (PointND) subjectPointsIterator.next();
			int id = point.getId();
			boolean targetPool = random.nextBoolean();
			final PointND point1 = x.points[id];
			final PointND point2 = x.points[id+n];
			moveToPole(point1, targetPool, nu);
			moveToPole(point2, targetPool, nu);
			if (verbose) {
				System.out.println(point1);
				System.out.println(point2);
				System.out.println();
			}
		}
	}

//	/**
//	 * @param point the point to polarize
//	 * @param firstPool if <code>true</code>, go to Castors pool, otherwise Puloxes pool
//	 * @param nu number [0, 1] where 1 indicates that the two triangular pools touch each other
//	 */
//	private static void moveToPole(PointND point, boolean firstPool, float nu) {
//		nu = nu / 2;//actual nu is until half of the area
//		point.scale(nu);
//		PointND ref;
//		if (firstPool) {
//			ref = new PointND(Defaults.CASTOR+"Ref", new float[] {1, 0, 0}, 0);
//		} else {
//			ref = new PointND(Defaults.PULLOX+"Ref", new float[] {0, 0.5f, 0.5f}, 0);
//		}
//		float[] miniRef = ref.copyX_i();
//		Utils.scaleLine(miniRef, nu);
//		float[] translation = PointND.minusRawData(ref.getX_i(),miniRef);
//		point.matchValues(PointND.plusRawData(point.getX_i(),translation));
//		point.normalize();
//	}
	/**Note that this method <i>modifies</i> the passed in point.
	 * @param point the point to polarize
	 * @param firstPool if <code>true</code>, go to Castors pool, otherwise Puloxes pool
	 * @param nu number [0, 1] where 1 indicates that the two triangular pools touch each other
	 */
	static void moveToPole(PointND point, boolean firstPool, float nu) {
		nu = nu / 2;//actual nu is until half of the area
		point.scale(nu);
		PointND ref;
		if (firstPool) {
			ref = new PointND(Defaults.CASTOR+"Ref", new float[] {1, 0, 0}, 0);
		} else {
			ref = new PointND(Defaults.PULLOX+"Ref", new float[] {0, 0.5f, 0.5f}, 0);
		}
		ref.scale(1.0f-nu);
		point.add(ref);
		point.normalize();
	}	
	private static boolean mobilize(Graph<PointND, DefaultEdge> graph, float rho, Random randomGenerator) {
		final Comparator<DefaultEdge> inDegreeEdgeTargetComparator = new InDegreeEdgeTargetComparator(graph);
		final Comparator<PointND> outDegreeVertixComparator = new OutDegreeVertixComparator();
		final Comparator<PointND> inThenOutDegreeVertixComparator = new InThenOutDegreeVertixComparator();

		int targetFixed = (int) (rho * graph.vertexSet().size());
		final int fixedPointsCount = (int) graph.vertexSet().stream()
				.filter(vertex -> graph.inDegreeOf(vertex) == 1).count(); //only from itself

		if(verbose) {
			System.out.println("Fixedpoints = "+fixedPointsCount+"\tTarget = "+ targetFixed);
		}
		int newFixed = 0, targetNewFixed = 0, newMobile = 0, targetNewMobile = 0;
		if (targetFixed < fixedPointsCount) {// we need to decrease fixed points (mobilize)
			List<PointND> subjectPointsList = graph.vertexSet().stream()
					.filter(point -> point.getInDegree() == 1)
					.sorted(outDegreeVertixComparator.reversed())
					.collect(Collectors.toList());
			targetNewMobile = fixedPointsCount - targetFixed;
			if (subjectPointsList.size() < targetNewMobile) {
				System.err.println("Too much!");
				return false;
			}
			Iterator<PointND> subjectPointsIterator = subjectPointsList.iterator();
			node: while (newMobile < targetNewMobile && subjectPointsIterator.hasNext()) {
				PointND pointND = subjectPointsIterator.next();
				// candidates should have at least one remaining target (other than itself)
				Iterator<DefaultEdge> sortedCandidateEdgesIterator = graph.outgoingEdgesOf(pointND).stream()
						.filter(edge -> graph.inDegreeOf(graph.getEdgeTarget(edge)) > 2)
						.sorted(inDegreeEdgeTargetComparator.reversed())
						.iterator();

				edge: while (sortedCandidateEdgesIterator.hasNext()) {
					DefaultEdge edge = sortedCandidateEdgesIterator.next();
					PointND edgeSource = pointND;
					PointND edgeTarget = graph.getEdgeTarget(edge);
					// ignore "loop" edges
					// repeat check to avoid shrinking a node already shrunk in a previous operation (redundant?)
					if (edgeSource == edgeTarget || edgeTarget.getInDegree() <= 1) {
						continue edge;
					}
					graph.removeEdge(edge);
					graph.addEdge(edgeTarget, edgeSource);
					edgeSource.setInDegree(graph.inDegreeOf(edgeSource));
					edgeSource.setOutDegree(graph.outDegreeOf(edgeSource));
					edgeTarget.setInDegree(graph.inDegreeOf(edgeTarget));
					edgeTarget.setOutDegree(graph.outDegreeOf(edgeTarget));
					newMobile++;
					if (verbose) {
						System.out.println("Mobilized " + pointND);
					}
					continue node;
				}
			}
			if(newMobile == targetNewMobile)
				return true;
		} else {// we need to add more fixed points (remove incoming edges)
			targetNewFixed = targetFixed - fixedPointsCount;

			node: while (newFixed < targetNewFixed) {
//				exclude stubborn points and their direct children
				Set<PointND> excludedPointsSet = new LinkedHashSet<>();
				Set<PointND> stubbornPointsSet = graph.vertexSet().stream()
						.filter(point -> point.getInDegree() == 1).collect(Collectors.toSet());
				for (PointND stubbornPointND : stubbornPointsSet) {
//					excludedPointsSet.add(stubbornPointND); //Redundant
					Set<DefaultEdge> itsOutgoingEdges = graph.outgoingEdgesOf(stubbornPointND);
					for (DefaultEdge defaultEdge : itsOutgoingEdges) {
						excludedPointsSet.add(graph.getEdgeTarget(defaultEdge));
					}
				}
				List<PointND> subjectPointsList = graph.vertexSet().stream()
						.filter(vertex -> !excludedPointsSet.contains(vertex))
						.sorted(inThenOutDegreeVertixComparator).collect(Collectors.toList());
				if (subjectPointsList.size() < targetNewFixed) {
					System.err.println("Too much!");
					return false;
				}
				for (PointND pointND : subjectPointsList) {
					Iterator<DefaultEdge> candidateEdgesIterator = new LinkedHashSet<DefaultEdge>(graph.incomingEdgesOf(pointND)).iterator();
					edge: while (candidateEdgesIterator.hasNext()) {
						DefaultEdge edge = candidateEdgesIterator.next();
						PointND edgeSource = graph.getEdgeSource(edge);
						PointND edgeTarget = pointND;
						// ignore loop edges
						if (edgeSource == edgeTarget) {
							continue edge;
						}
						graph.removeEdge(edge);
						graph.addEdge(edgeTarget, edgeSource);
						edgeSource.setInDegree(graph.inDegreeOf(edgeSource));
						edgeSource.setOutDegree(graph.outDegreeOf(edgeSource));
						edgeTarget.setInDegree(graph.inDegreeOf(edgeTarget));
						edgeTarget.setOutDegree(graph.outDegreeOf(edgeTarget));
						if (pointND.getInDegree() == 1) {
							newFixed++;
							if (verbose) {
								System.out.println("Fixed " + pointND);
							}
							continue node;
						}
					}
				}
			}
			if(newFixed == targetNewFixed)
				return true;
		}
		if (verbose) {
			System.out.println("total new fixed points = "+newFixed+" of "+targetNewFixed);
			System.out.println("total new mobile points = "+newMobile+" of "+targetNewMobile);
		}
		return false;
	}

	private static GraphGenerator<PointND, DefaultEdge, PointND> createTopologyGenerator(String[] args,
			Simulation simulation, int numCouples, Random random) {
		GraphGenerator<PointND, DefaultEdge, PointND> generator;
		String topology = Utils.getParameter(args, "-topology", null, Defaults.DEFAULT_TOPOLOGY);
		switch (topology) {
		case Constants.TOPOLOGY_BARABASI_ALBERT_GRAPH:
			int m0	= Integer.valueOf(Utils.getParameter(args, "-m0", "", "20"));
			int m 	= Integer.valueOf(Utils.getParameter(args, "-m",  "", "5"));
			generator = new BarabasiAlbertGraphGenerator<>(m0, m, numCouples, random);
			break;

		case Constants.TOPOLOGY_ERDOS_RENYI_GNP_RANDOM_GRAPH:
			int numEdges = Integer.valueOf(Utils.getParameter(args, "-edges", "0", "-1"));
			if(numEdges == -1) {
//				numEdges = numCouples * 3;
				numEdges = (numCouples / 20) * (numCouples-1);
			}
			//false, false are the default values for the last two parameters
			generator = new GnmRandomGraphGenerator<>(numCouples, numEdges, random, false, false);
			break;

		case Constants.TOPOLOGY_KLEINBERG_SMALL_WORLD_GRAPH:
			double sqrt = Math.sqrt(numCouples);
			if (sqrt != (int)sqrt) {
				throw new RuntimeException("numCouples must have a square root for Kleinberg model");
			}
			int propabilityDistripution = Integer.valueOf(Utils.getParameter(args, "-r", "", "2"));
			generator = new KleinbergSmallWorldGraphGenerator<>((int)sqrt, 1, (int)Math.ceil(sqrt / 100.0), propabilityDistripution, random);
			break;

		case Constants.TOPOLOGY_WATTS_STROGATZ_GRAPH:
			double propabilityRewiring = Double.valueOf(Utils.getParameter(args, "-p", "", "0.5"));
			int connectToKNN = Integer.valueOf(Utils.getParameter(args, "-k", "", "6")); //must be even
			//I don't know what is addInsteadOfRewire, but false is the default behavior
			generator = new WattsStrogatzGraphGenerator<>(numCouples, connectToKNN, propabilityRewiring, false, random);
			break;

		case Constants.BOLLOBAS_DIRECTED_SCALEFREE_GRAPH:
			float alpha = Float.valueOf(Utils.getParameter(args, "-alpha", "", "0.3333"));
			float beta = Float.valueOf(Utils.getParameter(args, "-beta", "", "0.3333"));
			float dIn = Float.valueOf(Utils.getParameter(args, "-dIn", "", "1"));
			float dOut = Float.valueOf(Utils.getParameter(args, "-dOut", "", "1"));
			generator = new BollobasGraphGenerator<>(alpha, beta, dIn, dOut, numCouples, random);
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
	
	private void demo(String[] args) {
		int dimensions = 3;
		int numCouples=Integer.parseInt(Utils.getParameter(args, "-numCouples", "", "10"));
		Simulation simulation = new Simulation();
		verbose = Boolean.valueOf(Utils.getParameter(args, "-v", "true", ""+simulation.isVerbose()));
		simulation.setVerbose(verbose);
		boolean showGUI = Boolean.valueOf(Utils.getParameter(args, "-showGUI", "true", "false"));
		simulation.setShowGUI(showGUI);
		long stepDelayMillis = (long)(1000 * Float.valueOf(Utils.getParameter(args, "-dt", "", "" + Defaults.DEFAULT_STEP_DELAY_SECS)));
		simulation.setStepDelayMillis(stepDelayMillis);

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
			data[data.length - i] = new float[] { ((0.5f * alpha) + 0), ((0.5f * alpha) + (0.5f * (1 - alpha))), ( 0 + (0.5f * (1 - alpha)))};
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

	static class InDegreeEdgeTargetComparator implements Comparator<DefaultEdge> {
		Graph<PointND, DefaultEdge> graph;
		public InDegreeEdgeTargetComparator(Graph<PointND, DefaultEdge> graph) {
			this.graph = graph;
		}
		@Override
		public int compare(DefaultEdge o1, DefaultEdge o2) {
			return graph.inDegreeOf(graph.getEdgeTarget(o1)) - graph.inDegreeOf(graph.getEdgeTarget(o2));
		}
	}
	static class OutDegreeVertixComparator implements Comparator<PointND> {
		@Override
		public int compare(PointND o1, PointND o2) {
			return o1.getOutDegree() - o2.getOutDegree();
		}
	}
	static class InThenOutDegreeVertixComparator implements Comparator<PointND> {
		@Override
		public int compare(PointND o1, PointND o2) {
			int inDegreeDifference = o1.getInDegree() - o2.getInDegree();
			return inDegreeDifference != 0? inDegreeDifference : o1.getOutDegree() - o2.getOutDegree();
		}
	}
}
