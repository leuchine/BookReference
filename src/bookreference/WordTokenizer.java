package bookreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

public class WordTokenizer {
	private StringBuilder finalStr;
	private HashSet<String> stopwords;
	public ArrayList<Integer> segPos;

	private int annoLen = 128;
	private int querySegLen = 256;

	public enum charType {
		ENGLISH, SPACE, PUNCT, UNKNOWN, DIGIT
	}

	public WordTokenizer(StopWords words) {
		this.stopwords = words.getStopWords();
		finalStr = new StringBuilder();
		segPos = new ArrayList<Integer>();
	}

	public charType identifyOneCharStr(String oneCharStr) {
		if (oneCharStr.toLowerCase().matches("[a-z]")) {
			// System.out.println("It is a English word");
			return charType.ENGLISH;
		} else if (isPunctuation(oneCharStr)) {
			// System.out.println("It is a punctuation");
			return charType.PUNCT;
		} else if (Character.isSpaceChar(oneCharStr.charAt(0))) {
			// System.out.println("It is a space");
			return charType.SPACE;
		} else if (oneCharStr.toLowerCase().matches("[0-9]")) {
			// System.out.println("It is a space");
			return charType.DIGIT;
		}

		else {
			// System.out.println("It is a chinese char");
			// return charType.CHINESE;
			return charType.UNKNOWN;
		}
	}

	public static boolean isPunctuation(String oneCharStr) {
		if (Pattern.matches("\\p{Punct}", oneCharStr)) {
			// System.out.println("this is a punctuation");
			return true;
		} else {
			// System.out.println("no");
			return false;
		}
	}

	public Annotation getIndexAnno(String input) {
		Annotation anno = new Annotation();
		String str = processString(input);
		String words[] = str.split(" ");
		String first = new String();
		String second = new String();
		if (segPos.size() != words.length) {
//			System.out.println(str);
//			System.out.println(Arrays.toString(words));
//			System.out.println(segPos);
		}
		for (int i = 0; i < segPos.size(); i++) {
			if (segPos.get(i) < annoLen) {
				first += " " + words[i];
			} else if (segPos.get(i) < 2 * annoLen) {
				second += " " + words[i];
			}
		}

		anno.setFirstPart(first.trim());
		anno.setSecondPart(second.trim());

		return anno;
	}

	public String processString(String input) {
		input = input.trim();
//		System.out.println(input);
		int len = input.length();
		boolean isEnglish = false;
		StringBuilder strb = new StringBuilder();
		// System.out.println("");
		if ((!input.isEmpty())
				&& input.substring(0, 1).toLowerCase().matches("[a-z]")) {
			isEnglish = true;
		}

		for (int i = 0; i < len; i++) {
			String oneCharStr = input.substring(i, i + 1);
			// System.out.println("The len is:" + len);
			// System.out.print(i);

			switch (identifyOneCharStr(oneCharStr)) {
			case DIGIT:
			case ENGLISH:
				// oneCharStr = oneCharStr.toLowerCase();
				strb = processEnglishStr(strb, oneCharStr, isEnglish);
				isEnglish = true;
				break;
			case UNKNOWN:
				strb = processChar(strb, oneCharStr, isEnglish, i);
				isEnglish = false;
				break;
			case SPACE:
			case PUNCT:
				strb = processSpecialStr(strb, oneCharStr, isEnglish, i);
				isEnglish = false;
				break;
			default:

			}
		}

		if (isEnglish) {
			String word = strb.toString().trim().toLowerCase();
			if (word.length() != 0 && (!stopwords.contains(word))) {
				finalStr.append(" " + word);
				appendPosition(isEnglish, word, len);

			}
		}

		// System.out.println(finalStr.toString().trim());

		return finalStr.toString().trim();
	}

	private void appendPosition(boolean isEnglish, String word, int pos) {
		if (word == null || word.length() == 0) {
			return;
		}
//		System.out.println("word:" + word);
		if (isEnglish) {
			segPos.add(pos - word.length());
//			System.out.println("pos:" + (pos - word.length()));
		} else {
			segPos.add(pos - word.length() + 1);
//			System.out.println("pos:" + (pos - word.length() + 1));
		}
	}

	public SearchQuery processQueryHtmlString(String htmlString) {
		String text = Jsoup.clean(htmlString, "", Whitelist.none(),
				new Document.OutputSettings().prettyPrint(false));
		return processQueryStr(text);
	}

	public SearchQuery processQueryStr(String qStr) {
		String str = processString(qStr);
		if (str.length() == 0) {
			return null;
		}
		SearchQuery query = new SearchQuery();
		String words[] = str.trim().split(" ");
		// System.out.println(str);
		query.totalNum = words.length;

		int queryLen = qStr.length();
		int totalLen = 0;
		int segID = 0;

		query.querySegs.add(new TermVector<Integer>());
		query.halfSegVector.add(new TermVector<Integer>());
		query.halfSegVector.add(new TermVector<Integer>());
		query.words = new Integer[words.length];
		query.segPos = segPos;
		// System.out.println(0);
		for (int i = 0; i < words.length; i++) {
			int wordID;
			int pos = segPos.get(i);
			if (query.keyToIdMap.containsKey(words[i])) {
				wordID = query.keyToIdMap.get(words[i]);
			} else {
				wordID = query.keywordSpace.size();
				query.keywordSpace.add(words[i]);
				query.keyToIdMap.put(words[i], wordID);
				query.keyToSegMap.add(new HashSet<Integer>());
			}

			query.words[i] = wordID;

			// System.out.print(words[i] + " ");
			if (pos - totalLen >= querySegLen && (queryLen - pos > 256)) {
				totalLen = pos;
				segID++;
				// System.out.println("");
				// System.out.println(segID);
				query.querySegs.add(new TermVector<Integer>());
				query.halfSegVector.add(new TermVector<Integer>());
				query.halfSegVector.add(new TermVector<Integer>());
			}

			if (pos - totalLen < annoLen) {
				query.halfSegVector.get(2 * segID).addWord(wordID);
			} else {
				query.halfSegVector.get(2 * segID + 1).addWord(wordID);
			}

			query.querySegs.get(segID).updateOccurPos(wordID, i);
			query.querySegs.get(segID).addWord(wordID);
			query.keyToSegMap.get(wordID).add(segID);
		}

		query.numOfKey = query.keywordSpace.size();
		// query.printString();
		return query;
	}

	private StringBuilder processSpecialStr(StringBuilder strb,
			String oneCharStr, boolean isEnglish, int pos) {
		String word = new String();
		if (isEnglish) {
			word = strb.toString().trim().toLowerCase();
			if (word.length() != 0 && (!stopwords.contains(word))) {
				finalStr.append(" " + word);
				appendPosition(isEnglish, word, pos);
			}
		}
		strb = new StringBuilder();

		return strb;
	}

	private StringBuilder processChar(StringBuilder strb, String oneCharStr,
			boolean isEnglish, int pos) {
		String word = new String();
		if (isEnglish) {
			word = strb.toString().trim().toLowerCase();
			if (word.length() != 0 && (!stopwords.contains(word))) {
				finalStr.append(" " + word);
				appendPosition(isEnglish, word, pos);
			}

			strb = new StringBuilder();
		}

		strb.append(oneCharStr);
		// System.out.println(strb.length());

		if (strb.length() == 4) {
			word = strb.toString();
			finalStr.append(" " + word);
			appendPosition(isEnglish, word, pos);
			word = word.substring(1);
			strb = new StringBuilder();
			strb.append(word);
		}
		return strb;
	}

	private StringBuilder processEnglishStr(StringBuilder strb,
			String oneCharStr, boolean isEnglish) {
		if (isEnglish) {
			strb.append(oneCharStr);
		} else {
			strb = new StringBuilder();
			strb.append(oneCharStr);
		}
		return strb;
	}
}
