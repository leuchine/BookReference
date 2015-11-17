package bookreference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
//import java.util.List;
import java.util.Map.Entry;

public class CandidatesVerifier {
	public static long important = 0;

	private static double cosBound = 0;

	public CandidatesVerifier() {

	}

	public static String verifyCandidates(SearchQuery query) {
		if (query == null) {
			return Messager.UNKNOWN_ERROR;
		}

		ArrayList<ArrayList<HashSet<Long>>> table = query.verifyCandidates();
		HashMap<Long, Double> cosTable = new HashMap<Long, Double>();
		HashMap<Long, IntegerPair> annoPosMap = new HashMap<Long, IntegerPair>();

		for (int i = 0; i < table.size(); i++) {
			// System.out.println(table.size() + " "+i);
			if (!query.segIdMap.containsKey(i)) {
				continue;
			}

			int segID = query.segIdMap.get(i);
			HashSet<Long> partOne = table.get(i).get(0);
			HashSet<Long> partTwo = table.get(i).get(1);
			Iterator<Long> iterator = partOne.iterator();

			while (iterator.hasNext()) {
				Long annoId = iterator.next();
				// System.out.println(annoId);

				if (!query.annos.get(1).containsKey(annoId)) {
					// if (annoId == 3421)
					// System.out.println(4);
				} else if (partTwo.contains(annoId)) {
					verifyBothPartsDominated(query, cosTable, annoPosMap,
							annoId, segID);
					// if (annoId == 3421)
					// System.out.println(1);
				} else if (segID < query.querySegs.size() - 1) {
					verifyFirstPartDominated(query, cosTable, annoPosMap,
							annoId, segID);
					// if (annoId == 3421)
					// System.out.println(2);
				}
			}

			if (segID > 0) {
				iterator = partTwo.iterator();

				while (iterator.hasNext()) {
					Long annoId = iterator.next();
					// System.out.println(annoId);

					if (!partOne.contains(annoId)) {
						verifySecondPartDominated(query, cosTable, annoPosMap,
								annoId, segID);
						// if (annoId == 3421)
						// System.out.println(3);
					}
				}
			}
		}

		return getAnswerString(annoPosMap, query);
	}

	private static void verifyBothPartsDominated(SearchQuery query,
			HashMap<Long, Double> cosTable,
			HashMap<Long, IntegerPair> annoPosMap, Long annoId, int segID) {
		TermVector<Integer> firstAnnoVector = query.annos.get(0).get(annoId);
		TermVector<Integer> secondAnnoVector = query.annos.get(1).get(annoId);

		if (firstAnnoVector.isDominatedBy(query.halfSegVector.get(segID * 2))
				&& secondAnnoVector.isDominatedBy(query.halfSegVector
						.get(segID * 2 + 1))) {
			updateResults(
					cosTable,
					annoPosMap,
					annoId,
					1.0,
					query.segPos.get(query.querySegs.get(segID).startPos),
					query.segPos.get(query.querySegs.get(segID).endPos),
					query.keywordSpace.get(
							query.words[query.querySegs.get(segID).endPos])
							.length());
		} else {
			TermVector<Integer> annoVector = TermVector.combineVectors(
					firstAnnoVector, secondAnnoVector);

			TermVector<Integer> minCover = query.querySegs.get(segID)
					.getMinCover(annoVector, query.words);

			double cosValue = TermVector.getCosValue(minCover, annoVector);

			if (cosValue > cosBound) {
				updateResults(cosTable, annoPosMap, annoId, cosValue,
						query.segPos.get(minCover.startPos),
						query.segPos.get(minCover.endPos), query.keywordSpace
								.get(query.words[minCover.endPos]).length());
			}
		}
	}

	private static void verifyFirstPartDominated(SearchQuery query,
			HashMap<Long, Double> cosTable,
			HashMap<Long, IntegerPair> annoPosMap, Long annoId, int segID) {
		TermVector<Integer> firstAnnoVector = query.annos.get(0).get(annoId);
		TermVector<Integer> secondAnnoVector = query.annos.get(1).get(annoId);

		TermVector<Integer> annoVector = TermVector.combineVectors(
				firstAnnoVector, secondAnnoVector);
		TermVector<Integer> minCover = annoVector.getMinCover(
				query.querySegs.get(segID), query.querySegs.get(segID + 1),
				query.words);

		double cosValue = TermVector.getCosValue(minCover, annoVector);
		// System.out.println(cosValue);
		if (cosValue > cosBound) {
			updateResults(cosTable, annoPosMap, annoId, cosValue,
					query.segPos.get(minCover.startPos),
					query.segPos.get(minCover.endPos),
					query.keywordSpace.get(query.words[minCover.endPos])
							.length());
		}
	}

	private static void verifySecondPartDominated(SearchQuery query,
			HashMap<Long, Double> cosTable,
			HashMap<Long, IntegerPair> annoPosMap, Long annoId, int segID) {

		TermVector<Integer> firstAnnoVector = query.annos.get(0).get(annoId);
		TermVector<Integer> secondAnnoVector = query.annos.get(1).get(annoId);

		if (secondAnnoVector != null && firstAnnoVector != null) {
			TermVector<Integer> annoVector = TermVector.combineVectors(
					firstAnnoVector, secondAnnoVector);
			TermVector<Integer> minCover = annoVector.getMinCover(
					query.querySegs.get(segID - 1), query.querySegs.get(segID),
					query.words);

			double cosValue = TermVector.getCosValue(minCover, annoVector);
			// System.out.println(cosValue);
			if (cosValue > cosBound) {
				updateResults(cosTable, annoPosMap, annoId, cosValue,
						query.segPos.get(minCover.startPos),
						query.segPos.get(minCover.endPos), query.keywordSpace
								.get(query.words[minCover.endPos]).length());
				// System.out.println("second" + segID);
			}
		}
	}

	private static void updateResults(HashMap<Long, Double> cosTable,
			HashMap<Long, IntegerPair> annoPosMap, Long annoId,
			double cosValue, int start, int end, int len) {
		if ((!cosTable.containsKey(annoId))
				|| (cosTable.get(annoId) < cosValue)) {
			cosTable.put(annoId, cosValue);
			annoPosMap.put(annoId, new IntegerPair(start, end + len - 1));
		}
	}

	public static String getAnswerString(HashMap<Long, IntegerPair> annoPosMap,
			SearchQuery query) {
		StringBuilder strb = new StringBuilder();

		if (annoPosMap.size() == 0) {
			strb.append("{\"status\":500, \"results\": \"no results\"}");
			return strb.toString();
		}

		strb.append("{\"status\":200, \"results\": [");
		Iterator<Entry<Long, IntegerPair>> iterator = annoPosMap.entrySet()
				.iterator();
		HashSet<String> bookids = new HashSet<String>();
		if (iterator.hasNext()) {
			Entry<Long, IntegerPair> entry = iterator.next();
			int key = Integer.parseInt(entry.getKey().toString());
			if (bookids.contains(query.annoIDtobookIDMap.get(key))) {

			} else {
				strb.append("{\"bid\":\"" + query.annoIDtobookIDMap.get(key)
						+ "\"}");
				bookids.add(query.annoIDtobookIDMap.get(key));
			}

		}

		while (iterator.hasNext()) {
			Entry<Long, IntegerPair> entry = iterator.next();
			int key = Integer.parseInt(entry.getKey().toString());
			if (bookids.contains(query.annoIDtobookIDMap.get(key))) {

			} else {
				strb.append("{\"bid\":\"" + query.annoIDtobookIDMap.get(key)
						+ "\"}");
				bookids.add(query.annoIDtobookIDMap.get(key));
			}

		}

		strb.append("]}");
		return strb.toString();
	}

	public static String getSearchQueryFromFile(String filename) {
		String query = new String();
		File wordlist = new File(filename);
		FileReader fReader;
		try {
			fReader = new FileReader(wordlist);
			BufferedReader bReader = new BufferedReader(fReader);

			while (true) {
				String word = bReader.readLine();
				if (word == null) {
					break;
				} else {
					query += (word + " ");
				}
			}

			bReader.close();
			fReader.close();

			return query.toString();
		} catch (FileNotFoundException e) {
			System.out.println("500: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("500: " + e.getMessage());
		}

		return null;
	}

}