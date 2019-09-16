package sg.edu.ntu.jopinions.control.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Parser {
	private int n;
	private int d = 3;
//	OpinionsMatrix x;
//	Map<Integer, float[][]> states = new LinkedHashMap<>();

	File xFile = null;
	
	/**may contain nulls
	 * @param n
	 * @param d
	 * @param xFile
	 */
	public Parser(int n, int d, File xFile) {
		
		this.n = n;
//		x = new OpinionsMatrix(d,n,false);
		this.xFile = xFile;
	}

	public float[][][] parse() {
		Map<Integer, float[][]> states = new HashMap<>();
		int stepId, maxStepID=Integer.MIN_VALUE;
		Scanner scanner = null;
		try {
			scanner = new Scanner(xFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while (scanner.hasNextLine()) {
			String line = (String) scanner.nextLine();
			if (line.length()== 0 || line.startsWith("#")) {
				continue;
			}
			if (line.startsWith("Step")) {
				stepId = Integer.parseInt(line.substring(7));
				if (stepId > maxStepID) {
					maxStepID = stepId;
				}
			} else {
				throw new IllegalStateException("unexpected line: "+line);
			}
			float[][] stepState = new float[2*n][d];
			for (int i = 0; i < d; i++) {
				line = scanner.nextLine();
				String[] strings = line.trim().split(" +");
				if (strings.length != 2*n) {
					throw new IllegalStateException("unexpected length of line: "+strings.length);
				}
				for (int j = 0; j < strings.length; j++) {
					stepState[j][i]= Float.valueOf(strings[j]);
				}
			}
			states.put(stepId, stepState);
		}
		scanner.close();

		float[][][] ret = new float[maxStepID+1][][];
		for (int i = 0; i < ret.length; i++) {
			ret[i]= states.get(i);//including null values
		}
		return ret;
	}

}
