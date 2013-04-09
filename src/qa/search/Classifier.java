package qa.search;

import java.util.ArrayList;

import qa.helper.NeRecognizer;

public class Classifier {
	
	ArrayList<String> list;
	
	public Classifier(){
		list = new ArrayList<String>();
	}
	
	public ArrayList<String> Classify(ArrayList<String> input){
		for(String snip : input){
			list.add(NeRecognizer.getInstance().tag(snip));
		}

		return list;
	}
}
