import java.util.ArrayList;
import java.util.List;

public class Posting {

	private List<Integer> positions;
	private Integer docId;

	public Posting(Integer docId, Integer position) {
		this.positions = new ArrayList<Integer>();
		this.positions.add(position);
		this.docId = docId;
	}

	public void add(Integer pos) {
		this.positions.add(pos);
	}

	public Integer[] getPositionsArray() {
		return positions.stream().toArray(Integer[]::new);
	}

	public Integer getTermFreq() {
		return this.positions.size();
	}

	public Integer getDocId() {
		return docId;
	}
}