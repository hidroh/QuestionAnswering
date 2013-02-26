package qa.factory;

import qa.indexer.DocumentRetrieverImpl;
import qa.search.DocumentRetriever;

public class DocumentRetrieverFactoryImpl implements DocumentRetrieverFactory {
	private DocumentRetriever instance; 
	
	@Override
	public DocumentRetriever createDocumentRetriever() {
		if (instance == null) {
			instance = new DocumentRetrieverImpl();
		}
		
		return instance;
	}

}
