package qa.search;

import java.util.List;

import qa.model.Document;

/**
 * Interface for document retriever, which retrieve relevant documents based
 * on a processed query
 */
public interface DocumentRetriever {
	/**
	 * Retrieves relevant documents for given query
	 * @param query query string
	 * @return list of ranked relevant documents
	 */
	List<Document> getDocuments(String query);
}
