package commonfragmentremoval;

import java.util.ArrayList;

import support.LibTuple;

public class MergedTupleForRemoval extends
		LibTuple {
	// this is n-gram tokens per code file.
	
	private ArrayList<LibTuple> members;
	// to determine whether the tokens can be merged
	private int firstTokenIndex;

	public MergedTupleForRemoval(int firstTokenIndex,
			LibTuple p1) {
		super(p1.getText(), "MERGED", p1.getLine());
		// TODO Auto-generated constructor stub
		this.firstTokenIndex = firstTokenIndex;
		this.members = new ArrayList<>();
		this.members.add(p1);
	}

	public MergedTupleForRemoval(int firstTokenIndex,
			LibTuple p1,
			LibTuple p2) {
		this(firstTokenIndex, p1);
		this.members.add(p2);
	}

	public MergedTupleForRemoval(int firstTokenIndex,
			LibTuple p1,
			LibTuple p2,
			LibTuple p3) {
		this(firstTokenIndex, p1, p2);
		// TODO Auto-generated constructor stub
		this.members.add(p3);
	}

	public ArrayList<LibTuple> getMembers() {
		return members;
	}

	public void setMembers(
			ArrayList<LibTuple> members) {
		this.members = members;
	}

	public int getFirstTokenIndex() {
		return firstTokenIndex;
	}

	public void setFirstTokenIndex(int firstTokenIndex) {
		this.firstTokenIndex = firstTokenIndex;
	}

	public String getText() {
		String out = "";
		for (int i = 0; i < members.size(); i++) {
			Object tt = members.get(i);
			if (tt instanceof LibTuple) {
				out = out + ((LibTuple)tt).getText() + " ";
			}else{
				System.out.println(tt.toString());
			}
		}
		return out;
	}

	@Override
	public int compareTo(LibTuple arg0) {
		// TODO Auto-generated method stub
		return this.getFirstTokenIndex()
				- ((MergedTupleForRemoval) arg0)
						.getFirstTokenIndex();
	}
}
