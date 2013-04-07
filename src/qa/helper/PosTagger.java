package qa.helper;

import qa.Settings;
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
				try {
					tagger = new MaxentTagger(modelPath);
					instantiated = true;
				} catch (ClassNotFoundException e) {
					ApplicationHelper.printError("", e);
				} catch (IOException e) {
					ApplicationHelper.printError("POS Tagger: Unable to load model file", e);
				}
			} catch (java.lang.OutOfMemoryError e) {
				ApplicationHelper.printError("POS Tagger: Out of memory");
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

	// public static void main(String[] args) {
	// System.out.print(PosTagger.getInstance().tag("What county is Modesto , California in ?"));
	// }

}
