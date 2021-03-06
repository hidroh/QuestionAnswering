package qa.helper;

import qa.Settings;
import qa.search.web.WebSearchApplication;
import qa.search.web.GoogleApplication;
import qa.search.web.BingApplication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationHelper {
    public static boolean SHOW_DEBUG = Boolean.parseBoolean(Settings.get("SHOW_DEBUG_INFO"));
    public static boolean SHOW_ERROR = Boolean.parseBoolean(Settings.get("SHOW_ERROR"));
    public static boolean QUERY_REFORMULATION = Boolean.parseBoolean(Settings.get("QUERY_REFORMULATION"));
    private static WebSearchApplication webSearchApp;

    public static void printDebug(String debugString) {
        if (SHOW_DEBUG) {
            System.out.print(debugString);    
        }
    }

    public static void printlnDebug(String debugString) {
        if (SHOW_DEBUG) {
            System.out.println(debugString);    
        }
    }

    public static void printError(String error) {
        System.out.printf("[ERROR] %s\n", error);
        System.exit(-1);
    }

    public static void printError(String message, Exception e) {
        System.out.printf("[ERROR] %s\n%s\n", message, e.getMessage());
        if (SHOW_DEBUG) {
            for (StackTraceElement s : e.getStackTrace()) {
                System.out.println(s);
            }
        }

        System.exit(-1);
    }

    public static void printWarning(String warning) {
        System.out.printf("[WARNING] %s\n", warning);   
    }

    public static String stripPunctuation(String text) {
        String result = "";
        text = text.replace(".", ""); // for abbr

        Pattern wordPattern = Pattern.compile("((?:\\w+))",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = wordPattern.matcher(text);

        while (m.find()) {
            result += m.group() + " ";
        }

        return result;
    }

    public static WebSearchApplication getWebSearchApplication() {
        String searchEngine = Settings.get("SEARCH_ENGINE");
        if (searchEngine.toLowerCase().equals("google")) {
            webSearchApp = new GoogleApplication();
        } else if (searchEngine.toLowerCase().equals("bing")) {
            webSearchApp = new BingApplication();
        }

        return webSearchApp;
    }
}
