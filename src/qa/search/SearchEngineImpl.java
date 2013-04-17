package qa.search;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Arrays;

import qa.model.QuestionInfo;
import qa.helper.ApplicationHelper;
import qa.helper.ChunkerWrapper;

public class SearchEngineImpl implements SearchEngine {
    public String search(QuestionInfo question) {
        ApplicationHelper.printDebug(String.format("Search web for \"%s\"\n", question.getRaw()));
        String webResult = ApplicationHelper.getWebSearchApplication().search(question.getRaw());
        Set<String> webChunks = ChunkerWrapper.getInstance().getSortedChunks(webResult);
        List<String> expandedTerms = new ArrayList<String>();
        expandedTerms.addAll(webChunks);
        question.setExpandedTerms(expandedTerms);
        List<String> results = new ArrayList<String>();
        for (String term : question.getQueryTerms()) {
            results.add(term.toLowerCase());
        }

        for (String webChunk : webChunks) {
            if (!results.contains(webChunk.toLowerCase())) {
                results.add(webChunk);
            }
        }
        ApplicationHelper.printlnDebug("IR query terms = " + results);
        return ApplicationHelper.join(results, " ");
    }
}