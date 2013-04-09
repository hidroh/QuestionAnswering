package qa.factory;

import qa.extractor.AnswerExtractor;
import qa.extractor.AnswerExtractorImpl;
import qa.search.web.WebSearchApplication;
import qa.helper.ApplicationHelper;

public class AnswerExtractorFactoryImpl implements AnswerExtractorFactory {

	@Override
	public AnswerExtractor createAnswerExtractor() {
        WebSearchApplication webSearchApp = ApplicationHelper.getWebSearchApplication();

		return new AnswerExtractorImpl(webSearchApp);
	}

}
