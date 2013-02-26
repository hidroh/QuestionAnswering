package qa.model;

public class QueryTermImpl implements QueryTerm {
	private String text;
	
	public QueryTermImpl(String text) {
		this.text = text;
	}

	@Override
	public String getText() {
		return text;
	}

}
