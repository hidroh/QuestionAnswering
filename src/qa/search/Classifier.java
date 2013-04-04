package qa.search;

import java.util.ArrayList;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class Classifier {
	
	ArrayList<String> list;
	
	public Classifier(){
		list = new ArrayList<String>();
	}
	
	public ArrayList<String> Classify(ArrayList<String> input){
		
		String serializedClassifier = "lib/classifiers/english.muc.7class.distsim.crf.ser.gz";
		AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
		
		for(String snip : input){
			list.add(classifier.classifyWithInlineXML(snip));
		}

		return list;
	}
}
