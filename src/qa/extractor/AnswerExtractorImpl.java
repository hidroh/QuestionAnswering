package qa.extractor;

import java.util.List;
import java.util.ArrayList;

import qa.model.Passage;
import qa.model.QuestionInfo;
import qa.model.ResultInfo;

/**
 * Interface for answer extractor component, which filters non-relevant
 * passages, extract exact answers from relevant passages and rank them
 */
public class AnswerExtractorImpl implements AnswerExtractor {
    public List<ResultInfo> extractAnswer(List<Passage> passages, 
            QuestionInfo questionInfo,
            String answerInfo) {
        return new ArrayList<ResultInfo>();
    }
}
