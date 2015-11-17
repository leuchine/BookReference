package bookreference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

public class SearchQuery implements Serializable {
	public int totalNum;
	public int numOfKey;
	public long searchTime;
	public long getVector;
	public long comparison;
	public int count;
	public String str;
	public Integer[] words;
	public ArrayList<String> keywordSpace;
	public HashMap<String, Integer> keyToIdMap;
	public ArrayList<Integer> segPos;

	public ArrayList<TermVector<Integer>> querySegs;
	public ArrayList<TermVector<Integer>> halfSegVector;
	public ArrayList<HashMap<Long, TermVector<Integer>>> annos;

	public ArrayList<HashSet<Integer>> keyToSegMap;

	public ArrayList<ArrayList<HashMap<Long, Integer>>> countTable;
	// segId(first/second annoId to count)
	public ArrayList<HashMap<Long, Integer>> annoDim;
	public HashMap<Integer, Integer> segIdMap;
	public HashMap<Integer, String> annoIDtobookIDMap;

	public SearchQuery() {
		this.annoIDtobookIDMap = new HashMap<Integer, String>();
		keywordSpace = new ArrayList<String>();
		keyToIdMap = new HashMap<String, Integer>();
		querySegs = new ArrayList<TermVector<Integer>>();
		halfSegVector = new ArrayList<TermVector<Integer>>();
		keyToSegMap = new ArrayList<HashSet<Integer>>();
		countTable = new ArrayList<ArrayList<HashMap<Long, Integer>>>();
		segPos = new ArrayList<Integer>();

		annos = new ArrayList<HashMap<Long, TermVector<Integer>>>();
		annos.add(new HashMap<Long, TermVector<Integer>>());
		annos.add(new HashMap<Long, TermVector<Integer>>());
		segIdMap = new HashMap<Integer, Integer>();
		count = 0;
		getVector = 0;
		comparison = 0;
	}

	public void initCountTable() {
		for (int i = 0; i < querySegs.size(); i++) {
			countTable.add(new ArrayList<HashMap<Long, Integer>>());
			countTable.get(i).add(new HashMap<Long, Integer>());
			countTable.get(i).add(new HashMap<Long, Integer>());
		}
	}

	public void updateCount(int segID, int part, Long id, int count) {
		countTable.get(segID).get(part).put(id, count);
	}

	public int getCount(int segID, int part, Long id) {
		if (countTable.get(segID).get(part).containsKey(id)) {
			return countTable.get(segID).get(part).get(id);
		} else {
			return 0;
		}
	}

	public int getSegDim(int segId) {
		return querySegs.get(segId).dim;
	}

	public int getKeyFreqInSeg(int segId, int keyId) {
		return querySegs.get(segId).getFreq(keyId);
	}

	public void printString() {
		System.out.println(keywordSpace.toString());
		System.out.println(keyToIdMap.toString());

		System.out.println(querySegs.toString());
		System.out.println(annos.toString());

		System.out.println(keyToSegMap.toString());
		System.out.println(countTable.toString());
	}

	public void printCountTable() {
		for (int i = 0; i < countTable.size(); i++) {
			System.out.println("\nSegID:" + i);
			for (int j = 0; j < 2; j++) {
				System.out.println("\npart:" + j);
				Iterator<Entry<Long, Integer>> iterator = countTable.get(i)
						.get(j).entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Long, Integer> entry = iterator.next();
					System.out.print("[" + entry.getKey() + " "
							+ entry.getValue() + "]");
				}
			}
		}
	}

	public ArrayList<ArrayList<HashSet<Long>>> verifyCandidates() {
		ArrayList<ArrayList<HashSet<Long>>> finalTable = new ArrayList<ArrayList<HashSet<Long>>>();
		int isEmpty = 1;
		int len = 0;

		for (int i = 0; i < countTable.size(); i++) {
			if (isEmpty > 0) {
				finalTable.add(new ArrayList<HashSet<Long>>());
				finalTable.get(len).add(new HashSet<Long>());
				finalTable.get(len).add(new HashSet<Long>());
				isEmpty = 0;
				len++;
			}
			for (int j = 0; j < 2; j++) {
				HashMap<Long, Integer> countMap = countTable.get(i).get(j);
				Iterator<Entry<Long, Integer>> iterator = countMap.entrySet()
						.iterator();
				while (iterator.hasNext()) {
					Entry<Long, Integer> entry = iterator.next();
					if (entry.getValue() == annoDim.get(j).get(entry.getKey())) {
						finalTable.get(len - 1).get(j).add(entry.getKey());
						isEmpty++;
						count++;
						if (!segIdMap.containsKey(len - 1)) {
							segIdMap.put(len - 1, i);
						}
					}
				}
			}
		}
		return finalTable;
	}

	public void combine(SearchQuery sQuery) {
		ArrayList<ArrayList<HashMap<Long, Integer>>> table = sQuery.countTable;
		for (int i = 0; i < countTable.size(); i++) {
			for (int j = 0; j < 2; j++) {
				HashMap<Long, Integer> map = table.get(i).get(j);
				Iterator<Entry<Long, Integer>> iterator = map.entrySet()
						.iterator();
				while (iterator.hasNext()) {
					Entry<Long, Integer> entry = iterator.next();
					Long annoId = entry.getKey();
					if (countTable.get(i).get(j).containsKey(annoId)) {
						int freq = countTable.get(i).get(j).get(annoId);
						if (freq == -1 || entry.getValue() == -1) {
							countTable.get(i).get(j).put(annoId, -1);
						} else {
							countTable.get(i).get(j)
									.put(annoId, freq + entry.getValue());
						}
					} else {
						countTable.get(i).get(j).put(annoId, entry.getValue());
					}
				}
			}
		}
		ArrayList<HashMap<Long, TermVector<Integer>>> annos2 = sQuery.annos;
		for (int i = 0; i < annos2.size(); i++) {
			HashMap<Long, TermVector<Integer>> map = annos2.get(i);
			Iterator<Entry<Long, TermVector<Integer>>> iterator = map
					.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Long, TermVector<Integer>> entry = iterator.next();
				if (annos.get(i).containsKey(entry.getKey())) {
					TermVector<Integer> vector = TermVector.combineVectors(
							entry.getValue(), annos.get(i).get(entry.getKey()));
					annos.get(i).put(entry.getKey(), vector);
				} else {
					annos.get(i).put(entry.getKey(), entry.getValue());
				}
			}
		}

	}
}
