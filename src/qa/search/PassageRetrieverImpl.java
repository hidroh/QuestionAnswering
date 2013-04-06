package qa.search;

import java.util.List;
import java.util.ArrayList;

import qa.model.Document;
import qa.model.Passage;

public class PassageRetrieverImpl implements PassageRetriever {
    public List<Passage> getPassages(Document document, String answerInfo) {
        return new ArrayList<Passage>();
    }
}
