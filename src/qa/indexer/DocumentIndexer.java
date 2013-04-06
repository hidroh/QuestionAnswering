package qa.indexer;

/**
 * Interface for document indexer document, which indexes data set for faster
 * and more efficient retrieval
 */
public interface DocumentIndexer {
    /**
     * Imports document sets to be queried upon
     * @param documentPath path to root folder that contains all documents
     */
    void indexDocuments(String documentPath) throws Exception;

    boolean hasIndexData(String indexPath);
}
