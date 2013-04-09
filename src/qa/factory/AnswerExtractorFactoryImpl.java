package qa.factory;

import qa.extractor.AnswerExtractor;
import qa.extractor.AnswerExtractorImpl;
import qa.search.web.WebSearchApplication;
import qa.search.web.GoogleApplication;
import qa.search.web.BingApplication;
import qa.Settings;

public class AnswerExtractorFactoryImpl implements AnswerExtractorFactory {

	@Override
	public AnswerExtractor createAnswerExtractor() {
        WebSearchApplication webSearchApp = null;
        String searchEngine = Settings.get("SEARCH_ENGINE");
        if (searchEngine.toLowerCase().equals("google")) {
            webSearchApp = new GoogleApplication();
        } else if (searchEngine.toLowerCase().equals("bing")) {
            webSearchApp = new BingApplication();
        }

		return new AnswerExtractorImpl(webSearchApp);
	}

}
