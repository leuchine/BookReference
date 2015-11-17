package bookreference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class will be used by application to call the functions in master node
 * 
 * @author huangzhi
 * */

public class Param implements Serializable {

	// specify the function type
	// format: function name_parameter type
	public static enum FUNCTION_TYPE {
		changeIndexfile_String, connectAllServers_String, initAllServers_int_String, initAllServers_int, addPairs_long_String_int, addPairs, answerStringQuery, answerDocQuery, answerQuery, setBound, getData, closeAllIndexwriters, disconnectAllServers;
	}

	public FUNCTION_TYPE function_type = null;
	// initialization
	public int param_int_type = -1;
	public String param_String_index_file_name = null;
	// insertion
	public List<Long> param_long_elementIDs;
	public long param_long_elementID;
	public List<String> param_String_elementValues;
	public int param_int_dim = -1;
	public int param_int_ndim = -1;
	public List<Integer> param_int_ndims;
	public List<Long> param_long_values;
	public int param_int_value_bi_length = -1;
	public List<Integer> para_anno_part;
	// query
	public QueryConfig[] qconfig = null;
	public int param_int_topK = -1;
	public float param_min_lowerbound = Float.MIN_VALUE;

	// constructor
	Param() {

		param_long_elementIDs = new ArrayList<Long>();
		param_long_values = new ArrayList<Long>();
		param_int_ndims = new ArrayList<Integer>();
		para_anno_part = new ArrayList<Integer>();
		param_String_elementValues = new ArrayList<String>();
	}
}
