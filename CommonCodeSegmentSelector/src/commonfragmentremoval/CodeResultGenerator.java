package commonfragmentremoval;

import java.io.File;
import java.util.ArrayList;

import language.LibJavaExtractor;
import language.LibPythonExtractor;
import support.LibTuple;

public class CodeResultGenerator {
	public static ArrayList<LibTuple> getTokenStringWithCommentWhitespace(File c,
			String extension) {
		// get token string for a submission

		// var to store the result
		ArrayList<LibTuple> tokenString = new ArrayList<>();

		if (c.isDirectory()) {
			// recursive if it is a directory
			File[] children = c.listFiles();
			for (File cc : children) {
				tokenString.addAll(getTokenStringWithCommentWhitespace(cc, extension));
			}
		} else {
			// if it is a file and the name has the extension, do the
			// process
			if (c.getName().endsWith(extension)) {
				// generate the tokens WITHOUT GENERALISATION
				ArrayList<LibTuple> tokens = null;
				if (extension.endsWith("java"))
					tokens = LibJavaExtractor.getDefaultTokenString(c.getAbsolutePath(), "");
				else if (extension.endsWith("py"))
					tokens = LibPythonExtractor.getDefaultTokenString(c.getAbsolutePath(), "");

				// remove comment token from given token string.
				for (int i = 0; i < tokens.size(); i++) {
					LibTuple token = tokens.get(i);
					if (token.getType().endsWith("COMMENT")) {
						tokens.remove(i);
						i--;
					}
				}

				// add separator token so that the segments will still be file
				// exclusive
				tokens.add(new LibTuple("***CSFSeparator***", "***CSFSeparator***", -1));

				// add to the merged token string
				tokenString.addAll(tokens);
			}
		}

		return tokenString;
	}

	public static ArrayList<String> getActualCodeFromCommonSegments(
			ArrayList<LibTuple> tokens, ArrayList<CommonSegmentTuple> segments) {
		ArrayList<String> codeResult = new ArrayList<String>();

		for (CommonSegmentTuple s : segments) {
			// get the first actual segment
			ArrayList<String> sArr = s.getFirstActualSegment();

			boolean isFound = false;
			int startIdx = 0;
			int finishIdx = -1;
			while (startIdx < tokens.size() - sArr.size()) {
				// if whitespaces as the first token, skip
				if(tokens.get(startIdx).getType().equals("WS")) {
					startIdx++;
					continue;
				}
				
				finishIdx = startIdx;
				int sArrIdx = 0;
				while (finishIdx < tokens.size() && sArrIdx < sArr.size()) {
					if (tokens.get(finishIdx).getType().equals("***CSFSeparator***"))
						// if find the end, break the loop, assuming it is not found
						break;
					else if (tokens.get(finishIdx).getType().equals("WS")) {
						// if whitespace, move to next token
						finishIdx++;
					}else if (tokens.get(finishIdx).getRawText().equals(sArr.get(sArrIdx))) {
						// if match, move both token pos and segment pos
						finishIdx++;
						sArrIdx++;
						if (sArrIdx == sArr.size()) {
							isFound = true;
							break;
						}
					} else {
						// if not match, break the loop
						break;
					}
				}
				
				// check whether next iteration is necessary
				if (isFound == false)
					startIdx++;
				else
					break;
			}
			
			// get the raw text of the segment
			StringBuffer out = new StringBuffer();
			for (int i = startIdx; i < finishIdx; i++) {
				out.append(tokens.get(i).getRawText());
			}
			
			// add it to the result
			codeResult.add(out.toString());
		}

		return codeResult;
	}
}
