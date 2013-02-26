package qa.model;

import java.util.List;

import qa.model.enumerator.QueryType;

public class QuestionInfoImpl implements QuestionInfo {
	private List<QueryTerm> terms;
	private QueryType queryType;
	
	public QuestionInfoImpl(QueryType queryType, List<QueryTerm> terms) {
		this.terms = terms;
		this.queryType = queryType;
	}
	
	@Override
	public List<QueryTerm> getQuestionTerms() {
		return terms;
	}

	@Override
	public QueryType getQueryType() {
		return queryType;
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
