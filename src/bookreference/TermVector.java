package bookreference;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public class TermVector<T> implements Serializable {
	public HashMap<T, Integer> keyFreqMap;
	public HashMap<T, Integer> firstOccur;
	public HashMap<T, Integer> lastOccur;
	public int dim;
	public int startPos;
	public int endPos;
	public double len;

	public TermVector() {
		keyFreqMap = new HashMap<T, Integer>();
		firstOccur = new HashMap<T, Integer>();
		lastOccur = new HashMap<T, Integer>();
		startPos = Integer.MAX_VALUE;
		endPos=0;
		dim = 0;
		len = -1;
	}

	public void addWord(T word) {
		int freq;
		if (keyFreqMap.containsKey(word)) {
			freq = keyFreqMap.get(word) + 1;
		} else {
			freq = 1;
			dim++;
		}

		keyFreqMap.put(word, freq);
	}

	public void addWord(T word, int add) {
		int freq;
		if (keyFreqMap.containsKey(word)) {
			freq = keyFreqMap.get(word) + add;
		} else {
			freq = add;
			dim++;
		}
		keyFreqMap.put(word, freq);
	}

	public void updateOccurPos(T key, Integer pos) {
		if (!firstOccur.containsKey(key)) {
			firstOccur.put(key, pos);
			lastOccur.put(key, pos);
		} else {
			lastOccur.put(key, pos);
		}
		
		if(pos<startPos){
			startPos = pos;
		}
		
		if(pos>endPos){
			endPos = pos;
		}
	}

	public void deleteWord(T word) {
		int freq;

		if (!keyFreqMap.containsKey(word)) {
			return;
		}

		freq = keyFreqMap.get(word);

		if (freq == 1) {
			keyFreqMap.remove(word);
			dim--;
		} else {
			keyFreqMap.put(word, freq - 1);
		}
	}

	public double getLen() {
		if (len != -1) {
			return len;
		}

		Iterator<Entry<T, Integer>> iterator = keyFreqMap.entrySet().iterator();
		double lenSquare = 0;

		while (iterator.hasNext()) {
			Entry<T, Integer> entry = iterator.next();
			lenSquare += Math.pow(entry.getValue(), 2);
		}

		len = Math.sqrt(lenSquare);
		return len;
	}

	public boolean isDominatedBy(TermVector<T> vector) {

		Iterator<Entry<T, Integer>> iterator = this.keyFreqMap.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			Entry<T, Integer> entry = iterator.next();

			if (!vector.keyFreqMap.containsKey(entry.getKey())) {
				return false;
			}

			if (vector.getFreq(entry.getKey()) < entry.getValue()) {
				return false;
			}
		}

		return true;
	}

	public int getFreq(T word) {
		if(!keyFreqMap.containsKey(word)){
			return -1;
		}
		return keyFreqMap.get(word);
	}

	public TermVector<T> getMinCover(TermVector<T> vector, T words[]) {
		TermVector<T> result = new TermVector<T>();
		Iterator<Entry<T, Integer>> iterator = vector.keyFreqMap.entrySet()
				.iterator();
		T key = iterator.next().getKey();

		int min = firstOccur.get(key);
		int max = lastOccur.get(key);

		while (iterator.hasNext()) {
			key = iterator.next().getKey();
			
			if (min > firstOccur.get(key)) {
				min = firstOccur.get(key);
			}

			if (max < lastOccur.get(key)) {
				max = lastOccur.get(key);
			}
		}

		for (int i = min; i <= max; i++) {
			result.addWord(words[i]);
		}

		result.startPos = min;
		result.endPos = max;
		return result;
	}

	public TermVector<T> getMinCover(TermVector<T> start, TermVector<T> end,
			T words[]) {
		TermVector<T> result = new TermVector<T>();
		Iterator<Entry<T, Integer>> iterator = this.keyFreqMap.entrySet()
				.iterator();
		T key;
		int min = Integer.MAX_VALUE;
		int max = -1;

		while (iterator.hasNext()) {
			key = iterator.next().getKey();
			if (start.firstOccur.containsKey(key)
					&& min > start.firstOccur.get(key)) {
				min = start.firstOccur.get(key);
			} else if (end.firstOccur.containsKey(key)
					&& min > end.firstOccur.get(key)) {
				min = end.firstOccur.get(key);
			}

			if (end.lastOccur.containsKey(key) && max < end.lastOccur.get(key)) {
				max = end.lastOccur.get(key);
			} else if (start.lastOccur.containsKey(key)
					&& max < start.lastOccur.get(key)) {
				max = start.lastOccur.get(key);
			}
		}

		for (int i = min; i <= max; i++) {
			result.addWord(words[i]);
		}

		result.startPos = min;
		result.endPos = max;
		return result;
	}

	public static TermVector<Integer> combineVectors(TermVector<Integer> first,
			TermVector<Integer> second) {
		Iterator<Entry<Integer, Integer>> iterator = first.keyFreqMap
				.entrySet().iterator();
		TermVector<Integer> result = new TermVector<Integer>();
		while (iterator.hasNext()) {
			Entry<Integer, Integer> entry = iterator.next();
			int freq = entry.getValue();
			if (second.keyFreqMap.containsKey(entry.getKey())) {
				freq += second.keyFreqMap.get(entry.getKey());
			}
			result.keyFreqMap.put(entry.getKey(), freq);
		}

		return result;
	}

	public static double getCosValue(TermVector<Integer> query,
			TermVector<Integer> anno) {
		Iterator<Entry<Integer, Integer>> iterator = anno.keyFreqMap
				.entrySet().iterator();
		double dotProduct = 0;

		while (iterator.hasNext()) {
			Entry<Integer, Integer> entry = iterator.next();

			if (!query.keyFreqMap.containsKey(entry.getKey())) {
				return -1;
			}
		}

		iterator = query.keyFreqMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Integer, Integer> entry = iterator.next();

			if (anno.keyFreqMap.containsKey(entry.getKey())) {
				dotProduct += entry.getValue()
						* anno.keyFreqMap.get(entry.getKey());
			}
		}

		double cosValue = dotProduct / (query.getLen() * anno.getLen());

		return cosValue;
	}

	@Override
	public String toString() {
		return keyFreqMap.toString();
	}
}
