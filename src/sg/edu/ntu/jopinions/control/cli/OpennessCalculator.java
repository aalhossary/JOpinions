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

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.Defaults;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.Utils;

/**
 * @author Amr
 *
 */
public class OpennessCalculator {

	public static final String HEADER = "step\tmeanO\tmedianO\tmaxO\tCIMDist\tCIM\tCTCDist\tPole1#\tPole2#\tBoth Poles";
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
		File outFile = new File(outFolderString,String.format("stat-%s.csv",id));
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
		
		new OpennessCalculator().calculate(inFolder, outFile, args);
	}

	static float[] parseLogFile(File logFile) {
		Pattern pattern = Pattern.compile("Step\\s?=\\s?([0-9]+), Total Diff\\s?=\\s?([-eE.0-9]+), Converged\\s?=\\s?(true|false)");
		Matcher matcher = pattern.matcher("");
		
		ArrayList<Float> temp = new ArrayList<>();
		int lastStep = 0, stepId = -1;
//		Scanner scanner;
		try (Scanner scanner = new Scanner(logFile)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.length()== 0 || line.startsWith("#")) {
					continue;
				}
				matcher.reset(line);
				if (matcher.matches()) {
					stepId = Integer.valueOf(matcher.group(1));
					if(stepId != lastStep+1)
						return null;
					Float dist = Float.valueOf(matcher.group(2));
					temp.add(dist);
					lastStep=stepId;
				}
			}
//			scanner.close();
		} catch (FileNotFoundException e) {
			return null;
		}

		float[] ret = new float[temp.size()+1];
		ret[0]= -1; //some impossible value
		for (int i = 1; i < ret.length; i++) {
			ret[i]= temp.get(i-1); //rest of the values
		}
		return ret;
	}

	private void calculate(File inFolder, File outFile, String[] args) {
		final float maxEpsilon = Defaults.DEFAULT_CONVERGENCE_PRECISION*10;
		PrintStream out = null;
		try {
			out = new PrintStream(outFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (out == null) {
			System.exit(-5);//any nonzero number
		}
//		String floatAndDelimeter = Defaults.OUTPUT_FORMAT+"\t";
		String floatAndDelimeter = "%8.5E\t";
		out.print(OpennessCalculator.HEADER);
		out.print("\n");
		
    	int n = Integer.valueOf(Utils.getParameter(args, "-numCouples", "-1", "400"));
    	float[][] prevState = new float[2*n][d_3];
    	for (int i = 0; i < prevState.length; i++) {
    		for (int j = 0; j < prevState[i].length; j++) {
    			prevState[i][j] = 1/d_3;
    		}
    	}
    	int prevStep = -1;//never parsed yet
    	
    	String id = inFolder.getName();
		File logFile = new File(inFolder, String.format("log-%s.log",id));
		float[] cimDistances = parseLogFile(logFile);
//		if(cimDistances != null)
//			cimDistances[0] = prevState[0];

    	File xFile = new File(inFolder, String.format("x-%s.log", inFolder.getName()));
		Parser parser = new Parser(n, d_3, xFile);
		float[][][] states = parser.parse();
		System.out.println("finished reading input files");
		
		
		//==============Prepare for polarity Calculation start =====================
		String[] manageStubbornValues = Utils.getParameters(args, Constants.PARAM_MANAGE_STUBBORN, (String[])null, new String[]{Constants.NONE});
		float nu=Defaults.NU;//non legal value
		if (manageStubbornValues == null) {
			throw new IllegalArgumentException("If parameter " + Constants.PARAM_MANAGE_STUBBORN + " is introduced, it must be given a value.");
		} else {
			String command = manageStubbornValues[0];//there is at least {"none"}
			if (command.equals(Constants.NONE)) {
				nu = 2.0f;
				System.err.println("Warning: Defaulting to No polarization");
			} else if (command.equals(Constants.POLARIZE_COUPLE) || command.equals(Constants.POLARIZE_SINGLE)) {
				try { nu = Float.valueOf(manageStubbornValues[1]); } catch (Exception e) {
					System.err.println("Could not parse nu value. Defaulting to No polarization");
				}
			} else {
				throw new IllegalArgumentException("Unknown stubborn management command " + command);
			}
		}
		
		PointND vec_001_100 = new PointND("VEC", PointND.minusRawData(new float[]{1,0,0}, new float[]{0,0,1}), -1);
		PointND vec_001_010 = new PointND("VEC", PointND.minusRawData(new float[]{0,1,0}, new float[]{0,0,1}), -1);
		PointND planeNormPositive = new PointND("VEC", PointND.crossProductRawData(vec_001_100.getX_i(), vec_001_010.getX_i()), -1);
//		System.out.println(vec_001_100);
//		System.out.println(vec_001_010);
//		System.out.println(planeNormPositive);
		
//		System.out.println("Nu from input= "+nu);
//		nu=0.999f;//(float) Math.sqrt(2);//1.0f;//TODO remove this test line
//		System.out.println("Nu hardcoded = "+nu);
		
		PointND point001ScaledToC = new PointND("REF", new float[] {0,0,1}, -1);
		JOpinionsCLI.moveToPole(point001ScaledToC, true, nu);
		PointND point010ScaledToC = new PointND("REF", new float[] {0,1,0}, -1);
		JOpinionsCLI.moveToPole(point010ScaledToC, true, nu);
		PointND boundaryVecC1 = new PointND("VEC", point010ScaledToC.minus(point001ScaledToC), -1);
		
		PointND point010ScaledToP = new PointND("REF", new float[] {0,1,0}, -1);
		JOpinionsCLI.moveToPole(point010ScaledToP, false, nu);
		PointND point100ScaledToP = new PointND("REF", new float[] {1,0,0}, -1);
		JOpinionsCLI.moveToPole(point100ScaledToP, false, nu);
		PointND point001ScaledToP = new PointND("REF", new float[] {0,0,1}, -1);
		JOpinionsCLI.moveToPole(point001ScaledToP, false, nu);
		PointND boundaryVecP2 = new PointND("VEC", point010ScaledToP.minus(point100ScaledToP), -1);
		PointND boundaryVecP3 = new PointND("VEC", point001ScaledToP.minus(point100ScaledToP), -1);
		//==============Prepare for polarity Calculation End =====================

		boolean cIM;
		//now start calculations
		for (int i = 0; i < states.length; i++) {
			float[][] currentState = states[i];
			//=========CIM Start=======================
			float dist, calculatedCimMaxDist = Float.MIN_VALUE, cachedCimMaxDist, cimMaxDist, ctcMaxDist=Float.MIN_VALUE;
			
			if(currentState == null)
				continue;
			
			for (int j = 0; j < currentState.length; j++) {
				dist = PointND.getDistRawData(currentState[j], prevState[j]);
				if (dist > calculatedCimMaxDist) {
					calculatedCimMaxDist = dist;
				}
			}

			if (cimDistances != null) {// parsed the log file
				if(i == 0) {
					//use cimMaxDist from the calculation
					cimMaxDist = calculatedCimMaxDist;
				}else {
					//use the cached value
					cimMaxDist = cachedCimMaxDist = cimDistances[i];
					if (i == prevStep+1) {//applicable only if we don't skip any steps
						//double check
						if(Math.abs(calculatedCimMaxDist - cachedCimMaxDist) > maxEpsilon) {// TODO check the threshold
							throw new RuntimeException("distance varies much between ("+cachedCimMaxDist+", "+calculatedCimMaxDist+")");
						}
					}
				}
			} else { //couldn't parse or there is no log file
				//use cimMaxDist from the calculation
				cimMaxDist = calculatedCimMaxDist;
			}
			
			cIM = cimMaxDist < Defaults.DEFAULT_CONVERGENCE_PRECISION;
			
			prevState = currentState;
			prevStep = i;
			//use cimMaxDist, cIM
			//========= CIM End =======================

			//=========CTC Start=======================
			float dist11, dist12, dist1, dist21, dist22, dist2;
			for (int ii = 0; ii < n; ii++) {
				for (int jj = 0; jj < n; jj++) {
					float[] ci=currentState[ii];
					float[] cj=currentState[jj];
					float[] pi=currentState[ii+n];
					float[] pj=currentState[jj+n];

					dist11 = PointND.getDistRawData(ci,cj);
					dist12 = PointND.getDistRawData(pi,pj);
					dist1 = dist11+dist12;
					
					dist21 = PointND.getDistRawData(ci,pj);
					if(dist21 > dist1) continue;
					dist22 = PointND.getDistRawData(pi,cj);
					dist2 = dist21+dist22;
					dist = Math.min(dist1, dist2);
					if (dist > ctcMaxDist) {
						ctcMaxDist = dist;
					}
				}
			}
			//use ctcMaxDist
			//========= CTC End =======================

			//=========Openness Start=======================
			float eO, mO, maxO;
			float[] distances = new float[n];
			for (int ii = 0; ii < n; ii++) {
				distances[ii]= PointND.getDistRawData(currentState[ii], currentState[ii+n]);
			}
			Arrays.sort(distances);
			eO = (n%2 != 0) ? distances[n/2] : (distances[n/2]+distances[(n/2)-1])/2;
			maxO= distances[n-1];
			mO=0;
			for (int j = 0; j < distances.length; j++) {
				mO += distances[j];
			}
			mO /= n;
			//use eO, mO, maxO
			//========= Openness End =======================

			// ================ Polar count start ===============================
			int castorPoleCount = 0, polluxPoleCount = 0;
			for (int j = 0; j < currentState.length; j++) {
				float[] queryPointRawData = currentState[j];
//				queryPointRawData = new float[] {0.5f, 0.25f, 0.25f}; //TODO remove this test line
				
				float sideC1 = PointND.side(boundaryVecC1, point001ScaledToC, queryPointRawData, planeNormPositive);
				if(sideC1 <= 0)
					castorPoleCount++;
				
				float[] queryVectorPoint100ScaledToP_queryPoint_RawData = PointND.minusRawData(queryPointRawData, point100ScaledToP.getX_i());
				float sideP2 = PointND.side(boundaryVecP2, queryVectorPoint100ScaledToP_queryPoint_RawData, planeNormPositive);
				float sideP3 = PointND.side(boundaryVecP3, queryVectorPoint100ScaledToP_queryPoint_RawData, planeNormPositive);
				if(sideP2 >= 0 && sideP3 <= 0)
					polluxPoleCount++;

//				System.out.println(nu);
//				System.out.println(sideC1);
//				System.out.println(sideP2);
//				System.out.println(sideP3);
//				System.out.format("%d, %d", castorPoleCount, polluxPoleCount);
//				System.exit(0);//TODO remove this test line
			}
			// ================ Polar count End ===============================

			// start output
//			out.format(Defaults.OUTPUT_FORMAT, data);
			out.format("%d\t",i);
			out.format(floatAndDelimeter, eO);
			out.format(floatAndDelimeter, mO);
			out.format(floatAndDelimeter, maxO);
			
			out.format(floatAndDelimeter, cimMaxDist);
			out.format("%B\t", cIM);
			out.format(floatAndDelimeter, ctcMaxDist);

			out.format("%d\t", castorPoleCount);
			out.format("%d\t", polluxPoleCount);
			out.format("%d", castorPoleCount+polluxPoleCount);//NO extra \t at the end
			out.format("\n");
		}
	}

}
