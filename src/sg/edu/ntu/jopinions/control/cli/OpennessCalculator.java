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

import sg.edu.ntu.jopinions.Defaults;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.Utils;

/**
 * @author Amr
 *
 */
public class OpennessCalculator {

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
		out.format("step\tmeanO\tmedianO\tmaxO\tCIMDist\tCIM\tCTCDist\n");
		
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
			
			// start output
//			out.format(Defaults.OUTPUT_FORMAT, data);
			out.format("%d\t",i);
			out.format(floatAndDelimeter, eO);
			out.format(floatAndDelimeter, mO);
			out.format(floatAndDelimeter, maxO);
			
			out.format(floatAndDelimeter, cimMaxDist);
			out.format("%B\t", cIM);
			out.format(floatAndDelimeter, ctcMaxDist);
			out.format("\n");
		}
	}

}
