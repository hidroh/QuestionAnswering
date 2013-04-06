package qa.factory;

import qa.extractor.AnswerExtractor;
import qa.extractor.AnswerExtractorImpl;

public class AnswerExtractorFactoryImpl implements AnswerExtractorFactory {

	@Override
	public AnswerExtractor createAnswerExtractor() {
		return new AnswerExtractorImpl();
	}

}
