package qa.helper;
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
				String modelPath = new File(PosTagger.class.getProtectionDomain()
						.getCodeSource().getLocation().getPath())
						+ File.separator
						+ ".."
						+ File.separator
						+ "lib"
						+ File.separator
						+ "tagger-models"
						+ File.separator
						+ "english-left3words-distsim.tagger";
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
