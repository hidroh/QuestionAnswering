package qa.model;

import java.util.List;

import qa.model.enumerator.QuerySubType;
import qa.model.enumerator.QueryType;

public class QuestionInfoImpl implements QuestionInfo {
	private List<QueryTerm> terms;
	private QueryType queryType;
	private QuerySubType querySubType;
	private String raw;
	private String multiClassification;
	
	public QuestionInfoImpl(QueryType queryType, QuerySubType querySubType, List<QueryTerm> terms, String raw) {
		this.terms = terms;
		this.queryType = queryType;
		this.querySubType = querySubType;
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
	public QuerySubType getQuerySubType() {
		return querySubType;
	}

	@Override
	public String getRaw() {
		return raw;
	}

	public String getMultiClassification() {
		return multiClassification;
	}

	public void setMultiClassification(String multiClassification) {
		this.multiClassification = multiClassification;
	}

	public String toString() {
		String str = "[" + queryType.toString() + ":" + querySubType.toString() + "] {";
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
