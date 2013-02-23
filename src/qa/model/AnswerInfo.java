package qa.model;

import java.util.List;

/**
 * Interface for answer information model, which contains information for 
 * answers returned from the web
 */
public interface AnswerInfo {
	/**
	 * Gets query terms for this answer
	 * @return list of query terms
	 */
	List<QueryTerm> getAnswerTerms();
}
