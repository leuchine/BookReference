package bookreference;

import java.io.Serializable;
import java.util.HashSet;

public class Annotation implements Serializable {
	public long id;

	private String firstPart;
	private String secondPart;

	public Annotation() {

	}

	public void setFirstPart(String str) {
		this.firstPart = str;
	}

	public void setSecondPart(String str) {
		this.secondPart = str;
	}

	public String getFirstPart() {
		return this.firstPart;
	}

	public String getSecondPart() {
		return this.secondPart;
	}

	public boolean hasSecond() {
		if (secondPart == null || secondPart.trim().length() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public boolean hasFirst() {
		if (firstPart == null || firstPart.trim().length() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public int getFirstDim() {
		String words[] = firstPart.split(" ");
		HashSet<String> wordSet = new HashSet<String>();
		for (int i = 0; i < words.length; i++) {
			wordSet.add(words[i]);
		}

		return wordSet.size();
	}

	public int getSecondDim() {
		String words[] = secondPart.split(" ");
		HashSet<String> wordSet = new HashSet<String>();
		for (int i = 0; i < words.length; i++) {
			wordSet.add(words[i]);
		}

		return wordSet.size();
	}
}
