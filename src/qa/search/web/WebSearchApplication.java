package qa.search.web;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import qa.helper.ApplicationHelper;

public abstract class WebSearchApplication {
    protected String url;
    protected String pagination;
    private final String userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    private final String referrer = "http://www.bing.com";
    protected String searchEngineName;

    protected WebSearchApplication(String searchEngineName, String url, String pagination) {
        this.searchEngineName = searchEngineName;
        this.url = url;
        this.pagination = pagination;
    }

    public String search(String query) {
        try {
            String result = "";
            query = ApplicationHelper.stripPunctuation(query).trim().replace(" ", "+");
            List<String> queries = new ArrayList<String>();
            queries.add(query);
            queries.add(query + pagination);
            List<Document> rawResults = httpGet(queries);
            Elements elemResults = extractResults(rawResults);
            Iterator<Element> itr = elemResults.iterator();

            //Prints only what you need, ie the result details
            while(itr.hasNext()){
                result += ((Element)itr.next()).text() + " ";
            }

            return result.trim();
        } catch (IOException e) {
            ApplicationHelper.printWarning(String.format("Answer Extractor: Unable to validate answer - %s", e.getMessage()));
        }

        return "";
    }

    protected abstract Elements extractResults(List<Document> docs);

    private List<Document> httpGet(List<String> queries) throws IOException {
        List<Document> docs = new ArrayList<Document>();
        for (String query : queries) {
            docs.add(Jsoup.connect(url + query)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .get());
        }

        return docs;
    }
}