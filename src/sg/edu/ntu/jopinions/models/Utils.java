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


}
