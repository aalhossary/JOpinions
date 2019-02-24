/**
 * 
 */
package sg.edu.ntu.jopinions.control.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sg.edu.ntu.jopinions.models.Utils;

/**
 * @author Amr
 *
 */
public class StatsJoiner {

	/**NOT yet set automatically*/
	File inputFile = null;
	File outputFile = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inFolderString = Utils.getParameter(args, "-inFolder", null, null);
		String outFolderString = Utils.getParameter(args, "-outFolder", null, "./");
		File inFolder = new File(inFolderString);
		File outFile = new File(outFolderString,"allstats.csv");
		PrintStream out = null;
		try {
			out = new PrintStream(outFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-5);
		}
		
		
		String[] statFileNames = inFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.startsWith("stat-") && name.endsWith(".csv"));
			}
		});
		
		Pattern statFileNamePattern = Pattern.compile("stat-(.+(seed=(\\d+)))\\.csv");
		Matcher matcher = statFileNamePattern.matcher("");
		TreeMap<Integer, String> fileIdMap = new TreeMap<>();
		for (int i = 0; i < statFileNames.length; i++) {
			final String fileName = statFileNames[i];
			matcher.reset(fileName);
			matcher.find();
			int seed = Integer.parseInt(matcher.group(3));
			fileIdMap.put(seed, matcher.group(1));
		}
		
		int stat1NumHeaderFields = new StringTokenizer(OpennessCalculator.HEADER, "\\t", false).countTokens();
		
		printHeader(out);
		
		for (Iterator<Entry<Integer, String>> iterator = fileIdMap.entrySet().iterator(); iterator.hasNext();) {
			Entry<Integer, String> entry = (Entry<Integer, String>) iterator.next();
			int seed = entry.getKey();
			String id = entry.getValue();
			
			File stat1File = new File(inFolderString,String.format("stat-%s.csv",id));
			File stat2AvrFile = new File(inFolderString,String.format("stat2avr-%s.csv",id));

			String stat1InitState = null, stat1LastState = null, stat2AvrInitState = null, stat2AvrLastState = null;
			try (Scanner stat1Scanner = new Scanner(stat1File);
					Scanner stat2AvrScanner = new Scanner(stat2AvrFile)){
				
				//we know it has no header
//				Scanner stat2AvrScanner = new Scanner(stat2AvrFile);
				stat2AvrInitState = stat2AvrScanner.nextLine();
				stat2AvrLastState = stat2AvrScanner.nextLine();
				//TODO check compatibility with header
//				long lastStep = Long.valueOf(stat2AvrLastState.split("\\t", 1)[0]);
				String lastStep = stat2AvrLastState.split("\\t", 2)[0];
				
				//we know it has a header. So we check compatibility (once only)
//				stat1Scanner = new Scanner(stat1File);
				String stat1Header = stat1Scanner.nextLine();
				final int actualStat1NumHeaderFields = new StringTokenizer(stat1Header, "\\t", false).countTokens();
				if(stat1NumHeaderFields != actualStat1NumHeaderFields)
					throw new RuntimeException(String.format("Unexpected Number of Fields: expected header=%d fields, here = %d", stat1NumHeaderFields, actualStat1NumHeaderFields));
				
				while (stat1Scanner.hasNextLine()) {
					String stat1Line = (String) stat1Scanner.nextLine().trim();
					if (stat1Line.length() == 0 || stat1Line.startsWith("#")) {
						continue;
					}else if (stat1Line.startsWith("0")) {
						stat1InitState = stat1Line;
					}else if (stat1Line.startsWith(lastStep)) {
						stat1LastState = stat1Line;
					}
//					else {
//						throw new RuntimeException("unexpected line:\\n"+stat1Line);
//					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			String[] id2Args = Utils.id2Args(id);

			out.print(seed); out.print("\t");
			out.print(Utils.getParameter(id2Args, "-model", null, "NA")); out.print("\t");
			out.print(Utils.getParameter(id2Args, "-numCouples", null, "NA")); out.print("\t");
			out.print(Utils.getParameter(id2Args, "-stubborn", null, "NA")); out.print("\t");
			out.print(Utils.getParameters(id2Args, "-manageStubborn", (String[]) null, new String[] {"NA", "NA"})[0]); out.print("\t");
			out.print(Utils.getParameters(id2Args, "-manageStubborn", (String[]) null, new String[] {"NA", "NA"})[1]); out.print("\t");
			out.print(Utils.getParameter(id2Args, "-ego", null, "NA")); out.print("\t");
			out.print(Utils.getParameter(id2Args, "-phi", null, "NA")); out.print("\t");
			out.print(Utils.getParameter(id2Args, "-beta", null, "NA")); out.print("\t");
			out.print(Utils.getParameter(id2Args, "-topology", null, "NA")); out.print("\t");

			out.print("#\t");

			out.print(stat1InitState); out.print("\t");
			out.print(stat2AvrInitState);
//			out.print("\t");//there is already a tab at the end of the line

			out.print("#\t");

			out.print(stat1LastState); out.print("\t");
			out.print(stat2AvrLastState);

			out.print("\n");
		}
	}
	static void printHeader(PrintStream out) {
		out.print("seed");
		out.print("\t");

//		out.print("ID (split later)");//TODO split later
		out.print("model"); out.print("\t");
		out.print("numCouples"); out.print("\t");
		out.print("stubborn% (rho)"); out.print("\t");
		out.print("manageStubborn"); out.print("\t");
		out.print("nu (eta)"); out.print("\t");
		out.print("ego"); out.print("\t");
		out.print("phi"); out.print("\t");
		out.print("beta"); out.print("\t");
		out.print("topology"); out.print("\t");
		
		out.print("#\t");
		out.print(OpennessCalculator.HEADER);
		out.print("\t");
		out.print(AnxietyCalculator.HEADER);
		out.print("\t");

		out.print("#\t");

		out.print(OpennessCalculator.HEADER);
		out.print("\t");
		out.print(AnxietyCalculator.HEADER);
		out.print("\t");//for consistency

		out.print("\n");
	}
}
