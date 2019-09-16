/**
 * 
 */
package sg.edu.ntu.voting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.ImportException;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.control.cli.GraphsIO;
import sg.edu.ntu.jopinions.control.cli.Parser;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.Utils;

/**
 * @author Amr
 *
 */
public class DistortionCalculator {

	public static final String HEADER = "step\tOthers";// TODO complete
	
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

		final int n = Integer.valueOf(Utils.getParameter(args, "-numCouples", "-1", "400"));
		allDistances = new float[n][n][];

		//		File logFile = new File(inFolder, String.format("log-%s.log",id));
		String id = Utils.getParameter(args, "-id", null, inFolder.getName());
		File fileGG = new File(inFolder, String.format("gg-%s.log", id));
		File filePP = new File(inFolder, String.format("pp-%s.log", id));
		File xFile  = new File(inFolder, String.format("x-%s.log", id));

		PointND.PointNDSupplier pointNDSupplier = new PointND.PointNDSupplier(d_3, Constants.CASTOR);
		DefaultDirectedGraph<PointND, DefaultEdge> graphCC = new DefaultDirectedGraph<>(pointNDSupplier, null, false);
		pointNDSupplier = new PointND.PointNDSupplier(d_3, Constants.PULLOX);
		DefaultDirectedGraph<PointND, DefaultEdge> graphPP = new DefaultDirectedGraph<>(pointNDSupplier, null, false);
		try {
			GraphsIO.importGraph(Constants.CASTOR, d_3, graphCC, fileGG);
			GraphsIO.importGraph(Constants.PULLOX, d_3, graphPP, filePP);
		} catch (ImportException e1) {
			e1.printStackTrace();
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
		ArrayList<Integer> allPossibleFullFixedCandidatesIDsArrayList = new ArrayList<>();
		ArrayList<Integer> allPossibleHalfFixedCandidatesIDsArrayList = new ArrayList<>();
		ArrayList<Integer> allPossible_________CandidatesIDsArrayList = new ArrayList<>();

		for (int i = 0; i < allPulloxPointNDs.length; i++) {
			PointND pointC = allCastorPointNDs[i];
			PointND pointP = allPulloxPointNDs[i];
			int pairId = pointC.getId();
			int level = 0;
			if(pointC.getInDegree() == 1) {
				level++;
			}
			if(pointP.getInDegree() == 1) {
				level++;
			}
			switch (level) {
			case 2:
				allPossibleFullFixedCandidatesIDsArrayList.add(pairId);
				//Fall through
			case 1:
				allPossibleHalfFixedCandidatesIDsArrayList.add(pairId);
				//Fall through
			case 0:
				allPossible_________CandidatesIDsArrayList.add(pairId);
			}
		}
		final int ffCandidatesSize = allPossibleFullFixedCandidatesIDsArrayList.size();
//		System.out.format("Num of FF Candidates = %d, Num combinations of 7 = %d, Num combinations of 8 = %d, Num combinations of 9 = %d.\n", ffCandidatesSize, Helper.nCr(ffCandidatesSize, 7), Helper.nCr(ffCandidatesSize, 8), Helper.nCr(ffCandidatesSize, 9));
		if (ffCandidatesSize < 13) {
			System.err.format("%s has only %d fully fixed candidates. Not processed.\n", id, ffCandidatesSize);
			return;
		}

//		System.out.println("starting Random generation");
		Random random = new Random(0);
		Helper<Integer> helper = new Helper<>();
		List<List<Integer>> twentyListsOfCombinationsOf7FFixed	= helper.giveMeXDifferentCombinations(allPossibleFullFixedCandidatesIDsArrayList, 7, 20, random);
		List<List<Integer>> twentyListsOfCombinationsOf7HFixed	= helper.giveMeXDifferentCombinations(allPossibleHalfFixedCandidatesIDsArrayList, 7, 20, random);
		List<List<Integer>> twentyListsOfCombinationsOf7Any 	= helper.giveMeXDifferentCombinations(allPossible_________CandidatesIDsArrayList, 7, 20, random);
		List<List<Integer>> twentyListsOfCombinationsOf8FFixed	= helper.giveMeXDifferentCombinations(allPossibleFullFixedCandidatesIDsArrayList, 8, 20, random);
		List<List<Integer>> twentyListsOfCombinationsOf8HFixed	= helper.giveMeXDifferentCombinations(allPossibleHalfFixedCandidatesIDsArrayList, 8, 20, random);
		List<List<Integer>> twentyListsOfCombinationsOf8Any		= helper.giveMeXDifferentCombinations(allPossible_________CandidatesIDsArrayList, 8, 20, random);
		List<List<Integer>> twentyListsOfCombinationsOf9FFixed	= helper.giveMeXDifferentCombinations(allPossibleFullFixedCandidatesIDsArrayList, 9, 20, random);
		List<List<Integer>> twentyListsOfCombinationsOf9HFixed	= helper.giveMeXDifferentCombinations(allPossibleHalfFixedCandidatesIDsArrayList, 9, 20, random);
		List<List<Integer>> twentyListsOfCombinationsOf9Any		= helper.giveMeXDifferentCombinations(allPossible_________CandidatesIDsArrayList, 9, 20, random);

		List<List<List<Integer>>> listOfTwentyListsOfCandidatesIdLists = new ArrayList<>();
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf7FFixed);
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf7HFixed);
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf7Any);
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf8FFixed);
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf8HFixed);
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf8Any);
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf9FFixed);
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf9HFixed);
		listOfTwentyListsOfCandidatesIdLists.add(twentyListsOfCombinationsOf9Any);


		HashSet<Integer> allCandidateIDs = new HashSet<>();
		for (List<List<Integer>> twentyListsOfCandidatesIds : listOfTwentyListsOfCandidatesIdLists) {
			for (List<Integer> listOfCandidateIds : twentyListsOfCandidatesIds) {
				allCandidateIDs.addAll(listOfCandidateIds);
			}
		}
		
		List<Integer> universalListOfIds = new ArrayList<Integer> (allPossible_________CandidatesIDsArrayList);
		List<Pair<List<Integer>>> listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists = new ArrayList<>();
		for (List<List<Integer>> twentyListsOfCandidatesIds : listOfTwentyListsOfCandidatesIdLists) {
			for (List<Integer> iterationListOfCandidateIds : twentyListsOfCandidatesIds) {
				List<Integer> iterationListOfVoterIds = new ArrayList<>(universalListOfIds);
				iterationListOfVoterIds.removeAll(iterationListOfCandidateIds);
				listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists.add(new Pair<List<Integer>>(iterationListOfCandidateIds, iterationListOfVoterIds));
			}
		}
		

		Parser parser = new Parser(n, d_3, xFile);
		states = parser.parse();
		System.out.println("finished reading input files");

		
		UtilityFunction[] utilityFunctions = new UtilityFunction[] {new BordaUtility(), new PluralityUtility(), new VetoUtility()};
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
		for (Integer candidateIdInteger : allCandidateIDs) {
			int candidateIndex = candidateIdInteger -1;
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
				//TODO Shall I change the names?
				PointND pointCi= candidateC;
				PointND pointPi= candidateP;

				vectorV1 = pointCi.minus(pointPi);
				//equals |v1|^2
				v1v1DotProduct = PointND.dotProductRawData(vectorV1, vectorV1);
				//////////////////////////////////////////////////////////////////////////////////////////////////
				//	Important Note: I use the suffex Id to indicate the natural (1-based) order,				//
				//	while the suffex Index to indicate the engineering (0-based) order.							//
				//	The index is used only for array indexing, while the ID is used for everything else.		//
				//  Please do NOT mix.																			//
				//////////////////////////////////////////////////////////////////////////////////////////////////
				for (int voterIndex = 0; voterIndex < n; voterIndex++) {
					if (voterIndex == candidateIndex) {
						continue;
					}
					float[] distances;
					if(stepIndex ==0) {
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

		printHeader(utilityFunctions, listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists, true, out);
		printHeader(utilityFunctions, listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists, false, summary);

		int indexInGroup;
		float[] distortions = new float[20];
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
				for (int i = 0; i < listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists.size(); i++) {
					indexInGroup = i % 20;
					Pair<List<Integer>> pair = listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists.get(i);
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
							sumDistance += allDistances[candidateId - 1][voterId - 1][stepIndex];
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
					if(indexInGroup ==19) {
//						final int ii = (i+1) / 20;
//						if(ii% 9 == 0) {
//							out.print("#\t");
//						}else if (ii % 3 == 0) {
//							out.print("|\t");
//						}else {
//							out.print(",\t");
//						}
						mean /= 20;
						for (int j = 0; j < distortions.length; j++) {
							variance += Math.pow((distortions[j] - mean), 2);
						}
						variance /= 20;
						summary.format(floatAndDelimeter, mean);
						summary.format(floatAndDelimeter, variance);
						mean = variance = 0.0f;
					}

				}
			}
			out.println();
			summary.println();
		}
		
//		out.print(DistortionCalculator.HEADER);
//		out.println("\n");
//
//		float[][] allValues = new float [24][mobileCastorPointNDs.length];
//


//		float[] averages = calcAverages(allValues, 3);
////		if (verbose) {
////			System.out.println(Arrays.toString(averages));
////		}
//		summary.format("%d\t",stepId);
//		summary.format("avr\t");//place holder for id. Just for consistency
//		for (int i = 0; i < averages.length; i++) {
//			summary.format(floatAndDelimeter, averages[i]);
//		}
//		summary.format("\n");

		System.out.println("All done!");
		out.close();
	}

	private static void printHeader(UtilityFunction[] utilityFunctions, List<Pair<List<Integer>>> listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists,
			boolean extended, PrintStream out) {
		out.print("step\t");
		final int[] candidates = new int[] {7, 8, 9};
		final String[] fixationStates = new String[] {"FF", "HF", "AF"};
		int groupSize, max;
		String[] individualId;
		if(extended) {
			groupSize = 20;
			individualId = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"};
			max = utilityFunctions.length * listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists.size();
		} else {
			groupSize = 2;
			individualId = new String[]{"M", "V"};
			max = utilityFunctions.length * listOfTwentyListsOfPairsOfCandidatesIdListsAndVoterIdLists.size() / 20 * groupSize;
		}
		for (int i = 0; i < max; i++) {
			int individualIndex = i % groupSize;
			int reminder = i / groupSize;
			final int utilityIndex = reminder / 9;
			reminder %= 9;
			final int candidatesNo = reminder / 3;
			reminder %= 3;
			out.format("U%cC%d%s%s\t", "bpv".charAt(utilityIndex), candidates[candidatesNo],fixationStates[reminder], individualId[individualIndex]);
		}
		out.println();
	}

	Integer elect(final float[][][] allDistances, int stepIndex, TieBreakingRule tieBreakingRule, VotingRule votingRule,
			UtilityFunction utilityFunction, List<Integer> candidatIds, List<Integer> voterIds) {
		List<Map<Integer, Integer>> allCandidatesScores = new ArrayList<>();
		for (Integer voterId : voterIds) {
			Map<Integer, Float> candidatesDistancesPerVoter = new LinkedHashMap<>();
			for (Integer candidateId : candidatIds) {
				candidatesDistancesPerVoter.put(candidateId, allDistances[candidateId -1][voterId -1][stepIndex]);
			}
			Map<Integer, Integer> candidatesScoreforThisVoter = utilityFunction.scoreCandidates(candidatesDistancesPerVoter);
			allCandidatesScores.add(candidatesScoreforThisVoter);
		}
		return votingRule.findWinner(allCandidatesScores, tieBreakingRule);
	}

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
