package qa.search;

import java.util.List;
import java.util.Set;

import qa.model.QuestionInfo;
import qa.helper.ApplicationHelper;
import qa.helper.ChunkerWrapper;

public class SearchEngineImpl implements SearchEngine {
    public String search(QuestionInfo question) {
        String webResult = ApplicationHelper.getWebSearchApplication().search(question.getRaw());
        Set<String> webChunks = ChunkerWrapper.getInstance().getSortedChunks(webResult);
        List<String> queryChunks = ChunkerWrapper.getInstance().getChunks(question.getRaw());
        for (int i = 0; i < queryChunks.size(); i++) {
            queryChunks.set(i, queryChunks.get(i).toLowerCase());
        }

        for (String webChunk : webChunks) {
            if (!queryChunks.contains(webChunk.toLowerCase())) {
                queryChunks.add(webChunk);
            }
        }
        ApplicationHelper.printlnDebug("IR query terms = " + queryChunks);
        return ApplicationHelper.join(queryChunks, " ");
    }

}