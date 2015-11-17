package bookreference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class StopWords {
	private String wordFile = System.getProperty("user.dir") + "/wordlist.txt";
	private HashSet<String> wordList;
	
	public StopWords(){
		wordList = new HashSet<String>();
	}
	
	public HashSet<String> getStopWords(){
		if(wordList.isEmpty()){
			addStopWords();
		}
		return wordList;
	}
	
	private void addStopWords() {
		
		File wordlist = new File(wordFile);
		
		try {
			FileReader fReader = new FileReader(wordlist);
			BufferedReader bReader = new BufferedReader(fReader);

			while (true) {
				String word = bReader.readLine();
				if (word == null) {
					break;
				}
				wordList.add(word);
			}

			bReader.close();
			fReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("500: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("500: " + e.getMessage());
		}
	}
	
}
