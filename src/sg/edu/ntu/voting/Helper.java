package sg.edu.ntu.voting;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import binomialCoefficient.BinCoeffL;

public class Helper<T>{

	public List<List<T>> combine(List<T> n_FullList, int r, List<Long> acceptedSolutionIndices) {
		int n = n_FullList.size();
		List<List<T>> resultContainer = new ArrayList<>();
		if (n <= 0 || n < r)
			return resultContainer;
		List<T> partialItem = new ArrayList<T>();
		dfs(n_FullList, n, r, 0, partialItem, 0, acceptedSolutionIndices, resultContainer);
		return resultContainer;
	}

	private long dfs(List<T> fullList, int n, int r, int start, List<T> partialItem, long indexSoFar, List<Long> acceptedSolutionIndices, List<List<T>> resAccomm) {
		if (partialItem.size() == r) {
			if (acceptedSolutionIndices == null) {
				resAccomm.add(new ArrayList<T>(partialItem));
			}else if (indexSoFar == acceptedSolutionIndices.get(0)) {
				resAccomm.add(new ArrayList<T>(partialItem));
				acceptedSolutionIndices.remove(0);
			}
			return indexSoFar+1;
		}
		for (int i = start; i < n; i++) {
			partialItem.add(fullList.get(i));
			indexSoFar = dfs(fullList, n, r, i + 1, partialItem, indexSoFar, acceptedSolutionIndices, resAccomm);
			if (acceptedSolutionIndices != null && acceptedSolutionIndices.isEmpty()) {
				return indexSoFar;
			}
			partialItem.remove(partialItem.size() - 1);
		}
		return indexSoFar;
	}

	public List<List<T>> giveMeXDifferentCombinations(List<T> n_FullList, int r, int howManyNeeded, Random random){
		final int n = n_FullList.size();
		final long nCr = nCr(n, r);
		List<Long> combinationIndices = new ArrayList<>();
		for (int i = 0; i < howManyNeeded; i++) {
			long nextPotentialIndex = (long) (nCr * random.nextDouble());
			combinationIndices.add(nextPotentialIndex);
		}
		Collections.sort(combinationIndices);
//		System.out.format("CombinationIndices for N=%2d, r=%2d are %s\n", n, r, combinationIndices);

		BinCoeffL binCoeffL = new BinCoeffL(n, r);
		List<List<T>> ret = new ArrayList<>();
		int[] itemKIndexes = new int[r];
		for (Long index : combinationIndices) {
			List<T> combination = new ArrayList<>();
			binCoeffL.getKIndexes(index, itemKIndexes);
			for (int i = 0; i < itemKIndexes.length; i++) {
				combination.add(n_FullList.get(itemKIndexes[i]));
			}
			ret.add(combination);
		}
		return ret;
		
//		return combine(n_FullList, r, acceptedIndeces);
	}

	public static long nCr(int n, int r) {
		if (r > n) {
			throw new IllegalArgumentException(String.format("n, r = %d, %d", n, r));
		}
		BigInteger nPr = nPr(n, n-r);
		return nPr.divide(factorial(r)).longValueExact();
	}

	static BigInteger nPr(int n, int r) {
		if (r > n) {
			throw new IllegalArgumentException(String.format("n, r = %d, %d", n, r));
		}
		BigInteger nPr = BigInteger.ONE;
		// calculate n! / r!
		for (long i = r+1 ; i <= n; i++) {
			nPr = nPr.multiply(BigInteger.valueOf(i));
		}
		return nPr;
	}

	public static BigInteger factorial(int i) {
		BigInteger prodcut = BigInteger.ONE;
		for (long j = 2; j <= i; j++) {
			prodcut = prodcut.multiply(BigInteger.valueOf(j));
		}
		return prodcut;
	}

	public static void main(String[] args) {
		final ArrayList<String> fullList = new ArrayList<String>();
		fullList.add("A");
		fullList.add("B");
		fullList.add("C");
		fullList.add("D");
//		fullList.add("E");
//		fullList.add("F");
		List<List<String>> result = new Helper<String>().combine(fullList,  3, null);
		System.out.println(result);

		for (int i = 0; i < 30; i++) {
			System.out.println(""+i +" "+ factorial(i));
		}
		for (int i = 3; i < 30; i++) {
			System.out.println(""+i +" "+ nPr(i, 3));
		}
		for (int i = 7; i < 30; i++) {
			System.out.println(""+i +" "+ nCr(i, 7));
		}
		
		
		ArrayList<Long> selectedIndeces = new ArrayList<>();
		selectedIndeces.add(0L);
		selectedIndeces.add(2L);
		result = new Helper<String>().combine(fullList,  3, selectedIndeces);
		System.out.println(result);

	}
}