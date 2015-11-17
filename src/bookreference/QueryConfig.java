package bookreference;

import java.io.Serializable;

public abstract class QueryConfig implements Serializable {

	public int freq;
	public SearchQuery sQuery;
	public int queryId;
	// whether to scan from the initial point of the list
	public boolean needRestart;
	// for aggregation function
	public double weight = 1.0;
	// dimension reduction
	public int num_combination = 1;
	// the type of application
	public static int STRING = 0;
	public static int VECTOR = 1;
	public static int DOC = 2;
	protected int type;
	// for string search
	private String querystr = null;
	// for sift search
	private long querylong = -1;
	private int dim = -1;
	private long dim_value = -1;
	// the range for bi-direction search
	private int up = 0;
	private int down = 0;
	// the range of the value and dimension
	private long value_range = -1;
	private int dim_range = -1;
	public int binary_value_range_length = -1;
	// the length of scanning the list of each key
	private int scanlength = -1;
	// the number of Top K
	private int K = 1;

	/**
	 * abstract functions calculate the distance
	 * */
	public abstract float calcDistance(long a, long b);

	/**
	 * return the query type
	 * */
	public abstract int getType();

	public QueryConfig(int id) {

		this.queryId = id;
		this.needRestart = false;
		this.scanlength = Integer.MAX_VALUE;
	}

	/**
	 * constructors for string query
	 * */
	public QueryConfig(int id, String qstring) {

		this.needRestart = false;
		this.queryId = id;
		this.querystr = qstring;
		this.scanlength = Integer.MAX_VALUE;
	}

	public QueryConfig(int id, int type, SearchQuery sQuery, String qstring) {

		this.needRestart = false;
		this.queryId = id;
		this.scanlength = Integer.MAX_VALUE;
		this.sQuery = sQuery;
		this.querystr = qstring;
		this.type = type;
	}

	public QueryConfig(int id, int type, String qstring) {

		this.needRestart = false;
		this.queryId = id;
		this.scanlength = Integer.MAX_VALUE;
		this.querystr = qstring;
		this.type = type;
	}

	public QueryConfig(int id, String qstring, int scanlength) {

		this.needRestart = false;
		this.queryId = id;
		this.querystr = qstring;
		this.scanlength = scanlength;
	}

	/**
	 * constructor for high-dimension vector query
	 * */
	public QueryConfig(int id, int dim, int values[]) {

		this.needRestart = false;
		this.queryId = id;
		this.dim = dim;
		this.setQuerylong(dim, values);
	}

	// for string search
	public void setQuerystring(String str) {
		this.querystr = str;
	}

	public String getQuerystring() {
		return this.querystr;
	}

	// for sift feature
	public void setQuerylong(long l) {
		this.querylong = l;
	}

	public void setQuerylong(int dim, int[] values) {

		this.querylong = DataProcessor.generateKey(dim, values, values.length
				* this.binary_value_range_length);
		this.dim = dim;
		this.dim_value = DataProcessor.getValue(this.querylong, values.length
				* this.binary_value_range_length);
	}

	public long getQuerylong() {
		return this.querylong;
	}

	// to specify the scan length
	public void setLength(int length) {
		this.scanlength = length;
	}

	public int getLength() {
		return this.scanlength;
	}

	public void setDim(int dim) {
		this.dim = dim;
	}

	public void setDimValue(long value) {
		this.dim_value = value;
	}

	public int getDim() {
		return this.dim;
	}

	public long getDimValue() {
		return this.dim_value;
	}

	public void setDimValueRange(int dim_range, long value_range) {

		this.dim_range = dim_range;
		this.value_range = value_range;
		this.binary_value_range_length = Long.toBinaryString(value_range)
				.length();
	}

	public long getValueRange() {
		return this.value_range;
	}

	public int getDimRange() {
		return this.dim_range;
	}

	/**
	 * set the range for bi-direction search
	 * */
	public void setRange(int down, int up) {
		this.down = down;
		this.up = up;
	}

	public int getUpRange() {
		return this.up;
	}

	public int getDownRange() {
		return this.down;
	}

	public void setK(int k) {
		this.K = k;
	}

	public int getK() {
		return this.K;
	}
}
