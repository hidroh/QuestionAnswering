package qa.factory;

import qa.search.DocumentRetriever;
import qa.search.DocumentRetrieverImpl;

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
