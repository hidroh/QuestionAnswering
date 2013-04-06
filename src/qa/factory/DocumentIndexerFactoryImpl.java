package qa.factory;

import qa.indexer.DocumentIndexer;
import qa.indexer.LuceneIndexer;

public class DocumentIndexerFactoryImpl implements DocumentIndexerFactory {

	@Override
	public DocumentIndexer createDocumentIndexer() {
		return new LuceneIndexer();
	}

}
