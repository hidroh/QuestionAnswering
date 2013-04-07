package qa;

import java.util.List;
import java.io.File;

import qa.Settings;
import qa.helper.ApplicationHelper;
import qa.indexer.DocumentIndexer;
import qa.indexer.LuceneIndexer;
import qa.model.Document;
import qa.model.DocumentImpl;
import qa.search.DocumentRetrieverImpl;

public class IndexerApplication {
    public static void main(String[] args) {
        System.err.close();
        DocumentIndexer indexer = new LuceneIndexer();
        boolean forceIndex = args.length > 0 && args[0].equals("-f");
        if (forceIndex || !indexer.hasIndexData(Settings.get("INDEX_PATH"))) {
            try {
                File dir = new File(Settings.get("INDEX_PATH"));
                if (dir.isDirectory()) {
                    for (File f : dir.listFiles()) {
                        f.delete();
                    }
                }

                indexer.indexDocuments(Settings.get("DOCUMENT_PATH"));    
            } catch (Exception e) {
                ApplicationHelper.printError(e.getMessage());
                return;
            }
        } else {
            System.out.println("Indexed data exists.");
            System.out.println("To force reindex, use -f option.");
            System.out.println("To query, run 'java qa.IndexerApplication \"your query\"'.");
        }

        if (!forceIndex) {
            DocumentRetrieverImpl retriever = new DocumentRetrieverImpl();
            for (int i = 0; i < args.length; i++) {
                String query = args[i];
                System.out.println("Query: " + query);
                List<Document> results = retriever.getDocuments(query);
                print(results);
            }
        }
    }

    private static void print(List<Document> documents) {
        for (Document document : documents) {
            System.out.println((DocumentImpl)document);
        }
    }
}
