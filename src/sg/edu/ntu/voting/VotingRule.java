package sg.edu.ntu.voting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * After all voters set their preferences, the voting rule takes care of finding
 * the winner
 * 
 * @author Amr
 *
 */
public interface VotingRule {

	Integer findWinner(List<Map<Integer, Integer>> allCandidatesScores, TieBreakingRule rule);

}

class PositionalScoringRule implements VotingRule {

	public Integer findWinner(List<Map<Integer, Integer>> allCandidatesScores, TieBreakingRule rule) {
		LinkedHashMap<Integer, Integer> votesPerCandidate = allCandidatesScores.stream()
				.flatMap(map -> (map.entrySet().stream()))
				.collect(
						Collectors.toMap(
								Entry::getKey, 
								Entry::getValue, 
								(a, b) -> a + b, 
								LinkedHashMap<Integer, Integer>::new));
//		System.out.println(votesPerCandidate);
		final Integer[] votesValues = new Integer[votesPerCandidate.size()];
		Arrays.sort(votesPerCandidate.values().toArray(votesValues), (a, b) -> b-a);
		Integer[] toppers = votesPerCandidate.entrySet().stream()
				.filter(e -> e.getValue() == votesValues[0])
				.map(e -> e.getKey()).toArray(Integer[]::new);
		if (toppers.length == 1) {
			return toppers[0];
		} else {
			return rule.breakTie(toppers);
		}
	}

	public static void main(String[] args) {
		LinkedHashMap<Integer, Integer> voter1Pref = new LinkedHashMap<Integer, Integer>();
		LinkedHashMap<Integer, Integer> voter2Pref = new LinkedHashMap<Integer, Integer>();
		LinkedHashMap<Integer, Integer> voter3Pref = new LinkedHashMap<Integer, Integer>();
		
		for (int i = 0; i < 5; i++) {
//			PointND candidate = new PointND(Character.valueOf((char) ('A' + i)).toString(), null, i);
			Integer candidate = Integer.valueOf(i);
			voter1Pref.put(candidate, i + 1);
			voter3Pref.put(candidate, i + 3);
			voter2Pref.put(candidate, i * 2);
		}
		Integer testCandidate1 = 30;
		Integer testCandidate2 = 20;
		voter3Pref.put(testCandidate1, 50);
		voter3Pref.put(testCandidate2, 50);
		ArrayList<Map<Integer, Integer>> allVotersVotes = new ArrayList<>();
		allVotersVotes.add(voter1Pref);
		allVotersVotes.add(voter2Pref);
		allVotersVotes.add(voter3Pref);
		new PositionalScoringRule().findWinner(allVotersVotes, new LexicographicTieBreakingRule());
	}

}
