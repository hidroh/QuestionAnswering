package qa;

import java.util.List;

import qa.Settings;
import qa.indexer.DocumentIndexer;
import qa.indexer.LuceneIndexer;
import qa.model.Document;
import qa.model.DocumentImpl;
import qa.search.DocumentRetrieverImpl;

public class IndexerApplication {
    public static void main(String[] args) {
        DocumentIndexer indexer = new LuceneIndexer();
        if (!indexer.hasIndexData(Settings.get("INDEX_PATH"))) {
            try {
                indexer.indexDocuments(Settings.get("DOCUMENT_PATH"));    
            } catch (Exception e) {
                System.err.println(e.getMessage());
                return;
            }
        } else {
            System.out.println("Indexed data exists. To query, run 'java qa.IndexerApplication \"your query\"'");
        }

        DocumentRetrieverImpl retriever = new DocumentRetrieverImpl();
        for (int i = 0; i < args.length; i++) {
            String query = args[i];
            System.out.println("Query: " + query);
            List<Document> results = retriever.getDocuments(query);
            print(results);
        }
    }

    private static void print(List<Document> documents) {
        for (Document document : documents) {
            System.out.println((DocumentImpl)document);
        }
    }
}
