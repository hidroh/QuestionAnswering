package qa.factory;

import qa.search.SearchEngine;
import qa.search.SearchEngineImpl;

public class SearchEngineFactoryImpl implements SearchEngineFactory {
	private SearchEngine instance;
	
	@Override
	public SearchEngine createSearchEngine() {
		if (instance == null) {
			instance = new SearchEngineImpl();
		}
		
		return instance;
	}

}
