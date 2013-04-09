package qa.search.web;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class GoogleApplication extends WebSearchApplication {
    public GoogleApplication() {
        super("Google", "http://www.google.com/search?tbs=cdr:1,cd_max:2000&q=", "&start=10");
    }

    @Override
    protected Elements extractResults(List<Document> docs) {
        Elements elemResults = new Elements();
        for (Document doc : docs) {
            elemResults.addAll(doc.select("li[class=g]").select("h3[class=r]"));
            elemResults.addAll(doc.select("li[class=g]").select("span[class=st]"));
        }

        return elemResults;
    }
}