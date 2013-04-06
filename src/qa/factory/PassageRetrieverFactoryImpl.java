package qa.factory;

import qa.search.PassageRetriever;
import qa.search.PassageRetrieverImpl;
import qa.model.Document;

public class PassageRetrieverFactoryImpl implements PassageRetrieverFactory {

	@Override
	public PassageRetriever createPassageRetriever(Document document) {
		return new PassageRetrieverImpl(document);
	}

}
