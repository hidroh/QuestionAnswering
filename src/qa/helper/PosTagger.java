package qa.helper;

import qa.Settings;
import java.io.File;
import java.io.IOException;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class PosTagger {

	private static PosTagger instance;
	private MaxentTagger tagger;

	private PosTagger() {
		boolean instantiated = false;
		while (!instantiated) {
			try {
				String modelPath = Settings.get("POS_TAGGER_MODEL_PATH");
//				System.out.println(modelPath);
				try {
					tagger = new MaxentTagger(modelPath);
					instantiated = true;
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
			} catch (java.lang.OutOfMemoryError e) {
				System.err.println("Out of memory! Retrying...");
				System.gc();
			}
		}
	}

	public static PosTagger getInstance() {
		if (instance == null) {
			instance = new PosTagger();
		}
		
		return instance;
	}
	
	public String tag(String input) {
		return tagger.tagString(input);
	}
	
//	public static void main(String[] args) {
//		System.out.print(PosTagger.getInstance().tag("What county is Modesto , California in ?"));
//	}

}
