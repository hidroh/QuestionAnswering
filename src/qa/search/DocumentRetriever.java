package qa.search;

import java.util.List;

import qa.model.Document;
import qa.model.QueryTerm;

/**
 * Interface for document retriever, which retrieve relevant documents based
 * on a processed query
 */
public interface DocumentRetriever {
	/**
	 * Retrieves relevant documents for given query
	 * @param query query in the form of list of query terms
	 * @return list of ranked relevant documents
	 */
	List<Document> getDocuments(List<QueryTerm> query);
}
