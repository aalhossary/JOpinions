/**
 * 
 */
package sg.edu.ntu.jopinions.control.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.ImportException;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.Utils;

/**
 * @author Amr
 *
 */
public class AnxietyCalculator {

	/**NOT yet set automatically*/
	private static final int d_3 = 3;
	File inputFile = null;
	File outputFile = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inFolderString = Utils.getParameter(args, "-inFolder", null, null);
		String outFolderString = Utils.getParameter(args, "-outFolder", null, "./");
		File inFolder = new File(inFolderString);
		String id = inFolder.getName();
		File outFile = new File(outFolderString,String.format("stat2-%s.csv",id));
		File summaryFile = new File(outFolderString,String.format("stat2avr-%s.csv",id));
//		File logFile = new File(outFolderString,String.format("log-%s.log",id));
//		if(logFile.exists()) {
//			float[] cimDistances = parseLogFile(logFile);
//		}

		//merge the two param sources, keeping the priority to the sacred command line arguments
		String[] id2Args = Utils.id2Args(id);
		String[] temp = args;
		args= new String[temp.length+id2Args.length];
		System.arraycopy(temp, 0, args, 0, temp.length);
		System.arraycopy(id2Args, 0, args, temp.length, id2Args.length);
		
		new AnxietyCalculator().calculate(inFolder, outFile, summaryFile, args);
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
		
    	int n = Integer.valueOf(Utils.getParameter(args, "-numCouples", "-1", "400"));
    	float[][] prevState = new float[2*n][d_3];
    	for (int i = 0; i < prevState.length; i++) {
    		for (int j = 0; j < prevState[i].length; j++) {
    			prevState[i][j] = 1/d_3;
    		}
    	}
    	
    	String id = inFolder.getName();
//		File logFile = new File(inFolder, String.format("log-%s.log",id));
		File fileGG = new File(inFolder, String.format("gg-%s.log", id));
		File filePP = new File(inFolder, String.format("pp-%s.log", id));
    	File xFile  = new File(inFolder, String.format("x-%s.log", id));

		PointND.PointNDSupplier pointNDSupplier = new PointND.PointNDSupplier(d_3, Constants.CASTOR);
		DefaultDirectedGraph<PointND, DefaultEdge> graphCC = new DefaultDirectedGraph<>(pointNDSupplier, null, false);
		pointNDSupplier = new PointND.PointNDSupplier(d_3, Constants.PULLOX);
		DefaultDirectedGraph<PointND, DefaultEdge> graphPP = new DefaultDirectedGraph<>(pointNDSupplier, null, false);
//		@SuppressWarnings("unchecked")
//		Graph<PointND, DefaultEdge>[] graphs = (Graph<PointND, DefaultEdge>[]) new Graph[] {graphCC, null, null, graphPP};
		
    	try {
			GraphsIO.importGraph(Constants.CASTOR, d_3, graphCC, fileGG);
    		GraphsIO.importGraph(Constants.PULLOX, d_3, graphPP, filePP);
    	} catch (ImportException e1) {
    		e1.printStackTrace();
    	}
//    	if (verbose) {
//			System.out.println(graphCC);
//			System.out.println(graphPP);
//		}
    	/**stateless nodes*/
    	PointND[] castorPointNDs = graphCC.vertexSet().toArray(new PointND[0]);
    	/**stateless nodes*/
    	PointND[] pulloxPointNDs = graphPP.vertexSet().toArray(new PointND[0]);
    	//check for consistency
    	for (int i = 0; i < pulloxPointNDs.length; i++) {
    		PointND pointC = castorPointNDs[i];
    		PointND pointP = pulloxPointNDs[i];
    		if (pointC.getId() != pointP.getId()) {
    			throw new RuntimeException("points are not corresponding: "+ pointC + ", " + pointP);
    		}
    	}
		Utils.cacheVerticesDegrees(graphCC);
		Utils.cacheVerticesDegrees(graphPP);

		//find stubborn and mobile points
		ArrayList<PointND> fixedCastorPointNDArrayList = new ArrayList<>();
		ArrayList<PointND> fixedPulloxPointNDArrayList = new ArrayList<>();
		ArrayList<PointND> mobileCastorPointNDArrayList = new ArrayList<>();
		ArrayList<PointND> mobilePulloxPointNDArrayList = new ArrayList<>();
		
		boolean[] mobile = new boolean[castorPointNDs.length];
		ArrayList<Integer> indexOArrayList = new ArrayList<>();
		ArrayList<Integer> indexVArrayList = new ArrayList<>();
		for (int i = 0; i < pulloxPointNDs.length; i++) {
			final PointND pointC = castorPointNDs[i];
			final PointND pointP = pulloxPointNDs[i];
			if(pointC.getInDegree() == 1 || pointP.getInDegree() == 1) {
				fixedCastorPointNDArrayList.add(pointC);
				fixedPulloxPointNDArrayList.add(pointP);
				indexOArrayList.add(i);
			}else {
				mobileCastorPointNDArrayList.add(pointC);
				mobilePulloxPointNDArrayList.add(pointP);
				indexVArrayList.add(i);
				mobile[i]= true;
			}
		}
		PointND[] mobileCastorPointNDs = mobileCastorPointNDArrayList.toArray(new PointND[mobileCastorPointNDArrayList.size()]);
		PointND[] mobilePulloxPointNDs = mobilePulloxPointNDArrayList.toArray(new PointND[mobilePulloxPointNDArrayList.size()]);
		PointND[] fixedCastorPointNDs  = fixedCastorPointNDArrayList .toArray(new PointND[fixedCastorPointNDArrayList .size()]);
		PointND[] fixedPulloxPointNDs  = fixedPulloxPointNDArrayList .toArray(new PointND[fixedPulloxPointNDArrayList .size()]);
		int[] indexO = new int[fixedCastorPointNDs.length];
		int[] indexV = new int[mobileCastorPointNDs.length];
		{
			int oi = 0, vi = 0;
			for (int i = 0; i < mobile.length; i++) {
				if (mobile[i]) {
					indexV[vi++] = i;
				} else {
					indexO[oi++] = i;
				}
			}
		}
		
		Parser parser = new Parser(n, d_3, xFile);
		float[][][] states = parser.parse();
		System.out.println("finished reading input files");

		//now start calculations
		
		//only for initial and final states
		for (int step = 0; step < states.length; step += states.length-1) {
			float[][] currentState = states[step];
			if(currentState == null) {
//				continue;
				throw new IllegalStateException("no data in step # "+step);
			}

			for (int i = 0; i < fixedCastorPointNDs.length; i++) {
				fixedCastorPointNDs[i].match(currentState[indexO[i]]);
				fixedPulloxPointNDs[i].match(currentState[indexO[i]+n]);
			}
			for (int j = 0; j < mobilePulloxPointNDs.length; j++) {
				mobileCastorPointNDs[j].match(currentState[indexV[j]]);
				mobilePulloxPointNDs[j].match(currentState[indexV[j]+n]);
			}
			
			float alpha, oneMinusAlpha;
			float v1v1DotProduct, v1v2DotProduct;
			float[] vectorV1, vectorV2;
			PointND yC, yP;
			Measures[][] allMeasures = new Measures[fixedCastorPointNDs.length][mobileCastorPointNDs.length];

			for (int i = 0; i < fixedCastorPointNDs.length; i++) {
				PointND pointCi= fixedCastorPointNDs[i];
				PointND pointPi= fixedPulloxPointNDs[i];
				
				vectorV1 = pointCi.minus(pointPi);
				//equals |v1|^2
				v1v1DotProduct = PointND.dotProductRawData(vectorV1, vectorV1);

				for (int j = 0; j < mobileCastorPointNDs.length; j++) {
					PointND pointCj= mobileCastorPointNDs[j];
					PointND pointPj= mobilePulloxPointNDs[j];
					Measures measures = new Measures();
					allMeasures[i][j] = measures;
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
						yC = new PointND("yCj", pointCi.copyX_i(), j).scale(alpha);
						yC.add(new PointND("Temp", pointPi.copyX_i(), -1).scale(oneMinusAlpha));
					}
					measures.yCij_cj = yC.getDist(pointCj);
					
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
						yP = new PointND("yPj", pointPi.copyX_i(), j).scale(alpha);
						yP.add(new PointND("Temp", pointCi.copyX_i(), -1).scale(oneMinusAlpha));
					}
					measures.yPij_pj = yP.getDist(pointPj);
					//-------------------------------------------------------
					if (measures.yCij_cj < measures.yPij_pj) {
						measures.d1 = measures.yCij_cj;
						measures.d2 = measures.yPij_pj;
					} else {
						measures.d1 = measures.yPij_pj;
						measures.d2 = measures.yCij_cj;
					}
				}
			}
			
			out.format("step\tid\tlMax\tlMin\tsMin/lMax\tsMin/lMin"
					+ "\t(sMin+m_v)/(lMin/m_v)\t(sMin+m_v)/(lMax/m_v)"
					+ "\th(sMin,lMin)\th(sMin,lMax)\th(s^C_*,s^P_*)\tH(sMin,lMin)\tH(sMin,lMax)\tH(s^C_*,s^P_*)"
					+ "\tH(sMin,lMin)-H(sMin,m_v,lMin)\tH(sMin,lMax)-H(sMin,m_v,lMax)\tH(s^C_*,s^P_*)-H(s^C_*,m_v,s^P_*)"
					+ "\th(sMin,m_v,lMin)\th(sMin,m_v,lMax)\th(s^C_*,m_v,s^P_*)"
					+ "\tH((sMin/(sMin+m_v+lMin)),(lMin/(sMin+m_v+lMin)))\tH((sMin/(sMin+m_v+lMax)),(lMax/(sMin+m_v+lMax)))\tH((s^C_*/(s^C_*,m_v,s^P_*)),(s^P_*/(s^C_*,m_v,s^P_*)))"
					+ "\tH((sMin/m_v),(lMin/m_v))\tH((sMin/m_v),(lMax/m_v))\tH((s^C_*/m_v),(s^P_*/m_v))"
					+ "\n");
			
			float[][] allValues = new float [24][mobileCastorPointNDs.length];

			//now start calculating real measures. Let the fun begin
			for (int j = 0; j < mobileCastorPointNDs.length; j++) {
				PointND pointCj= mobileCastorPointNDs[j];
				PointND pointPj= mobilePulloxPointNDs[j];
				float m_v = pointCj.getDist(pointPj);

				float sMin = Float.MAX_VALUE, lMin = Float.MIN_VALUE, lMax = Float.MIN_VALUE;
				float sC_star = Float.MAX_VALUE, sP_star = Float.MAX_VALUE;
				for (int i = 0; i < fixedCastorPointNDs.length; i++) {
					final Measures measures = allMeasures[i][j];
					final float tempSMin = measures.d1;
					if (tempSMin < sMin)
						sMin = tempSMin;
					final float tempLMin = measures.d1;
					if (tempLMin > lMin)
						lMin = tempLMin;
					final float tempLMax = measures.d2;
					if (tempLMax > lMax)
						lMax = tempLMax;
					final float tempSC_star = measures.yCij_cj;
					if (tempSC_star < sC_star)
						sC_star = tempSC_star;
					final float tempPC_star = measures.yPij_pj;
					if (tempPC_star < sP_star)
						sP_star = tempPC_star;
				}
				
				float distToWorst1 = lMax;
				float distToWorst2 = lMin;

				float bestToWorst1 = sMin / lMax;
				float bestToWorst2 = sMin / lMin;
				
				float bestToWorst_withSize1 = (sMin+m_v) / (lMin + m_v);
				float bestToWorst_withSize2 = (sMin+m_v) / (lMax + m_v);

				float relIndiff_bestToWorst1 = calc_h(sMin, lMin);
				float relIndiff_bestToWorst2 = calc_h(sMin, lMax);
				float relIndiff_bestToWorst3 = calc_h(sC_star, sP_star);
				float relIndiff_bestToWorst4 = calc_H(sMin, lMin);
				float relIndiff_bestToWorst5 = calc_H(sMin, lMax);
				float relIndiff_bestToWorst6 = calc_H(sC_star, sP_star);

				float difference_relIndiff_bestToWorst_becauseOfMv1 = calc_H(sMin, lMin) - calc_H(sMin, m_v, lMin);
				float difference_relIndiff_bestToWorst_becauseOfMv2 = calc_H(sMin, lMax) - calc_H(sMin, m_v, lMax);
				float difference_relIndiff_bestToWorst_becauseOfMv3 = calc_H(sC_star, sP_star) - calc_H(sC_star, m_v, sP_star);

				float relIndiff_bestToWorst_withMv1 = calc_h(sMin, m_v, lMin);
				float relIndiff_bestToWorst_withMv2 = calc_h(sMin, m_v, lMax);
				float relIndiff_bestToWorst_withMv3 = calc_h(sC_star, m_v, sP_star);

				float superNormalizedIndiff_bestToWorst1 = calc_H((sMin / (sMin + lMin + m_v)), (lMin / (sMin + lMin + m_v)));
				float superNormalizedIndiff_bestToWorst2 = calc_H((sMin / (sMin + lMax + m_v)), (lMax / (sMin + lMax + m_v)));
				float superNormalizedIndiff_bestToWorst3 = calc_H((sC_star / (sC_star + sP_star + m_v)), (sP_star / (sC_star + sP_star + m_v)));

				float scaledIndiff_bestToWorst_withMv1 = calc_H((sMin / m_v), (lMin / m_v));
				float scaledIndiff_bestToWorst_withMv2 = calc_H((sMin / m_v), (lMax / m_v));
				float scaledIndiff_bestToWorst_withMv3 = calc_H((sC_star / m_v), (sP_star / m_v));

				allValues[0][j]=distToWorst1; allValues[1][j]=distToWorst2; 
				allValues[2][j]=bestToWorst1;allValues[3][j]=bestToWorst2;
				allValues[4][j]=bestToWorst_withSize1;allValues[5][j]=bestToWorst_withSize2;

				allValues[6][j]=relIndiff_bestToWorst1;allValues[7][j]=relIndiff_bestToWorst2;allValues[8][j]=relIndiff_bestToWorst3;
				allValues[9][j]=relIndiff_bestToWorst4;allValues[10][j]=relIndiff_bestToWorst5;allValues[11][j]=relIndiff_bestToWorst6;
				allValues[12][j]=difference_relIndiff_bestToWorst_becauseOfMv1;allValues[13][j]=difference_relIndiff_bestToWorst_becauseOfMv2;allValues[14][j]=difference_relIndiff_bestToWorst_becauseOfMv3;
				allValues[15][j]=relIndiff_bestToWorst_withMv1;allValues[16][j]=relIndiff_bestToWorst_withMv2;allValues[17][j]=relIndiff_bestToWorst_withMv3;
				allValues[18][j]=superNormalizedIndiff_bestToWorst1;allValues[19][j]=superNormalizedIndiff_bestToWorst2;allValues[20][j]=superNormalizedIndiff_bestToWorst3;
				allValues[21][j]=scaledIndiff_bestToWorst_withMv1;allValues[22][j]=scaledIndiff_bestToWorst_withMv2;allValues[23][j]=scaledIndiff_bestToWorst_withMv3;

//				out.format("step\tid\tlMax\tlMin\tsMin/lMax\tsMin/lMin"
//						+ "\t(sMin+m_v)/(lMin/m_v)\t(sMin+m_v)/(lMax/m_v)"
//						+ "\th(sMin,lMin)\th(sMin,lMax)\th(s^C_*,s^P_*)\tH(sMin,lMin)\tH(sMin,lMax)\tH(s^C_*,s^P_*)"
//						+ "\tH(sMin,lMin)-H(sMin,m_v,lMin)\tH(sMin,lMax)-H(sMin,m_v,lMax)\tH(s^C_*,s^P_*)-H(s^C_*,m_v,s^P_*)"
//						+ "\th(sMin,m_v,lMin)\th(sMin,m_v,lMax)\th(s^C_*,m_v,s^P_*)"
//						+ "\tH((sMin/(sMin+m_v+lMin)),(lMin/(sMin+m_v+lMin)))\tH((sMin/(sMin+m_v+lMax)),(lMax/(sMin+m_v+lMax)))\tH((s^C_*/(s^C_*,m_v,s^P_*)),(s^P_*/(s^C_*,m_v,s^P_*)))"
//						+ "\tH((sMin/m_v),(lMin/m_v))\tH((sMin/m_v),(lMax/m_v))\tH((s^C_*/m_v),(s^P_*/m_v))"
//						+ "\n");
				// start output
				out.format("%d\t",step);
				out.format("%d\t", mobileCastorPointNDs[j].getId());//TODO if it doesn't work, change it to j and call it "relative order among mobile vertices"
				out.format(floatAndDelimeter, distToWorst1);
				out.format(floatAndDelimeter, distToWorst2);

				out.format(floatAndDelimeter, bestToWorst1);
				out.format(floatAndDelimeter, bestToWorst2);
				
				out.format(floatAndDelimeter, bestToWorst_withSize1);
				out.format(floatAndDelimeter, bestToWorst_withSize2);

				out.format(floatAndDelimeter, relIndiff_bestToWorst1);
				out.format(floatAndDelimeter, relIndiff_bestToWorst2);
				out.format(floatAndDelimeter, relIndiff_bestToWorst3);
				out.format(floatAndDelimeter, relIndiff_bestToWorst4);
				out.format(floatAndDelimeter, relIndiff_bestToWorst5);
				out.format(floatAndDelimeter, relIndiff_bestToWorst6);
				
				out.format(floatAndDelimeter, difference_relIndiff_bestToWorst_becauseOfMv1);
				out.format(floatAndDelimeter, difference_relIndiff_bestToWorst_becauseOfMv2);
				out.format(floatAndDelimeter, difference_relIndiff_bestToWorst_becauseOfMv3);

				out.format(floatAndDelimeter, relIndiff_bestToWorst_withMv1);
				out.format(floatAndDelimeter, relIndiff_bestToWorst_withMv2);
				out.format(floatAndDelimeter, relIndiff_bestToWorst_withMv3);
				
				out.format(floatAndDelimeter, superNormalizedIndiff_bestToWorst1);
				out.format(floatAndDelimeter, superNormalizedIndiff_bestToWorst2);
				out.format(floatAndDelimeter, superNormalizedIndiff_bestToWorst3);
				
				out.format(floatAndDelimeter, scaledIndiff_bestToWorst_withMv1);
				out.format(floatAndDelimeter, scaledIndiff_bestToWorst_withMv2);
				out.format(floatAndDelimeter, scaledIndiff_bestToWorst_withMv3);
//				out.format(floatAndDelimeter, );
//				out.format("%B\t", cIM);
				out.format("\n");
			}
			
			float[] averages = calcAverages(allValues, 3);
//			if (verbose) {
//				System.out.println(Arrays.toString(averages));
//			}
			summary.format("%d\t",step);
			summary.format("avr\t");//place holder for id. Just for consistency
			for (int i = 0; i < averages.length; i++) {
				summary.format(floatAndDelimeter, averages[i]);
			}
			summary.format("\n");
		}
		
		System.out.println("All done!");
		out.close();
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

	private static class Measures{
		float yCij_cj;
		float yPij_pj;
		float d1,d2;
	}
	
	public static float calc_H(float p1, float p2) {
		if(p1 == 0)return calc_H(p2);
		if(p2 == 0)return calc_H(p1);
		float piLogPi = (float) ((p1*Math.log(p1)) + (p2*Math.log(p2)));
		return  - piLogPi;
	}
	public static float calc_H(float p1, float p2, float p3) {
		if(p1 == 0)return calc_H(p2, p3);
		if(p2 == 0)return calc_H(p1, p3);
		if(p3 == 0)return calc_H(p1, p2);
		float piLogPi = (float) ((p1*Math.log(p1)) + (p2*Math.log(p2)) + (p3*Math.log(p3)));
		return  - piLogPi;
	}
	public static float calc_H(float... ps) {
		double piLogPi = 0;
		for (int i = 0; i < ps.length; i++) {
			final float pi = ps[i];
			if (pi>0) {
				piLogPi += (pi*Math.log(pi));
			}
		}
		return (float) - piLogPi;
	}

	public static float calc_h(float p1, float p2) {
		float sumP = p1 + p2;
		return calc_H(p1/sumP , p2 / sumP);
	}
	public static float calc_h(float p1, float p2, float p3) {
		float sumP = p1 + p2 + p3;
		return calc_H(p1/sumP, p2 / sumP, p3 / sumP);
	}
	public static float calc_h(float... ps) {
		float sumP = 0;
		for (int i = 0; i < ps.length; i++) {
			sumP += ps[i];
		}
		float[] psScaled = new float[ps.length];
		for (int i = 0; i < psScaled.length; i++) {
			psScaled[i] = ps[i] /sumP;
		}
		return calc_H(psScaled);
	}
}
