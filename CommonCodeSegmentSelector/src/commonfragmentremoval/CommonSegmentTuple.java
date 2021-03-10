package commonfragmentremoval;

import java.util.ArrayList;

import support.LibTuple;

public class CommonSegmentTuple implements Comparable<CommonSegmentTuple> {
	private String content;
	// content as array
	private ArrayList<String> contentArr;
	// all variations given in content as arrays
	private ArrayList<ArrayList<String>> contentArrVariations;

	// generalised content as array
	private ArrayList<String> generalisedContentArr;

	// the occurrenc frequency
	private int occFreq;

	// storing the first source of the segment
	private String firstSourcePath;

	public CommonSegmentTuple(String content, ArrayList<LibTuple> firstContent, String firstSourcePath) {
		super();
		this.content = content;
		this.contentArr = new ArrayList<>();
		this.contentArrVariations = new ArrayList<>();
		this.generalisedContentArr = new ArrayList<>();
		for (int i = 0; i < firstContent.size(); i++) {
			LibTuple c = firstContent.get(i);
			this.contentArr.add(c.getRawText());
			this.contentArrVariations.add(new ArrayList<String>());
			this.generalisedContentArr.add(c.getText());
		}
		this.occFreq = 1;

		this.firstSourcePath = firstSourcePath;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getOccFrequency() {
		return this.occFreq;
	}

	public void addNewFragmentInstance(ArrayList<LibTuple> newContent) {
		for (int i = 0; i < newContent.size(); i++) {
			LibTuple c = newContent.get(i);
			// if the token has many implementation variations, embed the new
			// variation in the contentArrVariations
			if (c.getRawText().hashCode() != this.contentArr.get(i).hashCode()
					&& c.getRawText().equals(this.contentArr.get(i)) == false) {
				// if no such element in the variations, add it
				if (this.contentArrVariations.get(i).contains(c.getRawText()) == false)
					this.contentArrVariations.get(i).add(c.getRawText());
			}
		}
		this.occFreq++;
	}

	public int getScore() {
		return this.getOccFrequency() * this.contentArr.size();
	}

	@Override
	public int compareTo(CommonSegmentTuple arg0) {
		// TODO Auto-generated method stub
		if (this.getOccFrequency() != arg0.getOccFrequency())
			return -(this.getOccFrequency() - arg0.getOccFrequency());
		else
			return -(this.getScore() - arg0.getScore());
	}

	public String getSmartContent() {
		/*
		 * which automatically generalise some tokens if they are written in different
		 * format
		 */
		String out = "";
		boolean isWithVariation = false;

		// print the string in summary
		for (int i = 0; i < contentArr.size(); i++) {
			String s = contentArr.get(i);
			// if there are many implementations, print a variable name
			if (contentArrVariations.get(i).size() > 0) {
				out = out + "$var_" + i + "$ ";
				isWithVariation = true;
			} else
				out = out + s + " ";
		}

		if (isWithVariation) {
			out = out + "\n\tImplementation variations:";
			// explain the variables
			for (int i = 0; i < contentArr.size(); i++) {
				String s = contentArr.get(i);
				// if there are many implementations, print all of it
				if (contentArrVariations.get(i).size() > 0) {
					// print the variable name and the possible values
					out = out + "\n\t\t" + "$var_" + i + "$ = { " + s;
					for (String t : contentArrVariations.get(i)) {
						// print each variation
						out = out + " | " + t;
					}
					out = out + " }";
				}
			}
		}

		out = out + "\n\tFully-generalised form: ";
		// show the generalised form
		for (int i = 0; i < generalisedContentArr.size(); i++) {
			// get the generalised form
			out = out + generalisedContentArr.get(i) + " ";
		}

		return out;
	}

	public String toString() {
		return this.getSmartContent() + ":" + this.getScore();
	}

	public String getFirstSourcePath() {
		return firstSourcePath;
	}

	public void setFirstSourcePath(String firstSourcePath) {
		this.firstSourcePath = firstSourcePath;
	}

	public ArrayList<String> getFirstActualSegment() {
		// get the first actual segment for this
		return contentArr;
	}
}
