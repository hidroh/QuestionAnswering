package qa.search;

import java.util.List;

import qa.indexer.DocumentIndexer;
import qa.model.Document;
import qa.model.QueryTerm;

/**
 * Interface for document retriever, which retrieve relevant documents based
 * on a processed query
 */
public interface DocumentRetriever {
	/**
	 * Sets indexer to be used by document retriever
	 * @param indexer indexer to be used
	 */
	void setIndexer(DocumentIndexer indexer);
	
	/**
	 * Retrieves relevant documents for given query
	 * @param query query in the form of list of query terms
	 * @return list of ranked relevant documents
	 */
	List<Document> getDocuments(List<QueryTerm> query);
	
	/**
	 * Imports document sets to be queried upon
	 * @param documentPath path to root folder that contains all documents
	 */
	void importDocuments(String documentPath);
}
