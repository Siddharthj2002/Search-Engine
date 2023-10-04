import java.util.ArrayList;
import java.util.List;

public class PostingList {
	List<Posting> postings;
	private int postingsIdx;

	public PostingList() {
		postings = new ArrayList<Posting>();
		postingsIdx = -1;
	}

	public void startIteration() {
		postingsIdx = 0;
	}

	public boolean hasMore() {
		return (postingsIdx >= 0 && postingsIdx < postings.size());
	}

	public void skipTo(int docid) {
		while (postingsIdx < postings.size() &&
				getCurrentPosting().getDocId() < docid) {
			postingsIdx++;
		}
	}

	public Posting getCurrentPosting() {
		Posting retval = null;
		try {
			retval = postings.get(postingsIdx);
		} catch (IndexOutOfBoundsException ex) {
		}
		return retval;
	}

	public int documentCount() {
		return postings.size();
	}

	public void add(Posting posting) {
		postings.add(posting);
		postingsIdx++;
	}

	public void add(Integer docid, Integer position) {
		Posting current = getCurrentPosting();
		if (current != null && current.getDocId().equals(docid)) {
			current.add(position);
		} else {
			Posting posting = new Posting(docid, position);
			add(posting);
		}
	}

	public int termFrequency() {
		return postings.stream().mapToInt(p -> p.getTermFreq()).sum();
	}
}
