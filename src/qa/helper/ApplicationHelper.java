package qa.helper;

import qa.Settings;

public class ApplicationHelper {
    public static boolean SHOW_DEBUG = Boolean.parseBoolean(Settings.get("SHOW_DEBUG_INFO"));

    public static void printDebug(String debugString) {
        if (SHOW_DEBUG) {
            System.out.print(debugString);    
        }
    }

    public static void printError(String error) {
        System.out.printf("[ERROR] %s\n", error);
    }

    public static void printWarning(String warning) {
        System.out.printf("[WARNING] %s\n", warning);   
    }
}
