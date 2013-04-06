package qa.factory;

import qa.search.PassageRetriever;
import qa.model.Document;

public interface PassageRetrieverFactory {
	PassageRetriever createPassageRetriever(Document document);
}
