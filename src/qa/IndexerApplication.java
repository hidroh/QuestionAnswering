package qa;

import qa.Settings;
import qa.indexer.DocumentIndexer;
import qa.indexer.LuceneIndexer;
import qa.indexer.DocumentRetrieverImpl;

public class IndexerApplication {
    public static void main(String[] args) {
        DocumentIndexer indexer = new LuceneIndexer();
        if (!indexer.hasIndexData(Settings.get("INDEX_PATH"))) {
            indexer.indexDocuments(Settings.get("DOCUMENT_PATH"));  
        }

        DocumentRetrieverImpl retriever = new DocumentRetrieverImpl();
        String query = "lenihan";

        retriever.getDocuments(query);
    }
}
