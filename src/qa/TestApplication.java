package qa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import qa.Settings;
import qa.helper.ApplicationHelper;

public class TestApplication {
    public static void main(String[] args) {
        BufferedReader br = null;
        List<String> lines = new ArrayList<String>();
        try {
            String line;
            br = new BufferedReader(new FileReader(new File(Settings.get("TEST_PATH"))));
            while ((line = br.readLine()) != null) {
                lines.add(ApplicationHelper.stripPunctuation(line));
            }

            String[] appArgs = new String[lines.size()];
            lines.toArray(appArgs);
            System.out.printf("Test data: %d questions\n", appArgs.length);
            Application.main(appArgs);
        } catch (IOException e) {
            ApplicationHelper.printError("Unable to load test file", e);
        }
    }
}