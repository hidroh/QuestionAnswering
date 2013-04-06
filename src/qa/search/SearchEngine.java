package qa.search;

import qa.model.QuestionInfo;

/**
 * Interface for search engine, which searches the web for possible answers
 * to a given processed query
 */
public interface SearchEngine {
	/**
	 * Returns ranked answers for a given question
	 * @param question given question
	 * @return list of ranked answers
	 */
	String search(QuestionInfo question);
}
