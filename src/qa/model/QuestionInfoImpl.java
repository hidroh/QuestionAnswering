package qa.model;

import java.util.List;

import qa.model.enumerator.QueryType;

public class QuestionInfoImpl implements QuestionInfo {
	private List<QueryTerm> terms;
	private QueryType queryType;
	private String raw;
	
	public QuestionInfoImpl(QueryType queryType, List<QueryTerm> terms, String raw) {
		this.terms = terms;
		this.queryType = queryType;
		this.raw = raw;
	}
	
	@Override
	public List<QueryTerm> getQuestionTerms() {
		return terms;
	}

	@Override
	public QueryType getQueryType() {
		return queryType;
	}

	@Override
	public String getRaw() {
		return raw;
	}

	public String toString() {
		String str = "[" + queryType.toString() + "] {";
		for (int i = 0; i < terms.size(); i++) {
			str += terms.get(i).getText();
			if (i < terms.size() - 1) {
				str += ", ";
			}
		}
		
		str += "}";
		return str;
	}
}
