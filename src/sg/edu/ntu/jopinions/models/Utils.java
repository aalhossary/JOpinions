package sg.edu.ntu.jopinions.models;

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


}
