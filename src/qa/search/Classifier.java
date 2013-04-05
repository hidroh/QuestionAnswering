package qa.search;

import java.util.ArrayList;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

import qa.Settings;

public class Classifier {
	
	ArrayList<String> list;
	
	public Classifier(){
		list = new ArrayList<String>();
	}
	
	public ArrayList<String> Classify(ArrayList<String> input){
		
		String serializedClassifier = Settings.get("CLASSIFIER_PATH_SEARCH_ENGINE");
		AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
		
		for(String snip : input){
			list.add(classifier.classifyWithInlineXML(snip));
		}

		return list;
	}
}
