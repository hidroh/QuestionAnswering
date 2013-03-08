package qa;

import java.io.File;
import java.lang.ClassNotFoundException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Properties;

import qa.classifier.QuestionClassifier;
import qa.classifier.QuestionClassifierImpl;
import qa.helper.ClassifierHelper;
import qa.model.ClassifierTrainingInfo;
import qa.model.QuestionInfo;
import qa.model.enumerator.QueryType;

public class ClassifierApplication {

	public static Properties Settings;

	/**
	 * @param args
	 *            array of input questions
	 */
	public static void main(String[] args) {
		loadProperties();
		if (args.length == 0) {
			printUsage();
			return;
		}

		if (args[0].equals("-train")) {
			train();
		} else if (args[0].equals("-eval")) {
			evaluate();
		} else {
			classify(args);
		}

	}

	private static void evaluate() {
		ClassifierHelper helper = ClassifierHelper.getInstance();
		List<QuestionInfo> testData;
		try {
			testData = helper.getAnnotatedData(
					ClassifierApplication.Settings.getProperty("TEST_CORPUS_PATH"),
					ClassifierApplication.Settings
							.getProperty("TEST_CORPUS_PREFIX"),
					ClassifierApplication.Settings.getProperty("TEST_CORPUS_EXT"));
			boolean SUPPRESS_LOG = true;
			QuestionClassifier qc = new QuestionClassifierImpl(SUPPRESS_LOG);
			ClassifierTrainingInfo trainingInfo = loadClassifier();
			if (trainingInfo != null) {
				int correct = 0;
				for (QuestionInfo question : testData) {
//					System.out.printf("\nQ: \"%s\"\n", question.getRaw());
//					System.out.printf(
//							"Classified as: %s\n",
//							qc.apply(helper.getAllQueryTypes(), trainingInfo,
//									question.getRaw()));
					QueryType classified = qc.apply(helper.getAllQueryTypes(),
							trainingInfo, question.getRaw());
					QueryType expected = question.getQueryType();
					if (classified == expected) {
						correct++;
					}
				}

				System.out.printf("Evaluation result: %d / %d = %.2f", correct,
						testData.size(), (double)correct / testData.size());			
			} else {
				System.err.println("Operation halted, unable to retrieve trained data");
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

	private static void classify(String[] args) {
		if (args.length == 0) {
			return;
		}

		ClassifierTrainingInfo trainingInfo = loadClassifier();
		if (trainingInfo != null) {
			boolean SUPPRESS_LOG = false;
			QuestionClassifier qc = new QuestionClassifierImpl(SUPPRESS_LOG);
			for (int i = 0; i < args.length; i++) {
				String question = args[i];
				ClassifierHelper helper = ClassifierHelper.getInstance();
				System.out.printf("\nQ: \"%s\"\n", question);
				System.out
						.printf("Classified as: %s\n", qc.apply(
								helper.getAllQueryTypes(), trainingInfo, question));

			}			
		} else {
			System.err.println("Operation halted, unable to retrieve trained data");
		}
	}

	private static void printUsage() {
		System.out
				.println("Usage: java ClassifierApplication <options> \"<question1>\" \"<question2>\"... ");
		System.out.println("where possible options include:");
		System.out.printf("  %-15s %s\n", "-train",
				"Only train question classifier, no input questions required");
		System.out
				.printf("  %-15s %s\n", "-eval",
						"Only evaluate question classifier, no input questions required");
		System.out.println();
		System.out
				.println("Run configuration stored in '/Application.properties'");
		System.out
				.println("Example: java ClassifierApplication \"Where is Milan ?\" \"Who developed the Macintosh computer ?\" ");
	}

	private static void train() {
		ClassifierHelper helper = ClassifierHelper.getInstance();
		List<QuestionInfo> trainingData;
		try {
			trainingData = helper
					.getAnnotatedData(ClassifierApplication.Settings
							.getProperty("TRAIN_CORPUS_PATH"),
							ClassifierApplication.Settings
									.getProperty("TRAIN_CORPUS_PREFIX"),
							ClassifierApplication.Settings
									.getProperty("TRAIN_CORPUS_EXT"));
			boolean SUPPRESS_LOG = true;
			QuestionClassifier qc = new QuestionClassifierImpl(SUPPRESS_LOG);
			ClassifierTrainingInfo trainingInfo = qc.train(
					helper.getAllQueryTypes(), trainingData);

			try {
				File classifierOutput = new File(
						ClassifierApplication.Settings
								.getProperty("CLASSIFIER_PATH"));
				if (!classifierOutput.isFile()) {
					classifierOutput.createNewFile();
				}

				FileOutputStream f_out = new FileOutputStream(
						ClassifierApplication.Settings
								.getProperty("CLASSIFIER_PATH"));
				ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
				obj_out.writeObject(trainingInfo);
				obj_out.close();
			} catch (FileNotFoundException fnfe) {
				System.err.println("Unable to find training data");
			} catch (IOException ioe) {
				System.err.println("Unable to read training data");
			}

			System.out.println(trainingInfo);
			System.out.println("Training all done!");
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	private static ClassifierTrainingInfo loadClassifier() {
		try {
			FileInputStream f_in = new FileInputStream(
					ClassifierApplication.Settings
							.getProperty("CLASSIFIER_PATH"));
			ObjectInputStream obj_in = new ObjectInputStream(f_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof ClassifierTrainingInfo) {
				return (ClassifierTrainingInfo) obj;
			}
		} catch (FileNotFoundException fnfe) {
			System.err.println("Unable to find trained data");
		} catch (IOException ioe) {
			System.err.println("Unable to read trained data");
		} catch (ClassNotFoundException cnfe) {
			System.err.println("Corrupted trained data");
		}

		return null;
	}

	private static void loadProperties() {
		String propertiesPath = new File(ClassifierApplication.class
				.getProtectionDomain().getCodeSource().getLocation().getPath())
				+ File.separator
				+ ".."
				+ File.separator
				+ "Application.properties";
		Settings = new Properties();
		try {
			Settings.load(new FileInputStream(propertiesPath));
			// for(String key : Settings.stringPropertyNames()) {
			// String value = Settings.getProperty(key);
			// System.out.println(key + " => " + value);
			// }
		} catch (IOException e) {
			System.err.println(String.format("Unable to load application settings from %s", propertiesPath));
		}
	}
}
