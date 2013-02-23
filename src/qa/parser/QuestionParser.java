package qa.parser;

import java.util.List;

import qa.model.QueryTerm;
import qa.model.QuestionInfo;
import qa.model.enumerator.QueryType;

/**
 * Interface for question parser, which parses question to retrieve a query
 * suitable to be used in subsequent steps of system, as well as deriving
 * query type
 */
public interface QuestionParser {
	/**
	 * Parses a given question to get a suitable query and query type
	 * @param question raw question
	 * @return list of query terms and query type
	 */
	QuestionInfo parse(String question);
	
	/**
	 * Gets list of expanded query terms for given question
	 * @param question raw question
	 * @return list of query terms
	 */
	List<QueryTerm> getQuery(String question);
	
	/**
	 * Get query type of given question
	 * @param question raw question
	 * @return corresponding query type
	 */
	QueryType getType(String question);
}
