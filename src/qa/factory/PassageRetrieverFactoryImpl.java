package qa.factory;

import qa.search.PassageRetriever;
import qa.search.PassageRetrieverImpl;

public class PassageRetrieverFactoryImpl implements PassageRetrieverFactory {

	@Override
	public PassageRetriever createPassageRetriever() {
		return new PassageRetrieverImpl();
	}

}
