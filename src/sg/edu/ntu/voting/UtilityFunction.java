package sg.edu.ntu.voting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface UtilityFunction {

	Map<Integer, Integer> scoreCandidates(Map<Integer, Float> distances);

}

class BordaUtility implements UtilityFunction{

	@Override
	public Map<Integer, Integer> scoreCandidates(Map<Integer, Float> distances) {
		final int distancesSize = distances.size();
		int maxScore = distancesSize -1;
		Map<Integer, Integer> ret = new LinkedHashMap<>(distancesSize);
		List<Integer> orderedKeys = distances.entrySet().stream()
		.sorted((e1, e2) -> (int)(e1.getValue() - e2.getValue()))
		.map(entry -> entry.getKey())
		.collect(Collectors.toList());
		for (int i = 0; i < distancesSize; i++) {
			ret.put(orderedKeys.get(i), maxScore - i);
		}
		return ret;
	}
}

class VetoUtility implements UtilityFunction{
	@Override
	public Map<Integer, Integer> scoreCandidates(Map<Integer, Float> distances) {
		final int distancesSize = distances.size();
		Map<Integer, Integer> ret = new LinkedHashMap<>(distancesSize);
		List<Integer> orderedKeys = distances.entrySet().stream()
		.sorted((e1, e2) -> (int)(e1.getValue() - e2.getValue()))
		.map(entry -> entry.getKey())
		.collect(Collectors.toList());
		for (int i = 0; i < distancesSize-1; i++) {
			ret.put(orderedKeys.get(i), 1);
		}
		ret.put(orderedKeys.get(distancesSize-1), 0);
		return ret;
	}
}

class PluralityUtility implements UtilityFunction{
	@Override
	public Map<Integer, Integer> scoreCandidates(Map<Integer, Float> distances) {
		final int distancesSize = distances.size();
		Map<Integer, Integer> ret = new LinkedHashMap<>(distancesSize);
		List<Integer> orderedKeys = distances.entrySet().stream()
				.sorted((e1, e2) -> (int)(e1.getValue() - e2.getValue()))
				.map(entry -> entry.getKey())
				.collect(Collectors.toList());
		ret.put(orderedKeys.get(0), 1);
		for (int i = 1; i < distancesSize; i++) {
			ret.put(orderedKeys.get(i), 0);
		}
		return ret;
	}
}