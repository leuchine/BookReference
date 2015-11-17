package bookreference;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * @author huang zhi this class provides functions to process the data
 * */

public class DataProcessor {

	public static boolean debug = false;

	/**
	 * get ngrams from a input string
	 * */
	static public String getGrams(int ngrams, String instr) {

		instr = addUnderline(instr);
		HashMap<String, String> map = new HashMap<String, String>();
		String grams = "";
		for (int i = 0; i < instr.length() - ngrams + 1; i++) {
			String gram = instr.substring(i, i + ngrams);
			grams += (gram + " ");
			map.put(gram, null);
		}
		return grams;
	}

	/**
	 * combine the grams get the string from grams
	 * */
	static public String combineGrams(int ngrams, String grams) {

		int i = 0, count = 0;
		StringBuffer strbuf = new StringBuffer();
		while (i < grams.length()) {
			// for first ngrams char, record them
			if (i < ngrams) {
				if (grams.charAt(i) == '_')
					strbuf.append(' ');
				else
					strbuf.append(grams.charAt(i));
			}
			// count and get the last char in the gram
			else if (grams.charAt(i) != ' ') {
				count++;
				if (count == ngrams) {
					if (grams.charAt(i) == '_')
						strbuf.append(' ');
					else
						strbuf.append(grams.charAt(i));
					count = 0;
				}
			}
			i++;
		}
		return strbuf.toString();
	}

	/**
	 * dealing with the underline between words
	 * */
	public static String removeUnderline(String line) {
		StringBuffer newstring = new StringBuffer();
		for (int i = 0; i < line.length(); i++)
			if (line.charAt(i) != '_')
				newstring.append(line.charAt(i));
		return newstring.toString();
	}

	public static String addUnderline(String line) {
		StringBuffer newstring = new StringBuffer();
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == ' ')
				newstring.append('_');
			else
				newstring.append(line.charAt(i));
		}
		return newstring.toString();
	}

	/**
	 * get the sift value
	 * */
	public static long getSiftValue(String str, int n_dim) {

		return getSiftValue(Long.valueOf(str), n_dim);
	}

	public static long getSiftValue(long dim_value, int n_dim) {

		long combined_value = 0;
		// specific for 8-bit sift value
		combined_value = (long) (dim_value % Math.pow(256, n_dim));
		return combined_value;
	}

	/**
	 * get the sift dimension
	 * */
	public static int getSiftDim(long dim_value, int n_dim) {

		int dim = -1;
		// specific for 8-bit sift value
		dim = (int) (dim_value / Math.pow(256, n_dim));
		return dim;
	}

	/**
	 * general function to get values from a long integer
	 * 
	 * @param length
	 *            indicates the binary length of combined value
	 * */
	public static long getValue(long dim_value, int length) {

		long value = -1;
		value = (int) (dim_value % Math.pow(2, length));
		return value;
	}

	/**
	 * general function to get dimension from a long integer
	 * 
	 * @param length
	 *            indicates the binary length of combined value
	 * */
	public static int getDim(long dim_value, int length) {

		int dim = -1;
		dim = (int) (dim_value / Math.pow(2, length));
		return dim;
	}

	/**
	 * this function serves for testing purpose
	 * */
	public static int[] get8bitValues(long dim_value, int n_dim) {

		int[] values = new int[n_dim];
		String bi_value = formatString(
				Long.toBinaryString(getSiftValue(dim_value, n_dim)), n_dim * 8);
//		System.out.println(bi_value);
		for (int i = 0; i < n_dim; i++) {
			StringBuffer sbuf = new StringBuffer();
			for (int j = 0; j < 8 * n_dim; j += n_dim) {
				sbuf.append(bi_value.charAt(i + j));
			}
			values[i] = Integer.parseInt(sbuf.toString().trim(), 2);

		}
		if (debug) {
			System.out.println("8-bits value for each dimension:");
			for (int i = 0; i < n_dim; i++)
				System.out.println(values[i]);
		}

		return values;
	}

	/**
	 * format the string to specific binary length 10 => 00000010
	 * */
	public static String formatString(String str, int length) {

		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < length - str.length(); i++)
			buf.append("0");
		return buf.toString() + str;
	}

	/**
	 * Z-order combination combine several integers into one long integer
	 * 
	 * @param length
	 *            : the binary length of the long integer
	 * */
	public static long toLong(int[] values, int length) {

		long valueL = 0;
		int num = values.length;
		String binary_value[] = new String[num];
		for (int i = 0; i < num; i++) {
			// length/num is the binary length of each value
			binary_value[i] = formatString(Integer.toBinaryString(values[i]),
					length / num);
			if (debug) {
				System.out.println(binary_value[i]);
			}
		}
		StringBuffer strbuf = new StringBuffer();
		int bi_length = 0;
		while (bi_length < length) {
			for (int i = 0; i < num; i++) {
				strbuf.append(binary_value[i].charAt(bi_length / num));
				bi_length++;
			}
		}
		valueL = Long.valueOf(strbuf.toString(), 2);
		if (debug) {
			System.out.println(strbuf.toString());
			System.out.println(valueL);
		}
		return valueL;
	}

	/**
	 * generate a long integer as a key for multiple dimensions
	 * */
	public static long generateKey(int dim, int value[], int length) {

		return (long) (dim * Math.pow(2.0, length) + toLong(value, length));
	}

	public static long generateKey(int dim, long value, int length) {

		return (long) (dim * Math.pow(2.0, length) + value);
	}

	/**
	 * combine the sift values on some dimensions
	 * */
	public static long[] combineSiftValues(int all_values[], int length,
			int n_dim) {

		long[] combined_values = new long[length / n_dim];
		int[] values = new int[n_dim];

		for (int i = 0; i < length; i += n_dim) {
			for (int j = 0; j < n_dim; j++) {
				values[j] = all_values[i + j];
			}
			// it is specified to be SIFT value, so the length is 8*n_dim
			combined_values[i / n_dim] = generateKey(i / n_dim, values,
					8 * n_dim);
		}
		if (debug) {
			System.out.println("\r\ncombined result:");
			for (int i = 0; i < combined_values.length; i++) {
				System.out.println(combined_values[i]);
				get8bitValues(combined_values[i], n_dim);
				System.out.println();
			}
		}
		return combined_values;
	}

	/**
	 * create some test data for sift feature
	 * */
	private static void createTestSift(int n) throws IOException {

		FileOutputStream out = new FileOutputStream("data/sift_query.test");
		Random r = new Random();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < 128; j++) {
				out.write((r.nextInt(256) + " ").getBytes());
			}
			out.write("\r\n".getBytes());
		}
	}

	/**
	 * test the functions
	 * */
	public static void main(String a[]) throws Throwable {
		// debug = true;
		// int test[] = new int[4];
		// test[0] = 2;
		// test[1] = 3;
		// test[2] = 10;
		// test[3] = 100;
		// combineSiftValues(test,4,4);
		createTestSift(100);
	}
}
