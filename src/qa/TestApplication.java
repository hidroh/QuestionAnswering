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
        if (!ApplicationHelper.SHOW_ERROR) {
            System.err.close();
        }

        boolean color = false;
        if (args.length > 0 && args[0].equals("-color")) {
            color = true;
        }

        BufferedReader br = null;
        List<String> questions = new ArrayList<String>();
        List<String> answers = new ArrayList<String>();
        try {
            String line;
            br = new BufferedReader(new FileReader(new File(Settings.get("TEST_PATH"))));
            while ((line = br.readLine()) != null) {
                questions.add(ApplicationHelper.stripPunctuation(line));
            }

            br = new BufferedReader(new FileReader(new File(Settings.get("TEST_RESULT_PATH"))));
            Integer i = 1;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(i.toString())) {
                    answers.add(line);
                    i++;
                } else {
                    answers.set(i - 2, answers.get(i - 2).concat(" / " + line));
                }
            }

            br.close();

            String[] appArgs = new String[questions.size()];
            questions.toArray(appArgs);
            System.out.printf("Test data: %d questions\n", appArgs.length);
            Application.answer(appArgs, answers, color);
        } catch (IOException e) {
            ApplicationHelper.printError("Unable to load test file", e);
        }
    }
}