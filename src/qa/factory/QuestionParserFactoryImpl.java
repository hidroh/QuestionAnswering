package qa.factory;

import qa.parser.QuestionParser;
import qa.parser.QuestionParserImpl;

public class QuestionParserFactoryImpl implements QuestionParserFactory {
	private static QuestionParser instance;
	
	@Override
	public QuestionParser createQuestionParser() {
		if (instance == null) {
			instance = new QuestionParserImpl();
		}
		
		return instance;
	}

}
