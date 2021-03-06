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
import org.jgrapht.generate.DirectedScaleFreeGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.KleinbergSmallWorldGraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
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
		File ccFile = null, ppFile = null, dcpFile = null;
		PrintStream xOut = null, DOut = null;
		if (outFolderString != null) {
			File outFolder = new File(outFolderString);
			if ( ! outFolder.exists()) {
				outFolder.mkdirs();
			}
			boolean effectMatrix = Boolean.valueOf(Utils.getParameter(args, "-effectsMatrix", "true", "false"));
			try {
				ccFile = new File(outFolder, String.format(Constants.PATTERN_LOG_FILE_GRAPH_CC, id));
				ppFile = new File(outFolder, String.format(Constants.PATTERN_LOG_FILE_GRAPH_PP, id));
				dcpFile = new File(outFolder, String.format(Constants.PATTERN_LOG_FILE_DETAILS_CP, id));
				File xFile = new File(outFolder, String.format(Constants.PATTERN_LOG_FILE_X, id));
				File DFile = new File(outFolder, String.format(Constants.PATTERN_LOG_FILE_D, id));
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
		PointND.setDefaultEgo(Float.valueOf(Utils.getParameter(args, "-ego", null, ""+Defaults.DEFAULT_EGO)));
		Graph<PointND, DefaultEdge> graphCC/* , graphCP, graphPC */, graphPP;

		//add Topology Random generators
//		randomGenerator.setSeed(seed); // no need to set the seed because this is the first call to randomGenerator
		GraphGenerator<PointND, DefaultEdge, PointND> generator = createTopologyGenerator(args, simulation, numCouples, randomGenerator);

//		Float rho = Float.valueOf(Utils.getParameter(args, Constants.PARAM_STUBBORN, "", ""+Defaults.RHO));

//		//==================== multiple trials of mobilization start ====================================
//		final int MAX_MOBILIZATION_ATTEMPTS = 1000;
//		boolean success;
//		int attempts = 0;
//		do {
//			graphCC = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, Constants.CASTOR),SupplierUtil.createDefaultEdgeSupplier(),false);
//			generator.generateGraph(graphCC);
//			addSelfLoops(graphCC);
//			Utils.cacheVerticesDegrees(graphCC);
//			success = mobilize(graphCC, rho, randomGenerator);
//		}while (! success & ++attempts < MAX_MOBILIZATION_ATTEMPTS); //This is & (not &&) on purpose; to increment attempts.
//		if (verbose) {
//			System.out.println("Attempted " + attempts + " times to manipulate graph.");
//		}
//		if(! success) {
//			System.err.println("ERROR: Failed to obtain a graph with desired distribution after "+MAX_MOBILIZATION_ATTEMPTS+" attempts. Exiting.");
//			System.exit(-3);
//		}
//
//		attempts = 0;
//		do {
//			graphPP = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, Constants.PULLOX),SupplierUtil.createDefaultEdgeSupplier(),false);
//			generator.generateGraph(graphPP);
//			addSelfLoops(graphPP);
//			Utils.cacheVerticesDegrees(graphPP);
//			success = mobilize(graphPP, rho, randomGenerator);
//		}while (! success & ++attempts < MAX_MOBILIZATION_ATTEMPTS); //This is & (not &&) on purpose; to increment attempts.
//		if (verbose) {
//			System.out.println("Attempted " + attempts + " times to manipulate graph.");
//		}
//		if(! success) {
//			System.err.println("ERROR: Failed to obtain a graph with desired distribution after "+ MAX_MOBILIZATION_ATTEMPTS+" attempts. Exiting.");
//			System.exit(-3);
//		}
//		//==================== multiple trials of mobilization end ====================================
//		//==================== Single trial of graph manipulation start ====================================
		graphCC = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, Constants.CASTOR), SupplierUtil.createDefaultEdgeSupplier(), false);
		graphPP = new DefaultDirectedGraph<>(new PointNDSupplier(numDimensions, Constants.PULLOX), SupplierUtil.createDefaultEdgeSupplier(), false);
		generator.generateGraph(graphCC);
		generator.generateGraph(graphPP);
		//No need to call cacheVerticesDegrees because addSelfLoops does the caching already
//		Utils.cacheVerticesDegrees(graphCC);
//		Utils.cacheVerticesDegrees(graphPP);
		addSelfLoops(graphCC);
		addSelfLoops(graphPP);
//		randomGenerator.setSeed(seed);
//		if(! fixOnly(graphCC, rho, randomGenerator))
//			throw new RuntimeException("Couldn't fix enough points in graph CC");
//		if(! fixOnly(graphPP, rho, randomGenerator))
//			throw new RuntimeException("Couldn't fix enough points in graph PP");
//		//==================== Single trial of graph manipulation end ====================================
		
		
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
		randomGenerator.setSeed(seed);
		x.randomize(randomGenerator);
		x.normalize();
		
		//=========== Manage stubborn start======================================
		float beta = Float.valueOf(Utils.getParameter(args, "-beta", "", ""+Defaults.DEFAULT_BETA));
		randomGenerator.setSeed(seed);
		Set<Integer>[] stubborns = selectStubborns(graphs);
		Set<Integer> egostics[] = null;
		//ONLY if parameter given
		Float egoRatio = Float.valueOf(Utils.getParameter(args, Constants.PARAM_EGO_RATIO, "", "-1"));
		if(egoRatio >= 0) {
			egostics = selectEgostics(graphs, stubborns, egoRatio, randomGenerator);
			float k = 5; //TODO to be parameterized later
			int idOffset = 0;
			for (int i = 0; i < egostics.length; i++) {
				final Set<Integer> ids = egostics[i];
				if(ids == null)
					continue;
				for (Integer egosticId : ids) {
					final PointND pointND = x.points[egosticId + idOffset];
					//must have in-degree cached already
					int inDegree = pointND.getInDegree();
					pointND.setEgo(k * (2 * inDegree + beta * (1 - (2 * inDegree))) /* / (1-beta) */);
				}
				idOffset += numCouples;
			}
		}
		String[] manageStubborn = Utils.getParameters(args, Constants.PARAM_MANAGE_STUBBORN, (String[])null, new String[]{Constants.NONE});
		randomGenerator.setSeed(seed);
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
//				polarizeCouple(graphCC, x, nu, randomGenerator);
//				polarizeCouple(graphPP, x, nu, randomGenerator);
				polarizeCouple(egostics, x, nu, randomGenerator);
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
		if (ccFile != null && ppFile != null && dcpFile != null) {
			GraphsIO.export(graphCC, ccFile);
			GraphsIO.export(graphPP, ppFile);
			GraphsIO.exportDetails(graphs, dcpFile);
		}
		//=========== Output graphs End ===========================================

		simulation.setX(x);
		
		long stepDelayMillis = (long)(1000 * Float.valueOf(Utils.getParameter(args, "-dt", "", "" + Defaults.DEFAULT_STEP_DELAY_SECS)));
		simulation.setStepDelayMillis(stepDelayMillis);
		simulation.start();
		
	}
	private static void addSelfLoops(Graph<PointND, DefaultEdge> graph) {
		graph.vertexSet().stream().forEach(vertex -> {	graph.addEdge(vertex, vertex); 
														vertex.setInDegree(graph.inDegreeOf(vertex));
														vertex.setOutDegree(graph.outDegreeOf(vertex));
													});
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
	
	private static Set<Integer>[] selectStubborns(Graph<PointND, DefaultEdge>[] graphs){
		@SuppressWarnings("unchecked")
		Set<Integer>[] ret = (Set<Integer>[]) new Set[graphs.length]; 
		//add intervals from both graphs together, because there may be double-fixed intervals.
		for (int i = 0; i < ret.length; i++) {
			Graph<PointND, DefaultEdge> graph = graphs[i];
			Set<Integer> stubbornIdsSet = new LinkedHashSet<>();
			if(graph == null)
				continue;
			graph.vertexSet().stream()
			.filter(vertex -> vertex.getInDegree() == 1)
			.forEach(vertex -> stubbornIdsSet.add(vertex.getId()));
			ret[i] = stubbornIdsSet;
		}
		return ret;
	}
	private static Set<Integer>[] selectEgostics(Graph<PointND, DefaultEdge>[] graphs, Set<Integer>[] stubbornIdsSets, float fixedPercent, Random random){
		int graphVertexSetSize = 0, targetNumFixed = 0;

		Set<Integer>[] ret = (Set<Integer>[]) new Set[graphs.length]; 
		for (int i = 0; i < graphs.length; i++) {
			Graph<PointND, DefaultEdge> graph = graphs[i];
			if(graph == null)
				continue;
			Set<Integer> stubbornIdsSet = stubbornIdsSets[i];
			graphVertexSetSize = graph.vertexSet().size();
			targetNumFixed = (int) (graphVertexSetSize * fixedPercent);
			Set<Integer> candidates = new LinkedHashSet<>();

			if(stubbornIdsSet.size() <= targetNumFixed) {
				candidates.addAll(stubbornIdsSet);
			} else {
				Integer[] ids = new Integer[stubbornIdsSet.size()];
				stubbornIdsSet.toArray(ids);
				while (candidates.size() < targetNumFixed) {
					candidates.add(ids[random.nextInt(ids.length)]);
				}
			}

			Set<PointND> vertexSet = graph.vertexSet();
			PointND[] AllVertices = new PointND[vertexSet.size()];
			vertexSet.toArray(AllVertices);
			while(candidates.size() < targetNumFixed) {
				candidates.add(AllVertices[random.nextInt(AllVertices.length)].getId());
			}
			ret[i] = candidates;
		}
		return ret;
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

	/**
	 * One potential bug in this implementation is that the pair would go to an
	 * arbitrary pole regardless to the place where they were originally in.
	 * @param x
	 * @param nu
	 * @param random
	 */
	private static void polarizeCouple(Set<Integer>[] egostics, OpinionsMatrix x, float nu, Random random) {
		int n = x.getN();
		for (int i = 0; i < egostics.length; i++) {
			Set<Integer> groupIdsSet = egostics[i];
			if(groupIdsSet == null)
				continue;
			Iterator<Integer> subjectPointsIdsIterator = groupIdsSet.iterator();
			while (subjectPointsIdsIterator.hasNext()) {
				int id = subjectPointsIdsIterator.next();
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

	private static boolean fixOnly(Graph<PointND, DefaultEdge> graph, float rho, Random randomGenerator) {
		int targetFixed = (int) (rho * graph.vertexSet().size());
		final int fixedPointsCount = (int) graph.vertexSet().stream()
				.filter(vertex -> graph.inDegreeOf(vertex) == 1).count(); //only from itself
		
		if(verbose) {
			System.out.println("Fixedpoints = "+fixedPointsCount+"\tTarget = "+ targetFixed);
		}
		int newFixed = 0, targetNewFixed = 0;
		if (targetFixed < fixedPointsCount) {// we need to decrease fixed points (mobilize)
			//We could not
			return false;
		} else {// we need to add more fixed points (remove incoming edges)
			targetNewFixed = targetFixed - fixedPointsCount;
			Set<PointND> stubbornPointsSet = graph.vertexSet().stream()
					.filter(point -> point.getInDegree() == 1).collect(Collectors.toSet());
			List<PointND> subjectPointsList = graph.vertexSet().stream()
					.filter(vertex -> !stubbornPointsSet.contains(vertex)).collect(Collectors.toList());
			if (subjectPointsList.size() < targetNewFixed) {
				System.err.println("Too much!");
				return false;
			}
			node: while (subjectPointsList.size() > 0 && newFixed < targetNewFixed) {
				PointND pointND = subjectPointsList.get(randomGenerator.nextInt(subjectPointsList.size()));
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

					edgeSource.setOutDegree(graph.outDegreeOf(edgeSource));
					edgeTarget.setInDegree(graph.inDegreeOf(edgeTarget));
					if (pointND.getInDegree() == 1) {
						subjectPointsList.remove(pointND);
						stubbornPointsSet.add(pointND);
						newFixed++;
						if (verbose) {
							System.out.println("Fixed " + pointND);
						}
						continue node;
					}
				}
			}
			if (verbose) {
				System.out.println("total new fixed points = "+newFixed+" of "+targetNewFixed);
			}
			return(newFixed == targetNewFixed);
		}
	}
	
	private static GraphGenerator<PointND, DefaultEdge, PointND> createTopologyGenerator(String[] args,
			Simulation simulation, int numCouples, Random random) {
		GraphGenerator<PointND, DefaultEdge, PointND> generator;
//		String topology = Utils.getParameter(args, "-topology", null, Defaults.DEFAULT_TOPOLOGY);
		String[] topologyAndParams = Utils.getParameters(args, "-topology", null, new String[] {Defaults.DEFAULT_TOPOLOGY});
		String topology = topologyAndParams[0];
		String[] topologyParams = new String[topologyAndParams.length - 1];
		System.arraycopy(topologyAndParams, 1, topologyParams, 0, topologyParams.length);
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

		case Constants.TOPOLOGY_BOLLOBAS_DIRECTED_SCALEFREE_GRAPH:
			float alpha = topologyParams.length >= 1? Float.valueOf(topologyParams[0]) : 0.3333f;
			float gamma = topologyParams.length >= 2? Float.valueOf(topologyParams[1]) : 0.3333f;
			float deltaIn = topologyParams.length >= 3? Float.valueOf(topologyParams[2]) : 1;
			float deltaOut = topologyParams.length >= 4? Float.valueOf(topologyParams[3]) : 1;
//			generator = new DirectedScaleFreeGraphGenerator<>(alpha, gamma, deltaIn, deltaOut, -1, numCouples, random);
			generator = new DirectedScaleFreeGraphGenerator<>(alpha, gamma, deltaIn, deltaOut, -1, numCouples, random, false, false);
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
