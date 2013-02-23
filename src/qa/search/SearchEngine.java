package qa.search;

import java.util.List;

import qa.model.AnswerInfo;
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
	List<AnswerInfo> search(QuestionInfo question);
}
