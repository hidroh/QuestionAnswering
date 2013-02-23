package qa.model;

import java.util.List;

import qa.model.enumerator.QueryType;

/**
 * Interface for question information model, which contains information for
 * raw questions 
 */
public interface QuestionInfo {
	/**
	 * Gets query terms representing this question
	 * @return list of expanded query terms
	 */
	List<QueryTerm> getQuestionTerms();
	
	/**
	 * Get query type of this question 
	 * @return query type
	 */
	QueryType getQueryType();
}
