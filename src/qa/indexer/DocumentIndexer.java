package qa.indexer;

/**
 * Interface for document indexer document, which indexes data set for faster
 * and more efficient retrieval
 */
public interface DocumentIndexer {
    void importDocuments(String documentPath);
}
