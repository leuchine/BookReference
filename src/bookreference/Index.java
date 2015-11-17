package bookreference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.IntegerEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttributeImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

/**
 * building the inverted with the interface of Lucene scanning the inverted list
 * 
 * @author huangzhi
 * */

public class Index {

	// for debug
	static boolean debug = false;

	// for testing
	static boolean test = true;
	static long init_time = 0;
	static long searching_time = 0;
	static long scanning_value_list_time = 0;
	static long scanning_key_list_time = 0;
	static long calc_time = 0;
	static long write_time = 0;
	static int num_build = 0;
	static int num_query = 0;

	// for general use
	public static int VECTOR_BUILD = 1;
	public static int STRING_BUILD = 2;
	public static int STRING_SEARCH = 3;
	public static int VECTOR_SEARCH = 4;
	// used in previous version, @deprecated now
	public static int BUILD = STRING_BUILD;

	private File indexFile;
	private String mapfilename = "key_enum.map";
	private PayloadAnalyzer payload_analyzer;
	private MMapDirectory MMapDir;
	private IndexWriterConfig config;
	private IndexWriter MMwriter;
	private IndexReader indexReader;
	private AtomicReader areader;
	private String fieldname1 = "DocumentID";
	private String fieldname2 = "ElementValue";
	private String fieldname3 = "Data";
	private String fieldname4 = "AnnoPart";
	private String fieldname5 = "BookID";
	private Field id_field;
	private Field value_field;
	private Field data_field;
	private Field part_field;
	private Field test_field;
	private Field bookid_field;
	private PayloadAttributeImpl payload;
	// store the search position of value list enumerator
	private HashMap<String, DocsEnum> position_map;
	// store the search position of value list enumerator for bi-direction
	// search
	// 0 is first , 1 is second, Map:id to dim
	private ArrayList<HashMap<Long, Integer>> annoDim;
	private HashMap<String, long[]> bi_position_map;
	// map the lucene id and doc id
	private long[] idmap;
	// map the key and posting list entrance
	private HashMap<String, DocsAndPositionsEnum> enum_map;
	private boolean initflag = false;
	// Maximum Buffer Size
	private int MAX_BUFF = 48;
	// number of dimensions to be combined
	private static int NUM_COMBINATION = 1;
	// number of total dimension
	private static int DIM_RANGE = 0;
	// Top K
	private static int K = 0;

	// expand direction
	private static int UPWARDS = -1;
	private static int DOWNWARDS = 1;

	// to create the TextField for vector insertion
	private StringBuffer strbuf;
	// to create the data_field
	private StringBuffer databuf;

	public Index() {
	}

	public synchronized void setIndexfile(String indexfilename) {

		this.indexFile = new File(indexfilename);
//		System.out.println("The Index File is set: " + indexfilename);
	}

	/**
	 * initialization for building the index
	 * 
	 * @throws Throwable
	 * */
	public synchronized void init_building() throws Throwable {

		// PayloadAnalyzer to map the Lucene id and Doc id
		payload_analyzer = new PayloadAnalyzer(new IntegerEncoder());
		// MMap
		MMapDir = new MMapDirectory(indexFile);
		// set the configuration of index writer
		config = new IndexWriterConfig(Version.LUCENE_45, payload_analyzer);
		config.setRAMBufferSizeMB(MAX_BUFF);
		// the index configuration
//		if (test) {
//			System.out.println("Max Docs Num:\t" + config.getMaxBufferedDocs());
//			System.out.println("RAM Buffer Size:\t"
//					+ config.getRAMBufferSizeMB());
//			System.out.println("Max Merge Policy:\t" + config.getMergePolicy());
//		}
		// use Memory Map to store the index

		config.setSimilarity(new mySimilarity());
		MMwriter = new IndexWriter(MMapDir, config);

		id_field = new LongField(this.fieldname1, -1, Field.Store.YES);
		// value_field = new TextField(this.fieldname2, "-1", Field.Store.YES);
		data_field = new TextField(this.fieldname3, "", Field.Store.NO);
		part_field = new LongField(this.fieldname4, 0, Field.Store.YES);
		// id_field = new FloatField(this.fieldname1, -1, Field.Store.NO);
		test_field = new StringField("test", "", Field.Store.YES);
		bookid_field = new TextField(this.fieldname5, "", Field.Store.YES);
		FieldType vectorStored = new FieldType();
		vectorStored.setIndexed(true);
		vectorStored.setTokenized(true);
		vectorStored.setStored(true);
		vectorStored.setStoreTermVectors(true);
		vectorStored.setStoreTermVectorPositions(true);

		value_field = new Field(this.fieldname2, "-1", vectorStored);

		strbuf = new StringBuffer();
		databuf = new StringBuffer();

		annoDim = new ArrayList<HashMap<Long, Integer>>();
		annoDim.add(new HashMap<Long, Integer>());
		annoDim.add(new HashMap<Long, Integer>());
		readAnnoDim();
	}

	/**
	 * add a document and build the index the document contains two string
	 * fields
	 * 
	 * @param id
	 *            the element id
	 * @param v
	 *            the value
	 * @param mapping
	 * 
	 * */
	public synchronized void addDoc(long id, int part, String v, String bookid) {
		Document doc = new Document();

		id_field.setLongValue(id);
		value_field.setStringValue(v);
		data_field.setStringValue("ID|" + id);
		part_field.setLongValue(part);
		test_field.setStringValue(Long.toString(id) + "|"
				+ Integer.toString(part));
		bookid_field.setStringValue(bookid);
		doc.add(id_field);
		doc.add(value_field);
		doc.add(data_field);
		doc.add(part_field);
		doc.add(test_field);
		doc.add(bookid_field);
		try {
			updateAnnoDim(id, part, v);
			MMwriter.updateDocument(new Term("test", Long.toString(id) + "|"
					+ Integer.toString(part)), doc);
//			System.out.println("add " + id);
		} catch (Exception e) {
			System.err.println("index writer error");
			if (debug)
				e.printStackTrace();
		}
	}

	private synchronized void updateAnnoDim(long id, int part, String v) {
		String words[] = v.split(" ");
		HashSet<String> wordSet = new HashSet<String>();
		for (int i = 0; i < words.length; i++) {
			wordSet.add(words[i]);
		}

		int dim = wordSet.size();

		annoDim.get(part).put(id, dim);
		recordNumOfKey();
	}

	private synchronized void recordNumOfKey() {
		String fileName = System.getProperty("user.dir") + "/annoDim";
		File record = new File(fileName);
		try {
			if (record.exists()) {
				record.delete();
			}

			record.createNewFile();

			FileWriter fWriter = new FileWriter(record, true);

			for (int i = 0; i < annoDim.size(); i++) {
				Iterator<Entry<Long, Integer>> iterator = annoDim.get(i)
						.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Long, Integer> entry = iterator.next();
					fWriter.write(entry.getKey() + " " + i + " "
							+ entry.getValue() + "\n");
				}
			}

			fWriter.close();
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	private synchronized void readAnnoDim() {
		File file = new File(System.getProperty("user.dir") + "/annoDim");
		FileReader fReader;

		try {
			fReader = new FileReader(file);
			BufferedReader bReader = new BufferedReader(fReader);
			String line;

			while (true) {
				line = bReader.readLine();
				if (line == null) {
					break;
				}
				String tokens[] = line.split(" ");
				Long id = Long.valueOf(tokens[0]);
				int part = Integer.valueOf(tokens[1]);
				int dim = Integer.valueOf(tokens[2]);

				annoDim.get(part).put(id, dim);
			}

			bReader.close();
			fReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("500: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("500: " + e.getMessage());
		}
	}

	/**
	 * initialize the query process
	 * */
	public synchronized void init_query() throws Throwable {
		if (initflag == true)
			return;
		else
			initflag = true;
		if (indexReader != null)
			indexReader.close();
		if (areader != null)
			areader.close();

		indexReader = DirectoryReader.open(MMapDirectory.open(indexFile));
		// change the reader
		areader = SlowCompositeReaderWrapper.wrap(indexReader);
		position_map = new HashMap<String, DocsEnum>();
		bi_position_map = new HashMap<String, long[]>();

		// map the lucene id and doc id
		idmap = new long[indexReader.maxDoc()];
		Term term = new Term(this.fieldname3, "ID");
		DocsAndPositionsEnum dp = areader.termPositionsEnum(term);
		int lucene_id = -1, doc_id = -1;
		BytesRef buf = new BytesRef();
		while ((lucene_id = dp.nextDoc()) != DocsAndPositionsEnum.NO_MORE_DOCS) {
			dp.nextPosition();
			buf = dp.getPayload();
			doc_id = PayloadHelper.decodeInt(buf.bytes, buf.offset);
			idmap[lucene_id] = doc_id;
		}

		annoDim = new ArrayList<HashMap<Long, Integer>>();
		annoDim.add(new HashMap<Long, Integer>());
		annoDim.add(new HashMap<Long, Integer>());
		readAnnoDim();
		mapKeyEnum();
	}

	/**
	 * This function is used to map the key and the entrance of its posting
	 * list. This function is not scalable. If the number of the key is too
	 * large,the map will cost too much space. Maybe there will be
	 * OutOfMemoryExceptions. After building the index, we write the map to
	 * disk.
	 * */
	private synchronized void mapKeyEnum() {

		long start = System.currentTimeMillis();
//		System.out.println("Starting mapping the keys...");
		enum_map = new HashMap<String, DocsAndPositionsEnum>();
		try {
			Terms terms = areader.terms(this.fieldname2);
			TermsEnum te = null;
			te = terms.iterator(te);
			String keystring;
			Bits liveDocs = areader.getLiveDocs();
			while (te.next() != null) {
				keystring = te.term().utf8ToString();
				DocsAndPositionsEnum enumer = te.docsAndPositions(liveDocs,
						null);
				// store the index of key strings
				enum_map.put(keystring, enumer);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out
					.println("There some problems in mapping the key and Enum");
			// if(debug)
			e.printStackTrace();
		}
		init_time = (System.currentTimeMillis() - start);
		// System.out.println("Mapping Done! Time:\t" + init_time + " ms");

	}

	public synchronized ReturnValue generalSearch(List<QueryConfig> qlist)
			throws Throwable {

		ReturnValue result = new ReturnValue();
		ReturnValue revalue = new ReturnValue();
		K = qlist.get(0).getK();
		// if we search the vector
		SearchQuery query = qlist.get(0).sQuery;
		// query.printString();
		query.annoDim = annoDim;
		query.initCountTable();
		long start = System.currentTimeMillis();
		long searchTime = 0;
		for (int i = 0; i < qlist.size(); i++) {
			searchTime += searchByKeywords(qlist.get(i).getQuerystring(), query);
		}

		result.sQuery = query;
		result.sQuery.searchTime = searchTime;
		long time = System.currentTimeMillis() - start;
		result.time = time;
		// System.out.println(time);
		return result;
	}

	public synchronized long searchByKeywords(String key, SearchQuery query)
			throws Exception {
		IndexSearcher searcher = new IndexSearcher(areader);
		searcher.setSimilarity(new mySimilarity());
		long start = System.currentTimeMillis();
		Term term = new Term(fieldname2, key);
		TermQuery termQuery = new TermQuery(term);
		TopDocs topdocs = searcher.search(termQuery, Integer.MAX_VALUE);
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
		// System.out.println("We have found all these results" +
		// scoreDocs.length);
		long total = System.currentTimeMillis() - start;
		long start2 = System.currentTimeMillis();
		for (int i = 0; i < scoreDocs.length; i++) {
			int doc = scoreDocs[i].doc;
			long id = idmap[doc];

			int freq = (int) scoreDocs[i].score;
			int keyId = query.keyToIdMap.get(key);
			start = System.currentTimeMillis();
			int part = Integer.valueOf(areader.document(doc).get(fieldname4)
					.toString());
			String bookid = areader.document(doc).get(fieldname5).toString();
			query.annoIDtobookIDMap.put((int) id, bookid);
			query.getVector += (System.currentTimeMillis() - start);

			if (query.annos.get(part).containsKey(id)) {
				query.annos.get(part).get(id).addWord(keyId, freq);
			} else {
				query.annos.get(part).put(id, new TermVector<Integer>());
				query.annos.get(part).get(id).addWord(keyId, freq);
			}

			Iterator<Integer> iterator = query.keyToSegMap.get(keyId)
					.iterator();
			while (iterator.hasNext()) {
				int segId = iterator.next();
				int count;
				count = query.getCount(segId, part, id);
				if (count == -1 || count == -2) {
					continue;
				}

				if (annoDim.get(part).get(id) > query.getSegDim(segId)) {
					query.updateCount(segId, part, id, -1);
					continue;
				}

				int freqInSeg = query.getKeyFreqInSeg(segId, keyId);

				if (freqInSeg < freq) {
					query.updateCount(segId, part, id, -2);
				} else {
					query.updateCount(segId, part, id, count + 1);
				}
			}
		}

		query.comparison += (System.currentTimeMillis() - start2);
		return total;
	}

	/**
	 * calculate the bounds and get candidates
	 * */
	private synchronized Candidates getCandidates(ReturnValue revalue,
			int dim_range, int k) {

		List<Map.Entry<Long, float[]>> list = new ArrayList<Map.Entry<Long, float[]>>(
				revalue.table.entrySet());
		Candidates candidate = new Candidates();
		// calculate the bounds for each element
		// build a priority queue to get the minimum k elements
		QueueElement element[] = new QueueElement[list.size()];
		for (int i = 0; i < list.size(); i++)
			element[i] = new QueueElement();
		// create a maximum priority queue
		PriorityQueue<QueueElement> queue = new PriorityQueue<QueueElement>(k,
				new UpperboundComparator());
		// O(n*log(k))
		for (int i = 0; i < list.size(); i++) {
			Entry<Long, float[]> entry = list.get(i);
			float[] count_dis = entry.getValue();
			element[i].element_id = entry.getKey();
			// upper bound = distance + (dim_range - count) * max_dis
			element[i].upper_bound = count_dis[1] + (dim_range - count_dis[0])
					* revalue.max_dis;
			// lower bound = distance + (dim_range - count) * min_dis
			element[i].lower_bound = count_dis[1] + (dim_range - count_dis[0])
					* revalue.min_dis;
			// maintain the priority queue
			// insert k element into the queue
			if (i < k)
				queue.add(element[i]);
			// if current element's upper bound smaller than the largest upper
			// bound in the queue
			// then delete the largest upper bound and insert current element
			else if (i >= k
					&& element[i].upper_bound < queue.peek().upper_bound) {
				queue.poll();
				queue.add(element[i]);
			}
		}
		candidate.elements_min_lowerbound = new QueueElement[1];
		candidate.elements_min_upperbound = new QueueElement[queue.size()];

		// hash map is used to find the element with the minimum lower bound
		HashMap<Long, Long> id_map = new HashMap<Long, Long>();
		for (int i = 0; i < k; i++) {
			candidate.elements_min_upperbound[i] = new QueueElement();
			candidate.elements_min_upperbound[i] = queue.poll();
			id_map.put(candidate.elements_min_upperbound[i].element_id, null);
		}

		// find the one with minimum lower bound
		float min_bound = Float.MAX_VALUE;
		candidate.elements_min_lowerbound[0] = new QueueElement();
		for (int i = 0; i < element.length; i++) {
			if (id_map.containsKey(element[i].element_id) == false
					&& element[i].lower_bound < min_bound) {
				min_bound = element[i].lower_bound;
				candidate.elements_min_lowerbound[0] = element[i];
			}
		}
		return candidate;
	}

	/**
	 * random access we can get data by providing the id
	 * */
	public synchronized String getData(long id) {

		// the enumerator of value list
		DocsEnum value_enum;
		// create a term for query
		// It is much easier to create a term for text.
		// To create a term for long, we have to get the prefix code first
		BytesRef bytesref = new BytesRef();
		// the code is stored in a BytesRef object
		NumericUtils.longToPrefixCoded(id, 0, bytesref);
		Term qterm = new Term(fieldname1, bytesref);
		try {
			value_enum = areader.termDocsEnum(qterm);
			// return
			// indexReader.document(value_enum.nextDoc()).getField(fieldname3).stringValue();
			return indexReader.document(value_enum.nextDoc())
					.getField(fieldname2).stringValue();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * calculate the actual distance for local TopK
	 * */
	private float[] getDistances(long ids[], List<QueryConfig> qlist) {

		float distances[] = new float[ids.length];
		String values[];
		int actual_value;
		for (int i = 0; i < ids.length; i++) {
			values = this.getData(ids[i]).split(" ");
			for (int j = 0; j < values.length; j++) {
				// extract the actual value from the number (dim%256+value)
				if (j == 0)
					actual_value = Integer.valueOf(values[j]);
				else
					actual_value = Integer.valueOf(values[j]) % (j << 8);
				distances[i] += qlist.get(j).calcDistance(
						qlist.get(j).getDimValue(), actual_value);
			}
		}

		return distances;
	}

	public synchronized void scanList(Long querylong, QueryConfig config,
			ReturnValue revalue) throws Throwable {

		if (debug)
			System.out.println("Scanning List: " + querylong);

		long start_time = 0;
		if (test)
			start_time = System.currentTimeMillis();

		DocsAndPositionsEnum enumer = null;
		String keystring = String.valueOf(querylong);

		if (enum_map.containsKey(keystring)) {
			enumer = enum_map.get(keystring);
		}
		// else {
		// // search in the key list
		// Term queryterm = new Term(fieldname2, keystring);
		// // search for the list entrance
		// enumer = areader.termPositionsEnum(queryterm);
		// }
		// the time to scan the key list, should be O(log(n))
		if (test)
			Index.scanning_key_list_time += (System.currentTimeMillis() - start_time);

		if (test)
			start_time = System.currentTimeMillis();

		// all the elements has the same distance, so we have to calculate only
		// once
		float distance = config.calcDistance(config.getDimValue(),
				DataProcessor.getValue(querylong,
						config.binary_value_range_length
								* config.num_combination));

		// the time to calculate the distance
		if (test)
			Index.calc_time += (System.currentTimeMillis() - start_time);

		if (enumer == null) {
			if (debug) {
				System.out.println("Not Found: " + querylong);
			}
			return;
		}
		int lucene_doc_id = -1;
		// scanning
		if (test)
			start_time = System.currentTimeMillis();

		BytesRef buf = new BytesRef();
		long doc_id;

		while ((lucene_doc_id = enumer.nextDoc()) != DocsAndPositionsEnum.NO_MORE_DOCS) {
			doc_id = idmap[lucene_doc_id];
			float count_dis[] = new float[2];
			// update the table
			count_dis[0] = 1;
			count_dis[1] = distance;
			revalue.table.put(doc_id, count_dis);
		}

		if (test)
			Index.scanning_value_list_time += (System.currentTimeMillis() - start_time);
	}

	/**
	 * create keys for bi-direction expand
	 * */
	private long createKey(long ori, int length, int direction,
			QueryConfig config) {

		long key = -1;
		if (direction == UPWARDS)
			key = ori - length;
		else
			key = ori + length;
		// expand in the same dimension
		if (DataProcessor.getDim(key, config.binary_value_range_length
				* config.num_combination) == config.getDim())
			return key;
		else
			return -1;
	}

	public void closeWriter() throws Throwable {
		recordNumOfKey();
		MMwriter.close();
		// MMapDir.close();
		// System.out.println("The index is closed.");
	}

	/**
	 * the following functions serve for the purpose of testing
	 * */
	/*
	 * public static void main(String[] args) throws Throwable {
	 * 
	 * Index index = new Index(); int num = 100; String indexname = "Index_1_" +
	 * num; index.setIndexfile(indexname); index.init_query(); //
	 * index.testPayload();
	 * 
	 * }
	 */

	private synchronized void testPayload() throws Throwable {

		int total = 0;

		for (int i = 0; i < 128; i++) {
			Term term = new Term(fieldname2, String.valueOf(i));
			BytesRef buf = new BytesRef();
			int doc_id = -1, lucene_doc_id;
			DocsAndPositionsEnum dp = areader.termPositionsEnum(term);

			if (dp != null) {
				System.out.print(i + ":\t");
				// mapping the doc id and lucene id
				while ((lucene_doc_id = dp.nextDoc()) != DocsAndPositionsEnum.NO_MORE_DOCS) {

					dp.nextPosition();
					buf = dp.getPayload();
					doc_id = PayloadHelper.decodeInt(buf.bytes, buf.offset);
					total++;
					System.out.print("<" + lucene_doc_id + ", ");
					System.out.print(doc_id + ">  ");
				}
				System.out.println();
			}
		}
		System.out.println(total);

	}
}

class PayloadAnalyzer extends Analyzer {

	private PayloadEncoder encoder;

	PayloadAnalyzer(PayloadEncoder encoder) {
		this.encoder = encoder;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldname,
			Reader reader) {
		// TODO Auto-generated method stub
		Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_45, reader);
		TokenFilter payloadFilter = new DelimitedPayloadTokenFilter(tokenizer,
				'|', encoder);
		return new TokenStreamComponents(tokenizer, payloadFilter);
	}

	public void testPayloadAnalyzer() {
		String text = "ID|-1";
		Analyzer analyzer = new PayloadAnalyzer(new IntegerEncoder());
		Reader reader = new StringReader(text);
		TokenStream ts = null;
		BytesRef br = new BytesRef();
		try {
			ts = analyzer.tokenStream(null, reader);
			ts.reset();
			while (ts.incrementToken()) {
				CharTermAttribute ta = ts.getAttribute(CharTermAttribute.class);
				System.out.println(ta.toString());
				br = ts.getAttribute(PayloadAttribute.class).getPayload();
				System.out.println(PayloadHelper.decodeInt(br.bytes, 0));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

/**
 * The following classes are used to build the priority queue
 * */
class QueueElement {

	public long element_id = -1;
	public float upper_bound = Float.MAX_VALUE;
	public float lower_bound = Float.MIN_VALUE;
}

class UpperboundComparator implements Comparator<QueueElement> {

	// build a maximum queue for upper bound
	@Override
	public int compare(QueueElement o1, QueueElement o2) {
		// TODO Auto-generated method stub
		return (int) (o2.upper_bound - o1.upper_bound);
	}
}

class Candidates {

	boolean isRealTopK = false;
	QueueElement elements_min_upperbound[];
	QueueElement elements_min_lowerbound[];
}

class mySimilarity extends DefaultSimilarity {
	@Override
	public float coord(int overlap, int maxOverlap) {
		return 1;
	}

	@Override
	public float idf(long docFreq, long numDocs) {
		return 1;
	}

	@Override
	public float tf(float freq) {
		return freq;
	}

	@Override
	public float sloppyFreq(int distance) {
		return 1;
	}

	@Override
	public float queryNorm(float sumOfSquaredWeights) {
		return 1;
	}

	@Override
	public float lengthNorm(FieldInvertState state) {
		return 1;
	}
}
