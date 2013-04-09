package qa.search.web;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class BingApplication extends WebSearchApplication {
    public BingApplication() {
        super("Bing", "http://www.bing.com/search?q=", "&first=11");
    }

    @Override
    protected Elements extractResults(List<Document> docs) {
        Elements elemResults = new Elements();
        for (Document doc : docs) {
            elemResults.addAll(doc.select("h3"));
            elemResults.addAll(doc.select("p"));
        }

        return elemResults;
    }

}