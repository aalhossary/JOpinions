package sg.edu.ntu.voting;

import java.util.Arrays;

public interface TieBreakingRule {
	public Integer breakTie(Integer... candidates);
}

class LexicographicTieBreakingRule implements TieBreakingRule{

	public Integer breakTie(Integer... candidates) {
		Integer[] clone = candidates.clone();
		Arrays.sort(clone);
		return clone[0];
	}
//	public static void main(String[] args) {
//		PointND p0 = new PointND("A", null, 0);
//		PointND p1 = new PointND("B", null, 1);
//		PointND p2 = new PointND("C", null, 2);
//		System.out.println(new LexicographicTieBreakingRule().breakTie(new PointND[] {p1, p0, p2}));
//	}
}
