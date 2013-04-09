package qa;

import java.io.IOException;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import qa.helper.ApplicationHelper;

public class GoogleApplication {

    public static String search(String query) {
        try {
            ApplicationHelper.printDebug(String.format("Search google for \"%s\n\"", query));
            String result = "";
            query = ApplicationHelper.stripPunctuation(query).trim().replace(" ", "+");
            String url = "http://www.google.com/search?q=";
            String page2 = "&start=10";
            Document doc;
            doc = Jsoup.connect(url + query)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get();
            Elements elemResults = new Elements();
            elemResults.addAll(doc.select("li[class=g]").select("h3[class=r]"));
            elemResults.addAll(doc.select("li[class=g]").select("span[class=st]"));
            Iterator<Element> itr = elemResults.iterator();

            //Prints only what you need, ie the result details
            while(itr.hasNext()){
                result += ((Element)itr.next()).text() + " ";
            }

            return result.trim();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }
}