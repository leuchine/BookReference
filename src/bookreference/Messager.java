package bookreference;

public class Messager {

	public static String CONNECT_FAIL = "{\"status\":500, \"results\": \"connect failure\"}";
	public static String INIT_FAIL = "{\"status\":500, \"results\": \"initialization failure\"}";
	public static String INSERTION_FAIL = "{\"status\":500, \"results\": \"insersion failure\"}";
	public static String SEARCH_FAIL = "{\"status\":400, \"results\": \"search failure\"}";
	public static String SET_LOCATOR_FAIL = "{\"status\":500, \"results\": \"set server locators failure\"}";
	public static String CHANGE_INDEX_FAIL = "{\"status\":500, \"results\": \"change index file failure\"}";
	public static String CLOSE_INDEX_FAIL = "{\"status\":500, \"results\": \"close index writer failure\"}";
	public static String SEARCH_DONE = "200: searching done";
	public static String START_SERVICE_FAIL = "{\"status\":500, \"results\": \"start service failure\"}";
	public static String UNKNOWN_ERROR = "{\"status\":500, \"results\": \"unknown error\"}";
	public static String BAD_REQUEST = "{\"status\":400, \"results\": \"bad request\"}";
	public static String NO_CONTENT = "{\"status\":204, \"results\": \"no content\"}";
	
	public static String setIndex(String id) {
		return "[id="+id.trim()+"]";
	}
}
