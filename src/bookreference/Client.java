package bookreference;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

public class Client {
	static String string_index = "string index";
	static String passage = "data/annotation_dataset_0.txt";
	static int counter = 0;
	public Param parameter;

	// Top K
	private static int K = 5;

	public static Index index;

	public Client() {
		index = new Index();

	}

	public void setIndexfile(String indexfile) {
		index.setIndexfile(indexfile);
	}

	public Object init(int type) throws Throwable {
		try {
			if (type == Index.STRING_BUILD)
				index.init_building();
			else if (type == Index.STRING_SEARCH)
				index.init_query();
			else
				System.out.println("Initialization error");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Object addDocHandler(long id_long, int part, String value_string,
			String bookid) {

		index.addDoc(id_long, part, value_string, bookid);
		return null;
	}

	public ReturnValue queryHandler(List<QueryConfig> qlist) {
		try {
			return index.generalSearch(qlist);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	public String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return Jsoup.clean(new String(encoded), "", Whitelist.none(),
				new Document.OutputSettings().prettyPrint(false));
	}

	public ArrayList<String> split(String str) {

		ArrayList<String> splits = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			if (sb.length() >= 256
					&& (Character.isSpaceChar(str.charAt(i)) || WordTokenizer
							.isPunctuation(str.substring(i, i + 1)))) {
				sb.append(str.charAt(i));
				splits.add(sb.toString());
				sb = new StringBuilder();
			} else {
				sb.append(str.charAt(i));
			}
		}
		return splits;
	}

	public void updateCounter() {
		try {
			PrintWriter pw = new PrintWriter("counter");
			pw.println(counter);
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void insertBook(String bookid, int pages, String index_file) {
		try {
			counter = Integer.parseInt(readFile("counter",
					StandardCharsets.UTF_8).trim());
		} catch (IOException e1) {

			e1.printStackTrace();
		}
		for (int i = 1; i <= pages; i++) {
			try {
				String str = readFile(i + ".html", StandardCharsets.UTF_8);
				ArrayList<String> splits = split(str);
				for (int j = 0; j < splits.size(); j++) {
					testInsertionString(counter, bookid, splits.get(j),
							index_file);
					counter++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			}

		}
		updateCounter();
	}

	private void testInsertionString(int id, String bookid, String str,
			String index_file) throws Throwable {
		this.setIndexfile(index_file);
		this.init(Index.STRING_BUILD);
		try {

			WordTokenizer wt = new WordTokenizer(new StopWords());
			str = URLDecoder.decode(str, "UTF-8");
			str = Jsoup.clean(str, "", Whitelist.none(),
					new Document.OutputSettings().prettyPrint(false));
			if (str.length() > 255) {
				str = str.substring(0, 255);
			}

			Annotation anno = wt.getIndexAnno(str);
			if (anno.hasFirst()) {
				this.addDocHandler(id, 0, anno.getFirstPart(), bookid);
			}
			if (anno.hasSecond()) {
				this.addDocHandler(id, 1, anno.getSecondPart(), bookid);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(1);
		}
		index.closeWriter();
	}

	private String testDocSearch(String doc, String index_file, int K)
			throws Throwable {
		this.setIndexfile(index_file);
		this.init(Index.STRING_SEARCH);
		WordTokenizer wt = new WordTokenizer(new StopWords());

		BufferedReader buf = new BufferedReader(new InputStreamReader(
				new FileInputStream(doc)));
		String line = new String();
		String qstr = new String();
		while ((line = buf.readLine()) != null) {
			qstr += " " + line;
		}
		buf.close();

		SearchQuery sQuery = null;

		String extension = doc.substring(doc.lastIndexOf(".") + 1);
		if (extension.equalsIgnoreCase("html")) {
			sQuery = wt.processQueryHtmlString(qstr);
		} else {
			sQuery = wt.processQueryStr(qstr);
		}

		if (sQuery == null) {
			System.out.println(Messager.UNKNOWN_ERROR);
			return null;
		}
		List<QueryConfig> configs = new ArrayList<QueryConfig>();

		for (int i = 0; i < sQuery.keywordSpace.size(); i++) {
			String word = sQuery.keywordSpace.get(i);
			QueryConfig config = new STRConfig(0, sQuery, word);
			config.setK(K);
			configs.add(config);
		}

		ReturnValue revalue = queryHandler(configs);
		String results = CandidatesVerifier.verifyCandidates(revalue.sQuery);
		System.out.println(results);
		return results;
	}

	public static void main(String[] args) throws Throwable {
		Client client = new Client();
		client.insertBook("aaa12345", 12, "annotation_index");
		client.testDocSearch("1.html", "annotation_index", 5);
	}
}

class STRConfig extends QueryConfig {

	public STRConfig(int i, SearchQuery sQuery, String string) {
		super(i, QueryConfig.DOC, sQuery, string);
	}

	public STRConfig(int i, String string) {
		super(i, QueryConfig.STRING, string);
	}

	@Override
	public float calcDistance(long a, long b) {
		return 0;
	}

	@Override
	public int getType() {
		return this.type;
	}
}
