package qa.extractor;

import java.util.List;
import java.util.ArrayList;

import qa.model.QuestionInfo;
import qa.model.ResultInfo;
import qa.model.Passage;
import qa.model.ResultInfoImpl;

/**
 * Interface for answer extractor component, which filters non-relevant
 * passages, extract exact answers from relevant passages and rank them
 */
public class AnswerExtractorImpl implements AnswerExtractor {
    public List<ResultInfo> extractAnswer(List<Passage> passages, 
            QuestionInfo questionInfo,
            String answerInfo) {
        List<ResultInfo> results = new ArrayList<ResultInfo>();
        for (Passage passage : passages) {
            results.add(new ResultInfoImpl(passage.getContent(), passage.getDocumentId()));
        }

        return results;
    }
}
