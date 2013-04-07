package qa.helper;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

import qa.Settings;

public class NeRecognizer {
    private static NeRecognizer instance;
    private AbstractSequenceClassifier<CoreLabel> classifier;

    private NeRecognizer() {
        String serializedClassifier = Settings.get("CLASSIFIER_PATH_SEARCH_ENGINE");
        classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
    }

    public static NeRecognizer getInstance() {
        if (instance == null) {
            instance = new NeRecognizer();
        }

        return instance;
    }

    public List<String> getNameEntities(String text) {
        text = stripPunctuation(text);
        List<String> results = new ArrayList<String>();
        
        String tagged = classifier.classifyWithInlineXML(text);

        Pattern taggedPattern = Pattern.compile("((?:<\\w+>[^<]*</\\w+>))",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = taggedPattern.matcher(tagged);
        while (m.find()) {
            String entity = m.group();
            results.add(entity);
        }

        return results;
    }

    private String stripPunctuation(String text) {
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
}