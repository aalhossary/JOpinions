package sg.edu.ntu.jopinions.models;

import java.util.ArrayList;
import java.util.Iterator;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public final class Utils {
	private Utils() {}
	
	public static float getSum(float[] line) {
		float sum = 0;
		for (float f : line) {
			sum += f;
		}
		return sum;
	}
	public static void scaleLine(float[] line, float scale) {
		for (int i = 0; i < line.length; i++) {
			line[i] *= scale;
		}
	}
	
	/**passing the same parameter several times adds more the new set of values to the first set. e.g. -files f1 f2 f3 -files f4 f5 f6 returns [f1 f2 f3 f4 f5 f6].
	 * @param args
	 * @param parameter parameter to search for
	 * @param defaultValueIfParameterFound
	 * @param valueIfParameterNotFound
	 * @return
	 */
	public static String[] getParameters(String[] args, String parameter, String[] defaultValueIfParameterFound, String[] valueIfParameterNotFound) {
		boolean flagFound=false;
		ArrayList<String> inputValues = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(parameter)) {//multiple inputs
				flagFound = true;
				for (int j = 1; i+j<args.length; j++) {
					String nextArg = args[i+j];
					if (nextArg.startsWith("-")) {
						break;
					} else {
						inputValues.add(nextArg);
					}
				}
				i += inputValues.size();
			}
		}
		if (flagFound) {
			if (inputValues.size() == 0) {
				return defaultValueIfParameterFound;
			}else {
				return inputValues.toArray(new String[inputValues.size()]);
			}
		}else {
			return valueIfParameterNotFound;
		}
	}


	public static String getParameter(String[] args, String parameter, String defaultValueIfParameterFound, String valueIfNotFound) {
		boolean flagFound=false;
		String input= defaultValueIfParameterFound;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(parameter)) {
				flagFound=true;
				if (i+1 >= args.length) {
					break;
				}
				String nextArg = args[i+1];
				if (nextArg.startsWith("-")) {
					break;
				} else {
					input = nextArg;
				}
				i+=1;
			}
		}
		if (!flagFound) {
			return valueIfNotFound;
		}
		return input;
	}

	public static void cacheVerticesDegrees(Graph<PointND, DefaultEdge> graph) {
		Iterator<PointND> iterator = graph.vertexSet().iterator();
		while (iterator.hasNext()) {
			PointND vertex = (PointND) iterator.next();
			vertex.setInDegree(graph.inDegreeOf(vertex));
			vertex.setOutDegree(graph.outDegreeOf((vertex)));
		}
	}


}
