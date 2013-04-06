package qa.parser;

import java.util.List;

import qa.ClassifierApplication;
import qa.model.QueryTerm;
import qa.model.QuestionInfo;
import qa.model.enumerator.QueryType;

public class QuestionParserImpl implements QuestionParser {

	@Override
	public QuestionInfo parse(String question) {
		QuestionInfo questionInfo = ClassifierApplication.classify(question);
		return questionInfo;
	}

	@Override
	public List<QueryTerm> getQuery(String question) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryType getType(String question) {
		// TODO Auto-generated method stub
		return null;
	}
}
