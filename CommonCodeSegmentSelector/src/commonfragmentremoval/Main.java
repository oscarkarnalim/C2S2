package commonfragmentremoval;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import support.LibTuple;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// TODO Auto-generated method stub
		execute(args);

	}

	public static void execute(String[] args) throws Exception {
		if (args.length == 0) {
			showHelp();
		} else {
			// start checking
			String mode = args[0];
			if (mode.equalsIgnoreCase("select")) {
				executeSelect(args);
			} else if (mode.equalsIgnoreCase("remove")) {
				executeRemove(args);
			} else {
				System.err.println("The first argument should be either 'select' or 'remove'.");
				System.err.println("Run this software without arguments to show help.");
			}
		}
	}

	public static void executeSelect(String[] args) {
		if (args.length != 4 && !(args.length >= 8 && args.length <= 13)) {
			System.err.println("[Common segment selection]");
			System.err.println("The number of arguments should be either four (quick command)");
			System.err.println("  or in between seven to twelve (complete command).");
			System.err.println("Run this software without arguments to show help.");
			return;
		}

		String input_dirpath = preparePathOrRegex(args[1]);
		if (isPathValidAndExist(input_dirpath) == false) {
			System.err.println("[Common segment selection]");
			System.err.println("<input_dirpath> is not a valid path or refers to a ");
			System.err.println("  nonexistent directory.");
			System.err.println("Run this software without arguments to show help.");
			return;
		}

		boolean temp = isProgrammingLanguageValid(args[2]);
		if (temp == false) {
			System.err.println("[Common segment selection]");
			System.err.println("<programming_language> should be either 'java' (for Java) or 'py'");
			System.err.println("  (for Python).");
			System.err.println("Run this software without arguments to show help.");
			return;
		}
		String programming_language = args[2];

		String output_filepath = preparePathOrRegex(args[3]);
		if (isPathValid(output_filepath) == false) {
			System.err.println("[Common segment selection]");
			System.err.println("<output_filepath> is not a valid path.");
			System.err.println("Run this software without arguments to show help.");
			return;
		}

		if (args.length == 4) {
			// execute with minimum setting
			ArrayList<CommonSegmentTuple> result = CommonSegmentGenerator.execute(input_dirpath, null,
					programming_language, 0.5, 10, 50);

			// write the contents in the output
			try {
				FileWriter fw = new FileWriter(output_filepath);
				for (int i = 0; i < result.size(); i++) {
					CommonSegmentTuple c = result.get(i);
					fw.write(c.getSmartContent() + System.lineSeparator());
				}
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// notify the completion of the process
			System.out.println("The command has been executed succesfully!");
			System.out.println("The result can be seen in '" + output_filepath + "'.");

			return;
		}

		String additional_keywords_path = preparePathOrRegex(args[4]);
		if (additional_keywords_path != null) {
			if (additional_keywords_path.equals("null"))
				additional_keywords_path = null;
			else if (isPathValidAndExist(additional_keywords_path) == false) {
				System.err.println("[Common segment selection]");
				System.err.println("<additional_keywords_path> is not a valid path or refers to");
				System.err.println("  a nonexistent file.");
				System.err.println("Run this software without arguments to show help.");
				return;
			}
		}

		Double tempM = prepareSimThreshold(args[5]);
		if (tempM == null) {
			System.err.println("[Common segment selection]");
			System.err.println("<inclusion_threshold> is not a valid floating number between 0");
			System.err.println("  and 1 (inclusive).");
			System.err.println("Run this software without arguments to show help.");
			return;
		}
		double inclusion_threshold = tempM;

		Integer tempN = prepareMinNGram(args[6]);
		if (tempN == null) {
			System.err.println("[Common segment selection]");
			System.err.println("<min_ngram_length> is not a valid positive integer.");
			System.err.println("Run this software without arguments to show help.");
			return;
		}
		int min_ngram_length = tempN;

		tempN = prepareMaxNGram(args[7], min_ngram_length);
		if (tempN == null) {
			System.err.println("[Common segment selection]");
			System.err.println("<max_ngram_length> is not a valid positive integer, lower than");
			System.err.println("  <min_ngram_range>, or equal to <min_ngram_length>.");
			System.err.println("Run this software without arguments to show help.");
			return;
		}
		int max_ngram_length = tempN;

		// take the remaining parameters
		boolean is_token_generalised = false;
		boolean is_ngram_start_with_identifier = false;
		boolean is_ngram_line_exclusive = false;
		boolean is_subfragment_removed = false;
		boolean is_code_result = false;

		for (int i = 8; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("coderesult"))
				is_code_result = true;
			else if (args[i].equalsIgnoreCase("generalised"))
				is_token_generalised = true;
			else if (args[i].equalsIgnoreCase("startident"))
				is_ngram_start_with_identifier = true;
			else if (args[i].equalsIgnoreCase("lineexclusive"))
				is_ngram_line_exclusive = true;
			else if (args[i].equalsIgnoreCase("subremove"))
				is_subfragment_removed = true;
			else {
				System.err.println("[Common segment selection]");
				System.err.println("'" + args[i] + "' is not a valid argument.");
				System.err.println("  It should be either 'coderesult', 'generalised',");
				System.err.println("  'startident', 'lineexclusive', or 'subremove'.");
				System.err.println("Run this software without arguments to show help.");
				return;
			}
		}

		// execute with the most comprehensive setting
		ArrayList<CommonSegmentTuple> result = CommonSegmentGenerator.execute(input_dirpath, additional_keywords_path,
				programming_language, is_token_generalised, inclusion_threshold, is_ngram_start_with_identifier,
				is_ngram_line_exclusive, min_ngram_length, max_ngram_length, is_subfragment_removed);

		// write the contents in the output
		if (is_code_result) {
			// code output, generated by searching the real implementation of the segments
			// from given code.
			_generateCommonCode(result, programming_language, output_filepath);
		} else {
			// complete output
			try {
				FileWriter fw = new FileWriter(output_filepath);
				for (int i = 0; i < result.size(); i++) {
					CommonSegmentTuple c = result.get(i);
					fw.write(c.getSmartContent() + System.lineSeparator());
				}
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// notify the completion of the process
		System.out.println("The command has been executed succesfully!");
		System.out.println("The result can be seen in '" + output_filepath + "'.");

		return;

	}

	private static void _generateCommonCode(ArrayList<CommonSegmentTuple> result, String extension, String outputFilepath) {
		HashMap<String, ArrayList<CommonSegmentTuple>> sourcePathAndTheSegments = new HashMap<>();
		// get the source paths
		for (int i = 0; i < result.size(); i++) {
			CommonSegmentTuple c = result.get(i);

			// check whether the path exists
			ArrayList<CommonSegmentTuple> segments = sourcePathAndTheSegments.get(c.getFirstSourcePath());
			if (segments == null) {
				// if not, create a new list and attach it in the hashmap with given path
				segments = new ArrayList<CommonSegmentTuple>();
				sourcePathAndTheSegments.put(c.getFirstSourcePath(), segments);
			}

			// add the common segment to given list
			segments.add(c);
		}

		try {
			FileWriter fw = new FileWriter(outputFilepath);
			int segmentCounter = 0;
			
			// per source path, search the actual form of the common segments and write them as the result
			Iterator<Entry<String, ArrayList<CommonSegmentTuple>>> it = sourcePathAndTheSegments.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, ArrayList<CommonSegmentTuple>> cur = it.next();
				String curpath = cur.getKey();
				ArrayList<CommonSegmentTuple> cursegments = cur.getValue();

				// generate the tokens
				ArrayList<LibTuple> tokens = CodeResultGenerator
						.getTokenStringWithCommentWhitespace(new File(curpath), extension);

				ArrayList<String> results = CodeResultGenerator.getActualCodeFromCommonSegments(tokens, cursegments);

				for(String r: results) {
					// update the segment counter
					segmentCounter++;
					
					// write comment header
					if (extension.endsWith("java")) {
						fw.write("//////////////////////////////" + System.lineSeparator());
						fw.write("// Segment #" + segmentCounter + System.lineSeparator());
						fw.write("//////////////////////////////" + System.lineSeparator());
					}
					else if (extension.endsWith("py")) {
						fw.write("##############################" + System.lineSeparator());
						fw.write("# Segment #" + segmentCounter + System.lineSeparator());
						fw.write("##############################" + System.lineSeparator());
					}
					fw.write(r + System.lineSeparator() + System.lineSeparator());
				}
			}
			
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.out.println(sourcePathAndTheSegments);
	}

	public static void executeRemove(String[] args) {
		if (args.length != 6) {
			System.err.println("[Common code removal]");
			System.err.println("The number of arguments should be equal to six.");
			System.err.println("Run this software without arguments to show help.");
			return;
		}

		String input_dirpath = preparePathOrRegex(args[1]);
		if (isPathValidAndExist(input_dirpath) == false) {
			System.err.println("[Common code removal]");
			System.err.println("<input_dirpath> is not a valid path or refers to a ");
			System.err.println("  nonexistent directory.");
			System.err.println("Run this software without arguments to show help.");
			return;
		}

		boolean temp = isProgrammingLanguageValid(args[2]);
		if (temp == false) {
			System.err.println("[Common code removal]");
			System.err.println("<programming_language> should be either 'java' (for Java) or 'py'");
			System.err.println("  (for Python).");
			System.err.println("Run this software without arguments to show help.");
			return;
		}
		String programming_language = args[2];

		String common_code_path = preparePathOrRegex(args[3]);
		if (common_code_path != null) {
			if (common_code_path.equals("null"))
				common_code_path = null;
			else if (isPathValidAndExist(common_code_path) == false) {
				System.err.println("[Common code removal]");
				System.err.println("<common_code_filepath> is not a valid path or refers to a");
				System.err.println("  nonexistent file.");
				System.err.println("Run this software without arguments to show help.");
				return;
			}
		}

		String additional_keywords_path = preparePathOrRegex(args[4]);
		if (additional_keywords_path != null) {
			if (additional_keywords_path.equals("null"))
				additional_keywords_path = null;
			else if (isPathValidAndExist(additional_keywords_path) == false) {
				System.err.println("[Common code removal]");
				System.err.println("<additional_keywords_path> is not a valid path or refers to");
				System.err.println("  a nonexistent file.");
				System.err.println("Run this software without arguments to show help.");
				return;
			}
		}

		// for common code type
		String common_code_type = "";
		if (args[5].equalsIgnoreCase("complete"))
			common_code_type = "complete";
		else if (args[5].equalsIgnoreCase("code"))
			common_code_type = "code";
		else if (args[5].equalsIgnoreCase("codegeneralised"))
			common_code_type = "codegeneralised";
		else {
			System.err.println("[Common code removal]");
			System.err.println("<common_code_type> is not a valid argument. It should be either");
			System.err.println("  'code', 'codegeneralised', or 'complete'");
			System.err.println("Run this software without arguments to show help.");
			return;
		}

		CommonSegmentRemoval.removeCommonCode(input_dirpath, programming_language, common_code_path,
				additional_keywords_path, common_code_type);

		System.out.println("The command has been executed succesfully!");
		String outputDirpath = input_dirpath.substring(0, input_dirpath.lastIndexOf(File.separator) + 1) + "[result] "
				+ input_dirpath.substring(input_dirpath.lastIndexOf(File.separator) + 1, input_dirpath.length());

		System.out.println("The result can be seen in '" + outputDirpath + "'.");

		return;
	}

	public static void showHelp() {
		println("C2S2 (Common Code Segment Selector) is a tool to select common code segments across Java or");
		println("  Python student submissions. Common segments that have been manually validated can be passed");
		println("  to a code similarity detection tool for exclusion as the segments are not evident for");
		println("  raising suspicion of plagiarism or collusion. C2S2 can also remove the segments from");
		println("  student submissions, which might be useful for code similarity detection tools without such");
		println("  exclusion feature, assuming the tools can deal with uncompilable code.");

		println("C2S2 provides two modes:");
		println("1. Common segment selection: this mode lists any common segments from given student");
		println("  submissions and stores the result in an output file.");
		println("  -> Quick command: select <input_dirpath> <programming_language> <output_filepath>");
		println("  -> Complete command: select <input_dirpath> <programming_language> <output_filepath>");
		println("       <additional_keywords_path> <inclusion_threshold> <min_ngram_length>");
		println("       <max_ngram_length> coderesult generalised startident lineexclusive subremove");
		println("       Any of the last five arguments can be removed to adjust the selection's behaviour.");
		println("       Further details about those can be seen below.");

		println("2. Common code removal: this mode removes common code segments from given student");
		println("  submissions. This accepts a directory containing the code files and generates the");
		println("  results under a new directory named '[result]' + given input directory.");
		println("  -> Command: remove <input_dirpath> <programming_language> <common_code_filepath>");
		println("       <additional_keywords_path> <common_code_type>");

		println("\n\nParameters description (sorted alphabetically):");
		println("  <additional_keywords_path>: a string representing a file containing additional ");
		println("    keywords with newline as the delimiter. Keywords with more than one token should be");
		println("    written by embedding spaces between the tokens. For example, 'System.out.print'");
		println("    should be written as 'System . out . print'. If unused, please set this to 'null'.");
		println("  <common_code_filepath>: a string representing a file containing common code segments.");
		println("    The file can be either the mode 1's output or an arbitrary code written in compliance");
		println("    to the programming language's syntax.");
		println("  <common_code_type>: a string that should be either 'code', 'codegeneralised', or");
		println("    'complete'. The first one means the common code file is a regular code file. The second");
		println("    one is similar to the first except that the code tokens will be generalised prior");
		println("    compared for exclusion. The third one means the common code file is the mode 1's");
		println("    output without 'coderesult' parameter.");
		println("  <input_dirpath>: a string representing the input directory containing student submissions");
		println("    (each submission is represented by either one file or one sub-directory). Please use");
		println("    quotes if the path contains spaces.");
		println("  <inclusion_threshold>: a floating number representing the minimum percentage threshold for");
		println("    common segment inclusion. Any segments which submission occurrence proportion is higher");
		println("    than or equal to the threshold are included. This is assigned with 0.75 by default; all");
		println("    segments that occur in more than or equal to three fourths of the submissions are");
		println("    included.");
		println("    Value: a floating number between 0 to 1 (inclusive).");
		println("  <max_ngram_length>: a number depicting the largest n-gram length of the filtered common");
		println("    segments. This is assigned 50 by default.");
		println("    Value: a positive integer higher than <min_ngram_length>.");
		println("  <min_ngram_length>: a number depicting the smallest n-gram length of the filtered common");
		println("    segments. This is assigned 10 by default.");
		println("    Value: a positive integer.");
		println("  <output_filepath>: a string representing the filepath of the output, containing the");
		println("    common segments. Please use quotes if the path contains spaces.");
		println("  <programming_language>: a constant depicting the programming language used on given");
		println("    student submissions.");
		println("    Value: 'java' (for Java) or 'py' (for Python).");
		println("  'coderesult': this ensures the suggested segments are displayed as raw code instead of");
		println("    generalised while having no information about the variation. The segments can be passed");
		println("    directly to a code similarity detection tool for exclusion. It is set true by default.");
		println("  'generalised': this enables token generalisation while selection common segments. It is");
		println("    set true by default. See the paper for details.");
		println("  'lineexclusive': this ensures the common segment selection only considers segments that");
		println("    start at the beginning of a line and end at the end of a line. It is set as true by");
		println("    default. See the paper for details.");
		println("  'startident': this ensures the common segment selection only considers segments that");
		println("    start with identifier or keyword. It is set true by default. See the paper for details.");
		println("  'subremove': this removes any common segments that are a part of longer fragments from");
		println("    the result. It is set true by default. See the paper for details.");

	}

	private static void println(String s) {
		System.out.println(s);
	}

	private static Integer prepareMinNGram(String s) {
		// check whether s is actually a positive decimal.
		try {
			Integer x = Integer.parseInt(s);
			if (x > 0)
				return x;
			else
				return null;
		} catch (Exception e) {
			return null;
		}
	}

	private static Integer prepareMaxNGram(String s, int minNgram) {
		// check whether s is larger than min n gram value
		try {
			Integer x = Integer.parseInt(s);
			if (x > minNgram)
				return x;
			else
				return null;
		} catch (Exception e) {
			return null;
		}
	}

	private static Double prepareSimThreshold(String s) {
		// check whether s is actually a floating number ranged from 0 to 1
		// inclusive.
		try {
			Double x = Double.parseDouble(s);
			if (x >= 0 && x <= 1)
				return x;
			else
				return null;
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean isProgrammingLanguageValid(String prog) {
		if (prog != null && (prog.equals("java") || prog.equals("py")))
			return true;
		else
			return false;
	}

	private static String preparePathOrRegex(String path) {
		if (path != null && (path.startsWith("'") || path.startsWith("\"")))
			return path.substring(1, path.length() - 1);
		else
			return path;
	}

	private static boolean isPathValidAndExist(String path) {
		// check the validity of the string
		if (isPathValid(path) == false)
			return false;

		// check whether such file exists
		File f = new File(path);
		if (f.exists() == false)
			return false;

		return true;
	}

	private static boolean isPathValid(String path) {
		// check the validity of the string
		if (path == null || path.length() == 0)
			return false;
		else
			return true;
	}
}
