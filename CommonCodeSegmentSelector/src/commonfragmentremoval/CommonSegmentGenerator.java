package commonfragmentremoval;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import language.LibJavaExtractor;
import language.LibPythonExtractor;
import support.LibTuple;

public class CommonSegmentGenerator {

	public static ArrayList<CommonSegmentTuple> execute(String inputpath,
			String additionalkeywordspath, String extension,
			double inclusionThreshold, int minNGramLength, int maxNGramLength) {
		return execute(inputpath, additionalkeywordspath, extension,
				true, inclusionThreshold, true, true, minNGramLength,
				maxNGramLength, true);
	}

	public static ArrayList<CommonSegmentTuple> execute(String inputpath,
			String additionalkeywordspath, String extension,
			boolean isTokenGeneralised,
			double inclusionThreshold, boolean isNGramStartWithIdentifier,
			boolean isNGramLineExclusive, int minNGramLength,
			int maxNGramLength, boolean isSubFragmentRemoved) {
		System.gc();
		
		File input = new File(inputpath);
		// store all token strings for the data set. Key refers to file path
		// while value refers to the token string.
		HashMap<String, ArrayList<LibTuple>> tokenStringIndex = new HashMap<>();

		// 1.
		// System.out.println("1");
		// generate the token string
		collectingTokenStrings(input, additionalkeywordspath, extension,
				tokenStringIndex);

		// 2.
		// System.out.println("2");
		// generalise the token string
		if (isTokenGeneralised) {
			if (extension.endsWith("java"))
				tokenStringIndex = generaliseTokenStringJava(tokenStringIndex);
			else if (extension.endsWith("py"))
				tokenStringIndex = generaliseTokenStringPy(tokenStringIndex);
		}

		// 3, 4, 5, 6
		// System.out.println("3,4,5,6");

		// calculate the tokens' document frequency
		HashMap<String, Integer> df = generateDocFreq(tokenStringIndex);

		/*
		 * remove the separator token for df so that any segments with that
		 * token will be excluded.
		 */
		df.remove("***CSFSeparator***");

		/*
		 * merge adjacent n-grams for each token string, filter only the ones
		 * start with identifier, filter only the ones which first token is
		 * located at the beginning of a line and the last token is located at
		 * the end of a line
		 */
		HashMap<String, ArrayList<MergedTupleForRemoval>> pTokenStringIndex = mergeAdjacentNGrams(
				tokenStringIndex, df, isNGramStartWithIdentifier,
				isNGramLineExclusive, minNGramLength, maxNGramLength);

		// 7.
		// System.out.println("7");
		// calculate the n-grams' document frequency
		ArrayList<CommonSegmentTuple> result = generateDocumentFrequencyPerNGram(pTokenStringIndex);

		// 8.
		// System.out.println("8");
		// take only n-grams with DF score higher or equal to an inclusion
		// threshold
		result = removeCommonFragmentsLowerThanThreshold(result,
				inclusionThreshold, tokenStringIndex.size());

		// 9.
		// System.out.println("9");
		if (isSubFragmentRemoved) {
			// remove common sub-fragments
			result = removeCommonSubFragments(result);
		}

		// sort the result
		Collections.sort(result);

		return result;
	}

	public static void collectingTokenStrings(
			File input,
			String additionalkeywordspath,
			String extension,
			HashMap<String, ArrayList<LibTuple>> tokenStringIndex) {
		// for each sub-directory, treat them as one student submission
		File[] submissionDirs = input.listFiles();
		for (File submissionDir : submissionDirs) {
			// get all tokens
			ArrayList<LibTuple> tokenString = getTokenString(
					submissionDir, additionalkeywordspath, extension);

			// put it in the index if the token string is not empty
			if (tokenString.size() > 0) {
				tokenStringIndex.put(submissionDir.getAbsolutePath(),
						tokenString);

				//System.out.println(submissionDir.getAbsolutePath());
				//System.out.println(tokenString);
			}
		}
	}

	private static ArrayList<LibTuple> getTokenString(
			File c, String additionalkeywordspath, String extension) {
		// get token string for a submission

		// var to store the result
		ArrayList<LibTuple> tokenString = new ArrayList<>();

		if (c.isDirectory()) {
			// recursive if it is a directory
			File[] children = c.listFiles();
			for (File cc : children) {
				tokenString.addAll(getTokenString(cc, additionalkeywordspath,
						extension));
			}
		} else {
			// if it is a file and the name has the extension, do the
			// process
			if (c.getName().endsWith(extension)) {
				// generate the tokens
				ArrayList<LibTuple> tokens = null;
				if (extension.endsWith("java"))
					tokens = LibJavaExtractor.getDefaultTokenString(
							c.getAbsolutePath(), additionalkeywordspath);
				else if (extension.endsWith("py"))
					tokens = LibPythonExtractor.getDefaultTokenString(
							c.getAbsolutePath(), additionalkeywordspath);

				// remove comment and whitespaces
				commentAndWhitespaceTokenRemoval(tokens);
				
				
				// add separator token so that the segments will still be file
				// exclusive
				tokens.add(new LibTuple(
						"***CSFSeparator***", "***CSFSeparator***", -1));

				// add to the merged token string
				tokenString.addAll(tokens);
			}
		}

		return tokenString;
	}

	public static void commentAndWhitespaceTokenRemoval(
			ArrayList<LibTuple> tokens) {
		// remove comment and whitespace token from given token string.
		for (int i = 0; i < tokens.size(); i++) {
			LibTuple token = tokens.get(i);
			if (token.getType().equals("WS")
					|| token.getType().endsWith("COMMENT")) {
				tokens.remove(i);
				i--;
			}
		}
	}

	public static HashMap<String, ArrayList<LibTuple>> generaliseTokenStringJava(
			HashMap<String, ArrayList<LibTuple>> tokenStringIndex) {

		// for each token string
		Iterator<Entry<String, ArrayList<LibTuple>>> it = tokenStringIndex
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<LibTuple>> curEntry = it
					.next();
			ArrayList<LibTuple> inList = curEntry
					.getValue();

			// for each merged n-gram
			_generaliseTokenStringJava(inList);
		}

		return tokenStringIndex;
	}

	private static void _generaliseTokenStringJava(
			ArrayList<LibTuple> tokenString) {

		for (int i = 0; i < tokenString.size(); i++) {
			LibTuple c = tokenString.get(i);
			// this sect was copied and modified from
			// JavaFeedbackGenerator
			String type = c.getType();
			if (type.equals("Identifier")) {
				if (c.getText().equals("Integer")
						|| c.getText().equals("Short")
						|| c.getText().equals("Long")
						|| c.getText().equals("Byte")
						|| c.getText().equals("Float")
						|| c.getText().equals("Double")) {
					c.setText("$numt$");
				} else if (c.getText().equals("String")
						|| c.getText().equals("Character")) {
					c.setText("$strt$");
				} else
					c.setText("$idn$");
			} else if (type.equals("StringLiteral")
					|| type.equals("CharacterLiteral")) {
				c.setText("$strl$");
			} else if (type.equals("IntegerLiteral")
					|| type.equals("FloatingPointLiteral"))
				c.setText("$numl$");
			else if (type.equals("'char'"))
				c.setText("$strt$");
			else if (type.equals("'int'") || type.equals("'short'")
					|| type.equals("'long'") || type.equals("'byte'")
					|| type.equals("'float'") || type.equals("'double'"))
				c.setText("$numt$");
		}
	}

	public static String getGeneralisedTokenJava(
			LibTuple c) {
		// this sect was copied and modified from
		// JavaFeedbackGenerator
		String type = c.getType();
		if (type.equals("additional_keyword")) {
			return c.getRawText();
		} else if (type.equals("Identifier")) {
			if (c.getText().equals("Integer") || c.getText().equals("Short")
					|| c.getText().equals("Long") || c.getText().equals("Byte")
					|| c.getText().equals("Float")
					|| c.getText().equals("Double")) {
				return "$numt$";
			} else if (c.getText().equals("String")
					|| c.getText().equals("Character")) {
				return "$strt$";
			} else
				return "$idn$";
		} else if (type.equals("StringLiteral")
				|| type.equals("CharacterLiteral")) {
			return "$strl$";
		} else if (type.equals("IntegerLiteral")
				|| type.equals("FloatingPointLiteral"))
			return "$numl$";
		else if (type.equals("'char'"))
			return "$strt$";
		else if (type.equals("'int'") || type.equals("'short'")
				|| type.equals("'long'") || type.equals("'byte'")
				|| type.equals("'float'") || type.equals("'double'"))
			return "$numt$";
		else
			return c.getText();
	}

	public static HashMap<String, ArrayList<LibTuple>> generaliseTokenStringPy(
			HashMap<String, ArrayList<LibTuple>> tokenStringIndex) {

		// for each token string
		Iterator<Entry<String, ArrayList<LibTuple>>> it = tokenStringIndex
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<LibTuple>> curEntry = it
					.next();
			ArrayList<LibTuple> inList = curEntry
					.getValue();

			// for each merged n-gram
			_generaliseTokenStringPy(inList);
		}

		return tokenStringIndex;
	}

	private static void _generaliseTokenStringPy(
			ArrayList<LibTuple> tokenString) {

		for (int i = 0; i < tokenString.size(); i++) {
			LibTuple c = tokenString.get(i);
			// this sect was copied and modified from
			// JavaFeedbackGenerator
			String type = c.getType();
			if (type.equals("Identifier")) {
				c.setText("$idn$");
			} else if (type.equals("STRING_LITERAL"))
				c.setText("$strl$");
			else if (type.equals("DECIMAL_INTEGER")
					|| type.equals("FloatingPointLiteral"))
				c.setText("$numl$");
		}
	}

	public static String getGeneralisedTokenPy(
			LibTuple c) {
		// this sect was copied and modified from
		// JavaFeedbackGenerator
		String type = c.getType();
		if (type.equals("additional_keyword"))
			return c.getRawText();
		else if (type.equals("Identifier")) {
			return "$idn$";
		} else if (type.equals("STRING_LITERAL"))
			return "$strl$";
		else if (type.equals("DECIMAL_INTEGER")
				|| type.equals("FloatingPointLiteral"))
			return "$numl$";
		else
			return c.getText();
	}

	public static HashMap<String, Integer> generateDocFreq(
			HashMap<String, ArrayList<LibTuple>> tokenStringIndex) {
		HashMap<String, Integer> df = new HashMap<>();

		// for each token string
		Iterator<ArrayList<LibTuple>> it = tokenStringIndex
				.values().iterator();
		while (it.hasNext()) {
			ArrayList<LibTuple> cur = it.next();

			HashSet<String> distinctText = new HashSet<>();

			for (int i = 0; i < cur.size(); i++) {
				LibTuple c = cur.get(i);
				// check if it is previously occurred
				if (distinctText.contains(c.getText()) == false) {
					// get the frequency
					Integer f = df.get(c.getText());
					// if null, set as 0
					if (f == null)
						f = 0;
					// update by increment 1
					df.put(c.getText(), f + 1);
					// add the text to the distinct set
					distinctText.add(c.getText());
				}
			}
		}
		return df;
	}

	public static HashMap<String, Integer> filterDFWithHigherOccurrence(
			HashMap<String, Integer> df, double mergingThreshold, int totalFiles) {
		/*
		 * this method returns a hashmap containing df that occurrence is higher
		 * or equal to the merging threshold. The occurrence is defined by
		 * dividing the document frequency with total number of files.
		 */

		HashMap<String, Integer> ndf = new HashMap<>();

		// for each tuple in df
		Iterator<Entry<String, Integer>> it = df.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer> cur = it.next();
			if (cur.getValue() * 1.0 / totalFiles >= mergingThreshold) {
				ndf.put(cur.getKey(), cur.getValue());
			}
		}
		return ndf;
	}

	public static HashMap<String, ArrayList<MergedTupleForRemoval>> mergeAdjacentNGrams(
			HashMap<String, ArrayList<LibTuple>> tokenStringIndex,
			HashMap<String, Integer> fdf, boolean isNGramStartWithIdentifier,
			boolean isNGramLineExclusive, int minNGramLength, int maxNGramLength) {

		// to store the result
		HashMap<String, ArrayList<MergedTupleForRemoval>> result = new HashMap<>();

		// for each token string
		Iterator<Entry<String, ArrayList<LibTuple>>> it = tokenStringIndex
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<LibTuple>> curEntry = it
					.next();
			
			// the original form
			ArrayList<LibTuple> inList = curEntry
					.getValue();
			// the merged form
			ArrayList<MergedTupleForRemoval> outList = new ArrayList<>();
			for (int i = 0; i < inList.size(); i++) {

				boolean isFirstTokenIdentifier = true;
				boolean isStartLineExclusive = true;

				if (isNGramStartWithIdentifier) {
					// check whether it starts with identifier
					String firstTokenText = inList.get(i).getRawText();
					isFirstTokenIdentifier = false;
					if (Character.isJavaIdentifierStart(firstTokenText
							.charAt(0))) {
						// mark as a possible identifier
						isFirstTokenIdentifier = true;
						// check whether the remaining chars are
						// identifier-permissible
						for (int j = 1; j < firstTokenText.length(); j++) {
							char c = firstTokenText.charAt(j);
							if (Character.isJavaIdentifierPart(c) == false)
								isFirstTokenIdentifier = false;
						}
					}
				}

				if (isNGramLineExclusive) {
					// check whether the first token is at the beginning of a
					// line
					isStartLineExclusive = false;
					if (i == 0)
						isStartLineExclusive = true;
					else if (inList.get(i).getLine() != inList.get(i - 1)
							.getLine())
						isStartLineExclusive = true;
				}

				if (isFirstTokenIdentifier && isStartLineExclusive) {
					// get the longest possible common tokens
					for (int j = i; j < inList.size(); j++) {
						if (fdf.containsKey(inList.get(j).getText())) {
							boolean isFinishLineExclusive = true;

							if (isNGramLineExclusive) {
								// check whether the last token is at the end of
								// the line
								isFinishLineExclusive = false;
								if (j == inList.size() - 1)
									isFinishLineExclusive = true;
								else if (j + 1 < inList.size()
										&& inList.get(j).getLine() != inList
												.get(j + 1).getLine())
									isFinishLineExclusive = true;
							}

							// if isFinishLineExclusive and the length is longer
							// than a particular threshold
							if (isFinishLineExclusive
									&& (j - i + 1) >= minNGramLength
									&& (j - i + 1) <= maxNGramLength) {
								// create the new members
								ArrayList<LibTuple> newMembers = new ArrayList<>();
								for (int k = i; k <= j; k++) {
									newMembers.add(inList.get(k));
								}

								// make the merged token
								MergedTupleForRemoval out = new MergedTupleForRemoval(
										i, inList.get(i));

								// add the new members
								out.setMembers(newMembers);

								// add to the outlist
								outList.add(out);
							}

						} else {
							break;
						}
					}
				}

			}
			// add to the result
			result.put(curEntry.getKey(), outList);
		}

		return result;
	}

	public static HashMap<String, ArrayList<MergedTupleForRemoval>> filterNGramsStartingWithIdentifier(
			HashMap<String, ArrayList<MergedTupleForRemoval>> tokenStringIndex) {

		// for each token string
		Iterator<Entry<String, ArrayList<MergedTupleForRemoval>>> it = tokenStringIndex
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<MergedTupleForRemoval>> curEntry = it
					.next();
			ArrayList<MergedTupleForRemoval> inList = curEntry.getValue();

			for (int i = 0; i < inList.size(); i++) {

				MergedTupleForRemoval cur = inList.get(i);
				String firstTokenText = cur.getMembers().get(0).getRawText();
				boolean isIdentifier = false;
				if (Character.isJavaIdentifierStart(firstTokenText.charAt(0))) {
					// mark as a possible identifier
					isIdentifier = true;
					// check whether the remaining chars are
					// identifier-permissible
					for (int j = 1; j < firstTokenText.length(); j++) {
						char c = firstTokenText.charAt(j);
						if (Character.isJavaIdentifierPart(c) == false)
							isIdentifier = false;
					}
				}

				// if it is not an identifier, remove
				if (isIdentifier == false) {
					inList.remove(i);
					i--;
				}

			}
		}

		return tokenStringIndex;
	}

	public static ArrayList<CommonSegmentTuple> generateDocumentFrequencyPerNGram(
			HashMap<String, ArrayList<MergedTupleForRemoval>> tokenStringIndex) {

		HashMap<String, CommonSegmentTuple> df = new HashMap<>();

		// for each token string
		Iterator<Entry<String,ArrayList<MergedTupleForRemoval>>> it = tokenStringIndex
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<MergedTupleForRemoval>> encur = it.next();
			
			String sourcePath = encur.getKey();
			ArrayList<MergedTupleForRemoval> cur = encur.getValue();

			HashSet<String> distinctText = new HashSet<>();

			for (int i = 0; i < cur.size(); i++) {
				MergedTupleForRemoval c = cur.get(i);
				// check if it is previously occured
				if (distinctText.contains(c.getText()) == false) {
					// get the frequency
					CommonSegmentTuple f = df.get(c.getText());
					if (f == null) {
						// if null, set as the new one
						f = new CommonSegmentTuple(c.getText(), c.getMembers(), sourcePath);
						df.put(c.getText(), f);
					} else {
						// otherwise, only update
						f.addNewFragmentInstance(c.getMembers());
					}
					// add to the distinct text
					distinctText.add(c.getText());
				}
			}

		}

		// get the result
		ArrayList<CommonSegmentTuple> result = new ArrayList<>();
		// per entry, add it to the list
		Iterator<CommonSegmentTuple> it2 = df.values().iterator();
		while (it2.hasNext()) {
			CommonSegmentTuple cur = it2.next();
			// System.out.println(cur.getKey() + " " + cur.getValue().size());
			result.add(cur);
		}

		return result;
	}

	public static ArrayList<CommonSegmentTuple> removeCommonFragmentsLowerThanThreshold(
			ArrayList<CommonSegmentTuple> r, double threshold, int totalFiles) {
		for (int i = 0; i < r.size(); i++) {
			int freq = r.get(i).getOccFrequency();
			double dp = freq * 1.0 / totalFiles;

			// if the condition does not met anymore, remove
			if (dp < threshold) {
				r.remove(i);
				i--;
			}
		}

		return r;
	}

	public static ArrayList<CommonSegmentTuple> removeCommonSubFragments(
			ArrayList<CommonSegmentTuple> r) {

		for (int i = 0; i < r.size(); i++) {
			CommonSegmentTuple cur = r.get(i);
			// check whether the text occurs on others
			for (int j = 0; j < r.size(); j++) {
				if (i != j) {
					CommonSegmentTuple next = r.get(j);
					if (next.getContent().contains(cur.getContent())) {
						// if found, remove the candidate
						r.remove(i);
						i--;
						break;
					}
				}
			}
		}

		return r;
	}
}
