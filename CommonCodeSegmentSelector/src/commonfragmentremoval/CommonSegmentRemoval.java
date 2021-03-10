package commonfragmentremoval;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

import language.LibJavaExtractor;
import language.LibPythonExtractor;
import support.LibTuple;
import support.stringmatching.GSTMatchTuple;
import support.stringmatching.GreedyStringTiling;

public class CommonSegmentRemoval {

	public static void removeCommonCode(String rootDirFilepath, String ext,
			String commonCodePath, String additionalKeywordPath,
			String commonCodeType) {

		String outputRootDirFilepath = rootDirFilepath.substring(0,
				rootDirFilepath.lastIndexOf(File.separator) + 1)
				+ "[result] "
				+ rootDirFilepath.substring(
						rootDirFilepath.lastIndexOf(File.separator) + 1,
						rootDirFilepath.length());

		// create output directory
		new File(outputRootDirFilepath).mkdir();

		if (commonCodeType.equals("complete")) {
			// if complete mode, just iterative search
			removeNonTemplateCode(rootDirFilepath, ext, commonCodePath,
					additionalKeywordPath, outputRootDirFilepath);
		} else {
			boolean isGeneralised = false;
			if (commonCodeType.equals("codegeneralised"))
				isGeneralised = true;
			removeTemplateCode(rootDirFilepath, ext, commonCodePath,
					additionalKeywordPath, outputRootDirFilepath, isGeneralised);
		}
	}

	private static void removeNonTemplateCode(String rootDirFilepath,
			String ext, String commonCodePath, String additionalKeywordPath,
			String outputRootDirFilepath) {
		// read the tokens
		try {
			ArrayList<String[]> commonFragments = new ArrayList<>();
			Scanner sc = new Scanner(new File(commonCodePath));
			while (sc.hasNextLine()) {
				sc.nextLine(); // skip the title of the common fragment

				String generalisedContent = "";
				while (sc.hasNextLine()) {
					generalisedContent = sc.nextLine();
					if (generalisedContent
							.startsWith("\tFully-generalised form:")) {
						break;
					}
				}
				// get the generalised content
				generalisedContent = generalisedContent
						.substring(generalisedContent.indexOf(":") + 2);

				// System.out.println(generalisedContent);
				commonFragments.add(generalisedContent.split(" "));
			}

			// for each project dir, take the code files and merge them as a
			// file
			File rootDir = new File(rootDirFilepath);
			File[] studentDir = rootDir.listFiles();
			for (File sdir : studentDir) {
				if (sdir.isDirectory()) {
					File outputDir = new File(outputRootDirFilepath);
					outputDir.mkdir();
					_removeCommonCode(sdir, outputDir, ext, commonFragments,
							additionalKeywordPath);
				}
			}

			sc.close();
		} catch (Exception e) {
			System.err
					.println("The content of common code file is not correctly formatted.");
			e.printStackTrace();
			return;
		}
	}

	private static void _removeCommonCode(File sfile, File tFile, String ext,
			ArrayList<String[]> commonFragments, String additionalKeywordPath) {
		if (sfile.isDirectory()) {
			// copy and create similar dir
			File newTFile = new File(tFile.getAbsolutePath() + File.separator
					+ sfile.getName());
			newTFile.mkdir();

			File[] schildren = sfile.listFiles();
			for (File sc : schildren) {
				_removeCommonCode(sc, newTFile, ext, commonFragments,
						additionalKeywordPath);
			}
		} else {
			String name = sfile.getName();
			// if the file does not end with the extension, ignore
			if (name.endsWith(ext) == false)
				return;

			String targetFilePath = tFile.getAbsolutePath() + File.separator
					+ sfile.getName();

			removeCommonCodeFromAFile(sfile.getAbsolutePath(), commonFragments,
					targetFilePath, ext, additionalKeywordPath);
		}
	}

	private static void removeCommonCodeFromAFile(String sourceFilePath,
			ArrayList<String[]> commonFragments, String targetFilePath,
			String ext, String additionalKeywordsPath) {
		// enable overlap common fragments

		// generate the token string
		ArrayList<LibTuple> sourceTokenString = null;
		String openingComment = "";
		String closingComment = "";
		String openingCommentForRemovedContent = "";

		if (ext.endsWith("java")) {
			// java
			sourceTokenString = LibJavaExtractor.getDefaultTokenString(
					sourceFilePath, additionalKeywordsPath);

			openingComment = "/*";
			closingComment = "*/";
			openingCommentForRemovedContent = "//";
		} else if (ext.endsWith("py")) {
			// python
			sourceTokenString = LibPythonExtractor.getDefaultTokenString(
					sourceFilePath, additionalKeywordsPath);

			openingComment = "#";
			closingComment = "#";
			openingCommentForRemovedContent = "#";
		}

		// convert to array as RKRGST needs such kind of input
		String[] str1 = convertTokenTupleToString(sourceTokenString, true, ext);

		
		// mark per token whether that token should be removed
		boolean[] isRemovedSyntax = new boolean[str1.length];
		// for each common fragment, search from str1 and exclude
		for (String[] c : commonFragments) {
			
			for (int i = 0; i < str1.length - c.length+1; i++) {
				boolean isFound = true;
				// check the next elements, whether c is contained on there
				for (int j = 0; j < c.length; j++) {
					if (c[j].equals(str1[i + j]) == false)
						isFound = false;
				}
				// if c is empty string, mark as not found
				if (c.length == 0)
					isFound = false;

				if (isFound) {
					// set as removed
					for (int j = 0; j < c.length; j++) {
						isRemovedSyntax[i + j] = true;
					}
					// jump index, decrement by 1 as the loop will increase that
					// by one
					i = i + (c.length - 1);
				}
			}
		}

		// store all code lines affected by the template code removal
		ArrayList<Integer> targetLinesForWritingTheRemovedFragments = new ArrayList<>();

		// generate the code with the template code removed
		StringBuilder sbBlank = new StringBuilder();
		// store the original code for reporting which ones will have been
		// removed
		StringBuilder sbOrig = new StringBuilder();

		// syntax counter refers to the position in syntax string used for
		// RKRGST comparison
		int syntaxIndexCounter = 0;
		// general counter refers to the position in general string taken from
		// the code (including whitespaces and comments)
		int generalIndexCounter = 0;

		// iterate the general token string
		while (generalIndexCounter < sourceTokenString.size()) {
			// increment syntax index only if the visited token is syntax
			if (!sourceTokenString.get(generalIndexCounter).getType()
					.equals("WS")
					&& !sourceTokenString.get(generalIndexCounter).getType()
							.endsWith("COMMENT")) {

				// if the token should be removed
				if (isRemovedSyntax[syntaxIndexCounter] == true) {
					// put spaces as a replacement for token text
					LibTuple t = sourceTokenString
							.get(generalIndexCounter);
					int spacesize = t.getText().length();
					String spaces = "";
					for (int i = 0; i < spacesize; i++)
						spaces += " ";
					sbBlank.append(spaces);

					// for orig code, just put the original text
					sbOrig.append(sourceTokenString.get(generalIndexCounter)
							.getText());

					// save the target lines
					if (targetLinesForWritingTheRemovedFragments.size() == 0) {
						targetLinesForWritingTheRemovedFragments.add(t
								.getLine());
					} else {
						// check whether the line is the same as previous
						int lastIndex = targetLinesForWritingTheRemovedFragments
								.size() - 1;
						int lastLine = targetLinesForWritingTheRemovedFragments
								.get(lastIndex);
						if (lastLine != t.getLine()) {
							// add as a new entry if different
							targetLinesForWritingTheRemovedFragments.add(t
									.getLine());
						}
					}
				} else {
					// set both code strings for blank and orig with the text
					sbBlank.append(sourceTokenString.get(generalIndexCounter)
							.getText());
					sbOrig.append(sourceTokenString.get(generalIndexCounter)
							.getText());
				}

				// increase syntax counter
				syntaxIndexCounter++;

			} else {
				// set both code strings for blank and orig with the text
				sbBlank.append(sourceTokenString.get(generalIndexCounter)
						.getText());
				sbOrig.append(sourceTokenString.get(generalIndexCounter)
						.getText());
			}

			// increase general index
			generalIndexCounter++;
		}

		// split both code strings based on newlines
		String[] codeInLines = sbBlank.toString().split(System.lineSeparator());
		String[] codeInLinesOrig = sbOrig.toString().split(
				System.lineSeparator());

		// write the string to target file
		try {
			int removedFragmentCounter = 0;
			FileWriter fw = new FileWriter(new File(targetFilePath));
			for (int i = 0; i < codeInLines.length; i++) {
				// if there are still target lines available for processing
				if (removedFragmentCounter < targetLinesForWritingTheRemovedFragments
						.size()) {
					// one line before the removed fragment
					if (i == targetLinesForWritingTheRemovedFragments
							.get(removedFragmentCounter) - 1) {

						// title of the comment
						String title = "line below prior common code removal:";
						// the content, taken from original code string
						String origContent = codeInLinesOrig[i];

						// get the max length between those two strings
						int maxLength = Math.max(title.length(),
								origContent.length());

						// generate headerfooter
						String headerfooter = "";
						for (int k = 0; k < maxLength; k++) {
							headerfooter += "=";
						}

						// header
						fw.write(openingComment + " " + headerfooter + " "
								+ closingComment + System.lineSeparator());

						// title
						fw.write(openingCommentForRemovedContent + " " + title
								+ System.lineSeparator());

						// the removed fragment
						fw.write(openingCommentForRemovedContent + " "
								+ origContent + System.lineSeparator());

						// footer
						fw.write(openingComment + " " + headerfooter + " "
								+ closingComment + System.lineSeparator());

						// move to the next fragment
						removedFragmentCounter++;
					}
				}
				fw.write(codeInLines[i] + System.lineSeparator());
			}
			// fw.write(sb.toString());
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void removeTemplateCode(String rootDirFilepath, String ext,
			String commonCodePath, String additionalKeywordPath,
			String outputRootDirFilepath, boolean isGeneralised) {

		ArrayList<LibTuple> templateTokenString = null;
		if (ext.endsWith("java")) {
			// java
			templateTokenString = LibJavaExtractor.getDefaultTokenString(
					commonCodePath, additionalKeywordPath);
		} else if (ext.endsWith("py")) {
			// python
			templateTokenString = LibPythonExtractor.getDefaultTokenString(
					commonCodePath, additionalKeywordPath);
		}
		String[] str2 = convertTokenTupleToString(templateTokenString,
				isGeneralised, ext);

		// for each project dir, take the code files and merge them as a file
		File rootDir = new File(rootDirFilepath);
		File[] studentDir = rootDir.listFiles();
		for (File sdir : studentDir) {
			if (sdir.isDirectory()) {
				File outputDir = new File(outputRootDirFilepath);
				outputDir.mkdir();
				_removeTemplateCode(sdir, outputDir, ext, str2,
						additionalKeywordPath, isGeneralised);
			}
		}
	}

	private static void _removeTemplateCode(File sfile, File tFile, String ext,
			String[] templateString, String additionalKeywordPath,
			boolean isGeneralised) {
		if (sfile.isDirectory()) {
			// copy and create similar dir
			File newTFile = new File(tFile.getAbsolutePath() + File.separator
					+ sfile.getName());
			newTFile.mkdir();

			File[] schildren = sfile.listFiles();
			for (File sc : schildren) {
				_removeTemplateCode(sc, newTFile, ext, templateString,
						additionalKeywordPath, isGeneralised);
			}
		} else {
			String name = sfile.getName();
			// if the file does not end with the extension, ignore
			if (name.endsWith(ext) == false)
				return;

			String targetFilePath = tFile.getAbsolutePath() + File.separator
					+ sfile.getName();

			removeTemplateCodeFromAFile(sfile.getAbsolutePath(),
					templateString, targetFilePath, ext, additionalKeywordPath,
					isGeneralised);
		}
	}

	private static void removeTemplateCodeFromAFile(String sourceFilePath,
			String[] templateString, String targetFilePath, String ext,
			String additionalKeywordsPath, boolean isGeneralised) {

		// generate the token string
		ArrayList<LibTuple> sourceTokenString = null;
		String openingComment = "";
		String closingComment = "";
		String openingCommentForRemovedContent = "";

		if (ext.endsWith("java")) {
			// java
			sourceTokenString = LibJavaExtractor.getDefaultTokenString(
					sourceFilePath, additionalKeywordsPath);

			openingComment = "/*";
			closingComment = "*/";
			openingCommentForRemovedContent = "//";
		} else if (ext.endsWith("py")) {
			// python
			sourceTokenString = LibPythonExtractor.getDefaultTokenString(
					sourceFilePath, additionalKeywordsPath);

			openingComment = "#";
			closingComment = "#";
			openingCommentForRemovedContent = "#";
		}

		// convert to array as RKRGST needs such kind of input
		String[] str1 = convertTokenTupleToString(sourceTokenString,
				isGeneralised, ext);

		// RKRGST
		ArrayList<GSTMatchTuple> tiles = GreedyStringTiling.getMatchedTiles(
				str1, templateString, 2);
		// mark per token whether that token should be removed
		boolean[] isRemovedSyntax = new boolean[str1.length];
		// iterate and mark all tokens that will be excluded
		for (GSTMatchTuple tile : tiles) {
			for (int i = 0; i < tile.length; i++) {
				isRemovedSyntax[tile.patternPosition + i] = true;
			}
		}

		// store all code lines affected by the template code removal
		ArrayList<Integer> targetLinesForWritingTheRemovedFragments = new ArrayList<>();

		// generate the code with the template code removed
		StringBuilder sbBlank = new StringBuilder();
		// store the original code for reporting which ones will have been
		// removed
		StringBuilder sbOrig = new StringBuilder();

		// syntax counter refers to the position in syntax string used for
		// RKRGST comparison
		int syntaxIndexCounter = 0;
		// general counter refers to the position in general string taken from
		// the code (including whitespaces and comments)
		int generalIndexCounter = 0;

		// iterate the general token string
		while (generalIndexCounter < sourceTokenString.size()) {
			// increment syntax index only if the visited token is syntax
			if (!sourceTokenString.get(generalIndexCounter).getType()
					.equals("WS")
					&& !sourceTokenString.get(generalIndexCounter).getType()
							.endsWith("COMMENT")) {

				// if the token should be removed
				if (isRemovedSyntax[syntaxIndexCounter] == true) {
					// put spaces as a replacement for token text
					LibTuple t = sourceTokenString
							.get(generalIndexCounter);
					int spacesize = t.getText().length();
					String spaces = "";
					for (int i = 0; i < spacesize; i++)
						spaces += " ";
					sbBlank.append(spaces);

					// for orig code, just put the original text
					sbOrig.append(sourceTokenString.get(generalIndexCounter)
							.getText());

					// save the target lines
					if (targetLinesForWritingTheRemovedFragments.size() == 0) {
						targetLinesForWritingTheRemovedFragments.add(t
								.getLine());
					} else {
						// check whether the line is the same as previous
						int lastIndex = targetLinesForWritingTheRemovedFragments
								.size() - 1;
						int lastLine = targetLinesForWritingTheRemovedFragments
								.get(lastIndex);
						if (lastLine != t.getLine()) {
							// add as a new entry if different
							targetLinesForWritingTheRemovedFragments.add(t
									.getLine());
						}
					}
				} else {
					// set both code strings for blank and orig with the text
					sbBlank.append(sourceTokenString.get(generalIndexCounter)
							.getText());
					sbOrig.append(sourceTokenString.get(generalIndexCounter)
							.getText());
				}

				// increase syntax counter
				syntaxIndexCounter++;

			} else {
				// set both code strings for blank and orig with the text
				sbBlank.append(sourceTokenString.get(generalIndexCounter)
						.getText());
				sbOrig.append(sourceTokenString.get(generalIndexCounter)
						.getText());
			}

			// increase general index
			generalIndexCounter++;
		}

		// split both code strings based on newlines
		String[] codeInLines = sbBlank.toString().split(System.lineSeparator());
		String[] codeInLinesOrig = sbOrig.toString().split(
				System.lineSeparator());

		// write the string to target file
		try {
			int removedFragmentCounter = 0;
			FileWriter fw = new FileWriter(new File(targetFilePath));
			for (int i = 0; i < codeInLines.length; i++) {
				// if there are still target lines available for processing
				if (removedFragmentCounter < targetLinesForWritingTheRemovedFragments
						.size()) {
					// one line before the removed fragment
					if (i == targetLinesForWritingTheRemovedFragments
							.get(removedFragmentCounter) - 1) {

						// title of the comment
						String title = "line below prior common code removal:";
						// the content, taken from original code string
						String origContent = codeInLinesOrig[i];

						// get the max length between those two strings
						int maxLength = Math.max(title.length(),
								origContent.length());

						// generate headerfooter
						String headerfooter = "";
						for (int k = 0; k < maxLength; k++) {
							headerfooter += "=";
						}

						// header
						fw.write(openingComment + " " + headerfooter + " "
								+ closingComment + System.lineSeparator());

						// title
						fw.write(openingCommentForRemovedContent + " " + title
								+ System.lineSeparator());

						// the removed fragment
						fw.write(openingCommentForRemovedContent + " "
								+ origContent + System.lineSeparator());

						// footer
						fw.write(openingComment + " " + headerfooter + " "
								+ closingComment + System.lineSeparator());

						// move to the next fragment
						removedFragmentCounter++;
					}
				}
				fw.write(codeInLines[i] + System.lineSeparator());
			}
			// fw.write(sb.toString());
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String[] convertTokenTupleToString(
			ArrayList<LibTuple> arr,
			boolean isGeneralised, String ext) {
		int size = 0;
		for (int i = 0; i < arr.size(); i++) {
			if (arr.get(i).getType().equals("WS")
					|| arr.get(i).getType().endsWith("COMMENT"))
				continue;
			else
				size++;
		}

		String[] s = new String[size];
		int count = 0;
		for (int i = 0; i < arr.size(); i++) {
			if (arr.get(i).getType().equals("WS")
					|| arr.get(i).getType().endsWith("COMMENT"))
				continue;
			else {
				if (isGeneralised) {
					if (ext.endsWith("java"))
						s[count] = CommonSegmentGenerator
								.getGeneralisedTokenJava(arr.get(i));
					else if (ext.endsWith("py"))
						s[count] = CommonSegmentGenerator
								.getGeneralisedTokenPy(arr.get(i));
				} else {
					s[count] = arr.get(i).getText();
				}
				count++;
			}
		}
		return s;
	}
}
