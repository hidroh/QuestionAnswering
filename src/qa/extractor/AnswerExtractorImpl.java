package qa.extractor;

import java.util.List;
import java.util.ArrayList;

import qa.model.QuestionInfo;
import qa.model.ResultInfo;
import qa.model.Passage;
import qa.model.ResultInfoImpl;
import qa.helper.NeRecognizer;
import qa.model.enumerator.QueryType;
import qa.model.enumerator.QuerySubType;

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
            String answer = getAnswer(passage.getContent(), questionInfo);
            if (answer.length() > 0) {
                results.add(new ResultInfoImpl(answer, passage.getDocumentId()));    
            }
        }

        return results;
    }

    private String getAnswer(String passage, QuestionInfo info) {
        String answer = "";
        List<String> nameEntities = NeRecognizer.getInstance().getNameEntities(passage);

        return mapAnswer(nameEntities, info);
    }

    private String mapAnswer(List<String> answers, QuestionInfo info) {
        String result = "";
        List<String> types = getEntityType(info);
        if (types.size() > 0) {
            for (String answer : answers) {
                for (String type : types) {
                    if (answer.contains("<" + type + ">")) {
                        result += answer.replace("<" + type + ">", "").replace("</" + type + ">", "") + " / ";
                    }
                }
            }
        }

        return result.trim();
    }

    private List<String> getEntityType(QuestionInfo info) {
        List<String> types = new ArrayList<String>();
        switch (info.getQueryType()) {
            case LOC: // Location
                types.add("LOCATION");
                break;
            case HUM:
                switch (info.getQuerySubType()) {
                    case HUM_ind: // Person
                    case HUM_desc:
                    case HUM_title:
                        types.add("PERSON");
                        break;
                    case HUM_gr: // Organization
                        types.add("ORGANIZATION");
                        break;
                }
                break;
            case NUM:
                switch (info.getQuerySubType()) {
                    case NUM_date: // Time / Date
                    case NUM_period:
                        types.add("TIME");
                        types.add("DATE");
                        break;
                    case NUM_money: // Money
                        types.add("MONEY");
                        break;
                    case NUM_perc: // Percent
                        types.add("PERCENT");
                        break;
                }
                break;
        }

        return types;
    }
}
