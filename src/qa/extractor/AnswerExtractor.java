package qa.extractor;

import java.util.List;

import qa.model.QuestionInfo;
import qa.model.ResultInfo;
import qa.model.Passage;

/**
 * Interface for answer extractor component, which filters non-relevant
 * passages, extract exact answers from relevant passages and rank them
 */
public interface AnswerExtractor {
	/**
	 * Extracts exact answers from list of passages (both relevant and non-
	 * relevant) based on query type and question keywords
	 * @param passages list of passages
	 * @param questionInfo question information
	 * @param answerInfo answer information
	 * @return list of ranked results
	 */
	List<ResultInfo> extractAnswer(List<Passage> passages, 
			QuestionInfo questionInfo,
			String answerInfo);
}
