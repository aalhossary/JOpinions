/**
 * 
 */
package sg.edu.ntu.voting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.ImportException;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.Defaults;
import sg.edu.ntu.jopinions.control.cli.GraphsIO;
import sg.edu.ntu.jopinions.control.cli.Parser;
import sg.edu.ntu.jopinions.models.FullyCoupledNetworkedCastorAndPolluxEffectMatrix;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.Utils;

/**
 * @author Amr
 *
 */
public class DistortionCalculator {

	private static final int NUM_SEEDS = 50;

	/**
	 * First dimension is candidates id.
	 * Second dimension is Voters id.
	 * Third dimension is step id.
	 * Note that the first dimension range is the the full list of number of
	 * segments, because any segment can be a candidate.
	 */
	float[][][] allDistances = null; 
	
	public static final int FULL_FIXED  = 0;
	public static final int HALF_MOBILE = 1;
	public static final int FULL_MOBILE = 2;
	
	
	/**NOT yet set automatically*/
	private static final int d_3 = 3;
	File inputFile = null;
	File outputFile = null;

	private float[][][] states;

	private PointND[] allCastorPointNDs;
	private PointND[] allPulloxPointNDs;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inFolderString = Utils.getParameter(args, Constants.PARAM_IN_FOLDER, null, null);
		String outFolderString = Utils.getParameter(args, Constants.PARAM_OUT_FOLDER, null, "./");
		File inFolder = new File(inFolderString);
		String id = Utils.getParameter(args, "-id", null, inFolder.getName());
//		String id = inFolder.getName();
		File outFile = new File(outFolderString,String.format("distortion-%s.csv",id));
		File summaryFile = new File(outFolderString,String.format("distortionavr-%s.csv",id));

		//merge the two param sources, keeping the priority to the sacred command line arguments
		String[] id2Args = Utils.id2Args(id);
		String[] temp = args;
		args= new String[temp.length+id2Args.length];
		System.arraycopy(temp, 0, args, 0, temp.length);
		System.arraycopy(id2Args, 0, args, temp.length, id2Args.length);
		
		new DistortionCalculator().calculate(inFolder, outFile, summaryFile, args);
	}

	private void calculate(File inFolder, File outFile, File summaryFile, String[] args) {
		PrintStream out = null, summary = null;
		try {
			out = new PrintStream(outFile);
			summary = new PrintStream(summaryFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (out == null || summary == null) {
			System.exit(-5);//any nonzero number
		}
		//		String floatAndDelimeter = Defaults.OUTPUT_FORMAT+"\t";
		String floatAndDelimeter = "%8.5E\t";
		//		out.format("step\tid\tl\ts\ts/l\tl/s\t(s+m)/(l/m)\tH(s,l)-H(s,m,l)\th(s,l)\tH(s,m,l)\tH(s,l)\tH((s/(s+m+l)),(l/(s+m+l)))\tH((s/m),(l/m))\n");

		final int n = Integer.valueOf(Utils.getParameter(args, "-numCouples", "-1", "-1"));
		allDistances = new float[n][n][];

		//		File logFile = new File(inFolder, String.format("log-%s.log",id));
		String id = Utils.getParameter(args, "-id", null, inFolder.getName());
		File fileCC = new File(inFolder, String.format(Defaults.PATTERN_LOG_FILE_GRAPH_CC, id));
		File filePP = new File(inFolder, String.format(Defaults.PATTERN_LOG_FILE_GRAPH_PP, id));
		File fileDCP = new File(inFolder, String.format(Defaults.PATTERN_LOG_FILE_DETAILS_CP, id));
		File xFile  = new File(inFolder, String.format(Defaults.PATTERN_LOG_FILE_X, id));

		PointND.PointNDSupplier pointNDSupplier = new PointND.PointNDSupplier(d_3, Constants.CASTOR);
		DefaultDirectedGraph<PointND, DefaultEdge> graphCC = new DefaultDirectedGraph<>(pointNDSupplier, null, false);
		pointNDSupplier = new PointND.PointNDSupplier(d_3, Constants.PULLOX);
		DefaultDirectedGraph<PointND, DefaultEdge> graphPP = new DefaultDirectedGraph<>(pointNDSupplier, null, false);
		Set<Integer> allPossibleFixedPointsIds = null;
		try {
			GraphsIO.importGraph(Constants.CASTOR, d_3, graphCC, fileCC);
			GraphsIO.importGraph(Constants.PULLOX, d_3, graphPP, filePP);
			final float defaultEgo = Float.parseFloat(Utils.getParameter(args, "-ego", "-1", ""+Defaults.DEFAULT_EGO));
			allPossibleFixedPointsIds = findFixedIDs(fileDCP, defaultEgo);
		} catch (ImportException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		final PointND[] dummyPointNDArray = new PointND[] {};
		allCastorPointNDs = graphCC.vertexSet().toArray(dummyPointNDArray);
		allPulloxPointNDs = graphPP.vertexSet().toArray(dummyPointNDArray);
		//check for consistency
		for (int i = 0; i < allPulloxPointNDs.length; i++) {
			PointND pointC = allCastorPointNDs[i];
			PointND pointP = allPulloxPointNDs[i];
			//    		System.out.println("PointND ID = " + pointC.getId());
			if (pointC.getId() != pointP.getId()) {
				throw new RuntimeException("points are not corresponding: "+ pointC + ", " + pointP);
			}
		}
		Utils.cacheVerticesDegrees(graphCC);
		Utils.cacheVerticesDegrees(graphPP);

		//find stubborn and mobile points
//		ArrayList<Integer> allPossibleFullFixedCandidatesIDsArrayList = new ArrayList<>();
//		ArrayList<Integer> allPossibleHalfFixedCandidatesIDsArrayList = new ArrayList<>();
//		ArrayList<Integer> allPossible_________CandidatesIDsArrayList = new ArrayList<>();
		ArrayList<Integer> allFixed_IdsArrayList = new ArrayList<>(allPossibleFixedPointsIds);
		ArrayList<Integer> allMobileIdsArrayList = new ArrayList<>();
		ArrayList<Integer> all______IdsArrayList = new ArrayList<>();

		for (int i = 0; i < allPulloxPointNDs.length; i++) {
			final int pointId = allPulloxPointNDs[i].getId();
			all______IdsArrayList.add(pointId);
			if(! allPossibleFixedPointsIds.contains(pointId)) {
				allMobileIdsArrayList.add(pointId);
			}
		}
//		final int ffCandidatesSize = allFixed_IdsArrayList.size();
////		System.out.format("Num of FF Candidates = %d, Num combinations of 7 = %d, Num combinations of 8 = %d, Num combinations of 9 = %d.\n", ffCandidatesSize, Helper.nCr(ffCandidatesSize, 7), Helper.nCr(ffCandidatesSize, 8), Helper.nCr(ffCandidatesSize, 9));
//		if (ffCandidatesSize < 13) {
//			System.err.format("%s has only %d fully fixed candidates. Not processed.\n", id, ffCandidatesSize);
//			return;
//		}

//		System.out.println("starting Random generation");
		final String seedString = Utils.getParameter(args, "-seed", "Wrong text", null);
		Random random = seedString != null ? new Random(Long.parseLong(seedString)) : new Random();
		Helper<Integer> helper = new Helper<>();
		int targetNumCandidates;
		targetNumCandidates = 3;
		if(targetNumCandidates > allFixed_IdsArrayList.size()) {
			System.err.format("%s has only %d fully fixed candidates. Not processed.\n", id, allFixed_IdsArrayList.size());
			return;
		}
		List<List<Integer>> fiftyListsOfCombinationsOf3AllFromFixed		= helper.giveMeXDifferentCombinations(allFixed_IdsArrayList, targetNumCandidates, NUM_SEEDS, random);
		List<List<Integer>> fiftyListsOfCombinationsOf3HalfFromFixed	= new ArrayList<>(NUM_SEEDS);
		List<List<Integer>> fiftyListsOfCombinationsOf3NoneFromFixed	= helper.giveMeXDifferentCombinations(allMobileIdsArrayList, targetNumCandidates, NUM_SEEDS, random);
		for (int i = 0; i < NUM_SEEDS; i++) {
			int halfTargetNumCandidates = Math.round(1.0f * targetNumCandidates / 2.0f);
			List<List<Integer>> firstHalf = helper.giveMeXDifferentCombinations(allFixed_IdsArrayList, halfTargetNumCandidates, 1, random);
			List<List<Integer>> secondHalf = helper.giveMeXDifferentCombinations(allMobileIdsArrayList, targetNumCandidates - halfTargetNumCandidates, 1, random);
			List<Integer> oneListOfCombinationsOf3From50_Fixed = firstHalf.get(0);
			oneListOfCombinationsOf3From50_Fixed.addAll(secondHalf.get(0));
			fiftyListsOfCombinationsOf3HalfFromFixed.add(oneListOfCombinationsOf3From50_Fixed);
		}

		targetNumCandidates = 6;
		if(targetNumCandidates > allFixed_IdsArrayList.size()) {
			System.err.format("%s has only %d fully fixed candidates. Not processed.\n", id, allFixed_IdsArrayList.size());
			return;
		}
		List<List<Integer>> fiftyListsOfCombinationsOf6AllFromFixed		= helper.giveMeXDifferentCombinations(allFixed_IdsArrayList, targetNumCandidates, NUM_SEEDS, random);
		List<List<Integer>> fiftyListsOfCombinationsOf6HalfFromFixed	= new ArrayList<>(NUM_SEEDS);
		List<List<Integer>> fiftyListsOfCombinationsOf6NoneFromFixed	= helper.giveMeXDifferentCombinations(allMobileIdsArrayList, targetNumCandidates, NUM_SEEDS, random);
		for (int i = 0; i < NUM_SEEDS; i++) {
			int halfTargetNumCandidates = Math.round(1.0f * targetNumCandidates / 2.0f);
			List<List<Integer>> firstHalf = helper.giveMeXDifferentCombinations(allFixed_IdsArrayList, halfTargetNumCandidates, 1, random);
			List<List<Integer>> secondHalf = helper.giveMeXDifferentCombinations(allMobileIdsArrayList, targetNumCandidates - halfTargetNumCandidates, 1, random);
			List<Integer> oneListOfCombinationsOf6From50_Fixed = firstHalf.get(0);
			oneListOfCombinationsOf6From50_Fixed.addAll(secondHalf.get(0));
			fiftyListsOfCombinationsOf6HalfFromFixed.add(oneListOfCombinationsOf6From50_Fixed);
		}


		List<List<List<Integer>>> listOfFiftyListsOfCandidatesIdLists = new ArrayList<>();
		listOfFiftyListsOfCandidatesIdLists.add(fiftyListsOfCombinationsOf3AllFromFixed);
		listOfFiftyListsOfCandidatesIdLists.add(fiftyListsOfCombinationsOf3HalfFromFixed);
		listOfFiftyListsOfCandidatesIdLists.add(fiftyListsOfCombinationsOf3NoneFromFixed);
		listOfFiftyListsOfCandidatesIdLists.add(fiftyListsOfCombinationsOf6AllFromFixed);
		listOfFiftyListsOfCandidatesIdLists.add(fiftyListsOfCombinationsOf6HalfFromFixed);
		listOfFiftyListsOfCandidatesIdLists.add(fiftyListsOfCombinationsOf6NoneFromFixed);


		HashSet<Integer> allAlreadySelectedCandidatesIDs = new HashSet<>();
		for (List<List<Integer>> fiftyListsOfCandidatesIds : listOfFiftyListsOfCandidatesIdLists) {
			for (List<Integer> listOfCandidateIds : fiftyListsOfCandidatesIds) {
				allAlreadySelectedCandidatesIDs.addAll(listOfCandidateIds);
			}
		}
		
		List<Pair<List<Integer>>> listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists = new ArrayList<>();
		for (List<List<Integer>> fiftyListsOfCandidatesIds : listOfFiftyListsOfCandidatesIdLists) {
			for (List<Integer> iterationListOfCandidateIds : fiftyListsOfCandidatesIds) {
				List<Integer> iterationListOfVoterIds = new ArrayList<>(all______IdsArrayList);
				iterationListOfVoterIds.removeAll(iterationListOfCandidateIds);
				listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists.add(new Pair<List<Integer>>(iterationListOfCandidateIds, iterationListOfVoterIds));
			}
		}

		Parser parser = new Parser(n, d_3, xFile);
		states = parser.parse();
		System.out.println("finished reading input files");
		
		UtilityFunction[] utilityFunctions = new UtilityFunction[] {new ExponentialBordaUtility(), new PluralityUtility(), new VetoUtility()};
		TieBreakingRule tieBreakingRule = new LexicographicTieBreakingRule();
		VotingRule votingRule = new PositionalScoringRule();

		
		final float[][][] states = this.states;
		final float[][][] allDistances = this.allDistances;
		boolean[][] toCalculateDist = new boolean[n][n];
		float alpha, oneMinusAlpha;
		float v1v1DotProduct, v1v2DotProduct;
		float[] vectorV1, vectorV2;
		PointND yC, yP;
		float yCij_cj, yPij_pj;
		for (Integer candidateIdInteger : allAlreadySelectedCandidatesIDs) {
			int candidateIndex = candidateIdInteger;
			for (int stepIndex = 0; stepIndex < states.length; stepIndex++) {
				final float[][] currentState = states[stepIndex];
				if (currentState == null) {
					//Not there. Continue.
					continue;
				}

				//calculate Candidate vectors
				PointND candidateC = allCastorPointNDs[candidateIndex];
				PointND candidateP = allPulloxPointNDs[candidateIndex];
				candidateC.match(currentState[candidateIndex]);
				candidateP.match(currentState[candidateIndex+n]);
				// Shall I change the next 2 names?
				PointND pointCi= candidateC;
				PointND pointPi= candidateP;

				vectorV1 = pointCi.minus(pointPi);
				//equals |v1|^2
				v1v1DotProduct = PointND.dotProductRawData(vectorV1, vectorV1);

				for (int voterIndex = 0; voterIndex < n; voterIndex++) {
					if (voterIndex == candidateIndex) {
						continue;
					}
					float[] distances;
					if(stepIndex == 0) {
						distances = new float[states.length];
						allDistances[candidateIndex][voterIndex] = allDistances[voterIndex][candidateIndex] = distances;//because the candidate may be a voter some day
						toCalculateDist[candidateIndex][voterIndex] = toCalculateDist[voterIndex][candidateIndex] = true;
					}else {
						if(! toCalculateDist[candidateIndex][voterIndex])
							continue;
						distances = allDistances[candidateIndex][voterIndex];
					}

					PointND voterC = allCastorPointNDs[voterIndex], voterP = allPulloxPointNDs[voterIndex];
					voterC.match(currentState[voterIndex]);
					voterP.match(currentState[voterIndex+n]);
					/**The names are kept <b>renamed</b> for compatibility with prev code in {@link AnxietyCalculator}.*/
					PointND pointCj= voterC;
					PointND pointPj= voterP;

					// =============== calculate distances ==========================
					float distance;
					if(PointND.doIntersect(candidateC, candidateP, voterC, voterP)) {
						distance = 0;
					}else {
						//calculate minimal distance between candidate and voter line segments
						//effect of pair (Ci, Pi) on point Cj
						vectorV2 = pointCj.minus(pointPi);
						//the dot product is |v1| * |v2| * cos (theta)
						v1v2DotProduct = PointND.dotProductRawData(vectorV1, vectorV2);
						if (v1v2DotProduct <= 0) {
							//point Cj projection is before or on Pi.
							alpha = 0; oneMinusAlpha = 1;
							yC = pointPi;
						} else if(v1v2DotProduct >= v1v1DotProduct){
							//in the line above, notice that v1v1DotProduct actually equals vectorV1LenSqr |v1| * |v1| * cos(0)

							//point Cj projection is on or after Ci.
							alpha = 1; oneMinusAlpha = 0;
							yC = pointCi;
						} else {
							alpha = v1v2DotProduct / v1v1DotProduct;
							oneMinusAlpha = 1 - alpha;
							yC = new PointND("yCj", pointCi.copyX_i(), voterIndex).scale(alpha);
							yC.add(new PointND("Temp", pointPi.copyX_i(), -1).scale(oneMinusAlpha));
						}
						yCij_cj = yC.getDist(pointCj);

						//-----------------------------------------------------------

						//effect of pair (Ci, Pi) on point Pj
						vectorV2 = pointPj.minus(pointPi);
						//the dot product is |v1| * |v2| * cos (theta)
						v1v2DotProduct = PointND.dotProductRawData(vectorV1, vectorV2);
						if (v1v2DotProduct <= 0) {
							//point Pj projection is before or on Pi.
							alpha = 1; oneMinusAlpha = 0;
							yP = pointPi;
						} else if(v1v2DotProduct >= v1v1DotProduct){
							//in the line above, notice that v1v1DotProduct actually equals vectorV1LenSqr |v1| * |v1| * cos(0)

							//point Pj projection is on or after Ci.
							alpha = 0; oneMinusAlpha = 1;
							yP = pointCi;
						} else {
							oneMinusAlpha = v1v2DotProduct / v1v1DotProduct;
							alpha = 1 - oneMinusAlpha;
							yP = new PointND("yPj", pointPi.copyX_i(), voterIndex).scale(alpha);
							yP.add(new PointND("Temp", pointCi.copyX_i(), -1).scale(oneMinusAlpha));
						}
						yPij_pj = yP.getDist(pointPj);
						//-------------------------------------------------------
						distance = Math.min(yCij_cj, yPij_pj);
					}
					distances[stepIndex] = distance;
				}
			}
		}
		toCalculateDist = null;
		//		=============================================================================
		//		 Now we have all distances. Let's calculate and output distortions.
		//		=============================================================================

		printHeader(utilityFunctions, listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists, true, out);
		printHeader(utilityFunctions, listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists, false, summary);

		int indexInGroup;
		float[] distortions = new float[NUM_SEEDS];
		float mean, variance = 0.0f;
		
		// Do this every step
		for (int stepIndex = 0; stepIndex < states.length; stepIndex++) {
			if (states[stepIndex] == null) {
				continue;
			}
//			System.out.println("Num of splitting schemata = "+ worldAfterSplitting.size());
			out.print(stepIndex); out.print('\t');
			summary.print(stepIndex); summary.print('\t');
			
			for (UtilityFunction utilityFunction : utilityFunctions) {
				for (int i = 0; i < listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists.size(); i++) {
					indexInGroup = i % NUM_SEEDS;
					Pair<List<Integer>> pair = listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists.get(i);
					List<Integer> iterationCandidateIdsList = pair.key;
					List<Integer> iterationVoterIdsList   = pair.value;

					// Vote (order + score, select winner + Break tie)
					Integer winnerId = elect(allDistances, stepIndex, tieBreakingRule, votingRule, utilityFunction, iterationCandidateIdsList, iterationVoterIdsList);

					// Sum all candidate-voters distances per candidate
					float nominator = 0, minSumDistance = Float.MAX_VALUE;
					mean = variance = 0.0f;
					for (Integer candidateId : iterationCandidateIdsList) {
						float sumDistance = 0.0f;
						for (Integer voterId : iterationVoterIdsList) {
							sumDistance += allDistances[candidateId][voterId][stepIndex];
						}
						// Find the nominator (sum of distances per winner)
						if (candidateId == winnerId) {
							nominator = sumDistance;
						}
						// Find denominator (minimum sum of distances)
						if (sumDistance < minSumDistance) {
							minSumDistance = sumDistance;
						}
					}
					
					float distortion = nominator / minSumDistance;
					mean += distortion;
					distortions[indexInGroup] = distortion;

					out.format(floatAndDelimeter, distortion);
					if(indexInGroup == NUM_SEEDS - 1) {
//						final int ii = (i+1) / 20;
//						if(ii% 9 == 0) {
//							out.print("#\t");
//						}else if (ii % 3 == 0) {
//							out.print("|\t");
//						}else {
//							out.print(",\t");
//						}
						mean /= NUM_SEEDS;
						for (int j = 0; j < distortions.length; j++) {
							variance += Math.pow((distortions[j] - mean), 2);
						}
						variance /= NUM_SEEDS;
						summary.format(floatAndDelimeter, mean);
						summary.format(floatAndDelimeter, variance);
						mean = variance = 0.0f;
					}

				}
			}
			out.println();
			summary.println();
		}

		System.out.println("All done!");
		out.close();
	}

	private Set<Integer> findFixedIDs(File fileDCP, float defaultEgo) throws IOException {
		LinkedHashSet<Integer> ids = new LinkedHashSet<>();
		Scanner scanner = new Scanner(fileDCP);
		while (scanner.hasNextLine()) {
			String line = (String) scanner.nextLine();
			if(line.length() == 0 || line.startsWith("#"))
				continue;
			String[] splits = line.split("(,|:) ");
//			float ego = Float.parseFloat(splits[1]);
			int dIn = Integer.parseInt(splits[2]);
			if(dIn == 1 || Float.parseFloat(splits[1]) > defaultEgo) {
				String pId = splits[0];
				ids.add(Integer.parseInt(pId.substring(1)));
			}
		}
		scanner.close();
		return ids;
	}

	private static void printHeader(UtilityFunction[] utilityFunctions, List<Pair<List<Integer>>> listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists,
			boolean extended, PrintStream out) {
		out.print("step\t");
		final String[] utilityName = new String[] {"xb", "p", "v"};
		final int[] candidates = new int[] {3, 6};
		final String[] source = new String[] {"100%F", "50%F", "0%F"};
		int groupSize, max;
		String[] individualId;
		if(extended) {
			groupSize = NUM_SEEDS;
			individualId = new String[NUM_SEEDS];
			for(int i = 0; i < individualId.length; i++){
				individualId[i] = String.valueOf(i+1);
			}
			max = utilityFunctions.length * listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists.size();
		} else {
			groupSize = 2;
			individualId = new String[]{"M", "V"};
			max = utilityFunctions.length * listOfFiftyListsOfPairsOfCandidatesIdListsAndVoterIdLists.size() / NUM_SEEDS * groupSize;
		}
		for (int i = 0; i < max; i++) {
			int individualIndex = i % groupSize;
			int reminder = i / groupSize;
			final int utilityIndex = reminder / 6; //TODO double check
			reminder %= 6; //TODO double check
			final int candidatesNo = reminder / 3; //TODO double check
			reminder %= 3; //TODO double check
			out.format("U%s_C%d_%s_%s\t", utilityName[utilityIndex], candidates[candidatesNo],source[reminder], individualId[individualIndex]);
		}
		out.println();
	}

	Integer elect(final float[][][] allDistances, int stepIndex, TieBreakingRule tieBreakingRule, VotingRule votingRule,
			UtilityFunction utilityFunction, List<Integer> candidatIds, List<Integer> voterIds) {
		List<Map<Integer, Integer>> allCandidatesScores = new ArrayList<>();
		for (Integer voterId : voterIds) {
			Map<Integer, Float> candidatesDistancesPerVoter = new LinkedHashMap<>();
			for (Integer candidateId : candidatIds) {
				candidatesDistancesPerVoter.put(candidateId, allDistances[candidateId][voterId][stepIndex]);
			}
			Map<Integer, Integer> candidatesScoreforThisVoter = utilityFunction.scoreCandidates(candidatesDistancesPerVoter);
			allCandidatesScores.add(candidatesScoreforThisVoter);
		}
		return votingRule.findWinner(allCandidatesScores, tieBreakingRule);
	}

	/**@deprecated not used :D
	 * @param values
	 * @param removePercentOutliers
	 * @return
	 */
	public static float[] calcAverages(float[][] values, float removePercentOutliers) {
		float[] ret = new float[values.length];
		for (int i = 0; i < ret.length; i++) {
			final float[] currentList = values[i];
			Arrays.sort(currentList);
			int lowIndex=0,highIndex=currentList.length - 1;
			while(currentList[lowIndex] == Float.NEGATIVE_INFINITY) lowIndex++;
			while(currentList[highIndex] == Float.POSITIVE_INFINITY) highIndex--;
			int effectiveLength = highIndex - lowIndex + 1;
			int halfOutliersToRemove = (int) (removePercentOutliers * effectiveLength / 100 / 2);
			float sum = 0;
			for (int j = lowIndex + halfOutliersToRemove; j < highIndex - halfOutliersToRemove; j++) {
				sum += currentList[j];
			}
			sum /= effectiveLength - 2 *  halfOutliersToRemove;
			ret[i]= sum;
		}
		return ret;
	}
	public static class Pair<T>{
		public T key;
		public T value;
		public Pair(T key, T value) {
			this.key = key;
			this.value = value;
		}
	}
}
