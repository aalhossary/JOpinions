package sg.edu.ntu.jopinions.control.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Parser {
	private int n;
	private int d = 3;
//	OpinionsMatrix x;
	Map<Integer, float[][]> states = new LinkedHashMap<>();

	File xFile = null;
	
	public Parser(int n, int d, File xFile) {
		this.n = n;
//		x = new OpinionsMatrix(d,n,false);
		this.xFile = xFile;
	}

	public Map<Integer, float[][]> parse() {
		try {
			Scanner scanner = new Scanner(xFile);
			while (scanner.hasNextLine()) {
				int stepId;
				String line = (String) scanner.nextLine();
				if (line.length()== 0 || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("Step")) {
					stepId = Integer.parseInt(line.substring(7));
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return states;
	}

}
