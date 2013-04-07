package qa;

import java.io.File;
import java.lang.ClassNotFoundException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import qa.helper.ApplicationHelper;
import qa.model.enumerator.QueryType;
import qa.model.enumerator.QuerySubType;
import qa.model.QuestionInfoImpl;
import qa.Settings;
import qa.classifier.QuestionClassifier;
import qa.classifier.QuestionClassifierImpl;
import qa.helper.ClassifierHelper;
import qa.model.ClassifierInfo;
import qa.model.QuestionInfo;

public class ClassifierApplication {
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	private static ClassifierInfo trainingInfo;
	private static QuestionClassifier classifier;

	/**
	 * @param args
	 *            array of input questions
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			printUsage();
			return;
		}

		if (!ApplicationHelper.SHOW_ERROR) {
			System.err.close();
		}

		if (args[0].equals("train")) {
			train();
		} else if (args[0].equals("eval")) {
			evaluate(Arrays.asList(args));
		} else {
			classify(args);
		}

	}

	private static void evaluate(List<String> options) {
		boolean debug = options.size() >= 2 && options.contains("-debug");
		boolean color = options.size() >= 2 && options.contains("-color");
		ClassifierHelper helper = ClassifierHelper.getInstance();
		List<QuestionInfo> testData;
		try {
			testData = helper.getAnnotatedData(
					Settings.get("TEST_CORPUS_PATH"),
					Settings.get("TEST_CORPUS_PREFIX"),
					Settings.get("TEST_CORPUS_EXT"), Settings.get("CHUNK_EXT"));
			QuestionClassifier qc = new QuestionClassifierImpl();
			qc.setStopWords(helper.getStopWords(Settings
					.get("STOPWORD_LIST_PATH")));
			qc.setThreshold(Double.parseDouble(Settings
					.get("CLASSIFIER_THRESHOLD")));
			qc.setResultLimit(Integer.parseInt(Settings.get("CLASSIFIER_LIMIT")));

			ClassifierInfo trainingInfo = loadClassifierInfo();
			if (trainingInfo != null) {
				int correct = 0;
				int subCorrect = 0;
				for (QuestionInfo question : testData) {
					// System.out.printf("\nQ: \"%s\"\n", question.getRaw());
					// System.out.printf(
					// "Classified as: %s\n",
					// qc.apply(helper.getAllQueryTypes(), trainingInfo,
					// question.getRaw()));
					List<String> classified = qc.apply(
							helper.getAllQueryTypes(),
							helper.getAllQuerySubTypes(), trainingInfo,
							question.getRaw());
					String expected = question.getQueryType().toString();
					String subExpected = question.getQuerySubType().toString();
					if (classified.get(0).equals(expected)) {
						correct++;
					}

					String subClassified = "";
					boolean isSubCorrect = false;
					for (int i = 1; i < classified.size(); i++) {
						subClassified += String.format("%-15s",
								classified.get(i));
						if (classified.get(i).equals(subExpected)) {
							isSubCorrect = true;
							break;
						}
						if (i == 1) {
							break;
						}
					}

					if (isSubCorrect) {
						subCorrect++;
					} else if (debug) {
						String format = "";
						if (color) {
							format = ANSI_GREEN
									+ "-- %-20s "
									+ ANSI_RED
									+ "++ [%-"
									+ (15 * Integer.parseInt(Settings
											.get("CLASSIFIER_LIMIT"))) + "s]"
									+ ANSI_RESET + " %s\n";
						} else {
							format = "-- %-20s ++ [%-"
									+ (15 * Integer.parseInt(Settings
											.get("CLASSIFIER_LIMIT")))
									+ "s] %s\n";
						}
						System.out.print(
							String.format(format, subExpected, subClassified,
								question.getRaw()));
					}
				}

				System.out
						.printf("Evaluation result: [TYPE] %d / %d = %.2f, [SUB_TYPE]  %d / %d = %.2f",
								correct, testData.size(), (double) correct
										/ testData.size(), subCorrect,
								testData.size(),
								(double) subCorrect / testData.size());
			} else {
				ApplicationHelper.printError("Operation halted, unable to retrieve trained data");
			}
		} catch (Exception e) {
			ApplicationHelper.printError("Question Classifier: Unable to evaluate", e);
		}

	}

	private static void classify(String[] args) {
		if (args.length == 0) {
			return;
		}

		if (trainingInfo == null) {
			trainingInfo = loadClassifierInfo();
		}

		if (classifier == null) {
			setClassifier();
		}

		if (trainingInfo != null && classifier != null) {
			for (int i = 0; i < args.length; i++) {
				String question = args[i];
				classifyQuestion(question);
			}
		} else {
			System.err
					.println("Operation halted, unable to retrieve trained data");
		}
	}

	private static QuestionInfo classifyQuestion(String question) {
		ClassifierHelper helper = ClassifierHelper.getInstance();
		ApplicationHelper.printDebug(String.format("\nQ: \"%s\"\n", question));
		List<String> classified = classifier.apply(helper.getAllQueryTypes(),
				helper.getAllQuerySubTypes(), trainingInfo, question);
		String subClassified = "";
		for (int j = 1; j < classified.size(); j++) {
			subClassified += String.format("%-15s", classified.get(j));
		}
		ApplicationHelper.printDebug(String.format("Classified as: %s:[%s]\n", classified.get(0),
				subClassified));
		return new QuestionInfoImpl(QueryType.valueOf(classified.get(0)),
				QuerySubType.valueOf(subClassified.trim()),
				helper.getSearchEngineQueryTerms(question), question);
	}

	private static void setClassifier() {
		classifier = new QuestionClassifierImpl();
		ClassifierHelper helper = ClassifierHelper.getInstance();
		classifier.setStopWords(helper.getStopWords(Settings
				.get("STOPWORD_LIST_PATH")));
		classifier.setThreshold(Double.parseDouble(Settings
				.get("CLASSIFIER_THRESHOLD")));
		classifier.setResultLimit(Integer.parseInt(Settings
				.get("CLASSIFIER_LIMIT")));
	}

	public static QuestionInfo classify(String question) {
		if (trainingInfo == null) {
			trainingInfo = loadClassifierInfo();
		}

		if (classifier == null) {
			setClassifier();
		}

		if (trainingInfo != null && classifier != null) {
			return classifyQuestion(question);
		} else {
			System.err
					.println("Operation halted, unable to retrieve trained data");
			return null;
		}
	}

	private static void printUsage() {
		System.out
				.println("Usage: java qa.ClassifierApplication <command> <options> \"<question1>\" \"<question2>\"... ");
		System.out.println("Available commands:");
		System.out.printf("  %-15s %s\n", "train",
				"Only train question classifier, no input questions required");
		System.out
				.printf("  %-15s %s\n", "eval",
						"Only evaluate question classifier, no input questions required");
		System.out.println("  Eval options:");
		System.out.printf("  %-15s %s\n", "-debug",
				"Print out debug information");
		System.out.printf("  %-15s %s\n", "-color", "Colorize output");
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
					.getAnnotatedData(Settings.get("TRAIN_CORPUS_PATH"),
							Settings.get("TRAIN_CORPUS_PREFIX"),
							Settings.get("TRAIN_CORPUS_EXT"),
							Settings.get("CHUNK_EXT"));
			QuestionClassifier qc = new QuestionClassifierImpl();
			// qc.setStopWords(helper.getStopWords(ClassifierApplication.Settings
			// .getProperty("STOPWORD_LIST_PATH")));
			ClassifierInfo trainingInfo = qc.train(helper.getAllQueryTypes(),
					helper.getAllQuerySubTypes(), trainingData);

			try {
				File classifierOutput = new File(
						Settings.get("CLASSIFIER_PATH"));
				if (!classifierOutput.isFile()) {
					classifierOutput.createNewFile();
				}

				FileOutputStream f_out = new FileOutputStream(
						Settings.get("CLASSIFIER_PATH"));
				ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
				obj_out.writeObject(trainingInfo);
				obj_out.close();
			} catch (FileNotFoundException fnfe) {
				ApplicationHelper.printError("Question Classifier: Unable to find training data", fnfe);
			} catch (IOException ioe) {
				ApplicationHelper.printError("Question Classifier: Unable to read training data", ioe);
			}

			System.out.println(trainingInfo);
			System.out.println("Training all done!");
		} catch (Exception e) {
			ApplicationHelper.printError("Question Classifier: Unable to train classifier", e);
		}
	}

	private static ClassifierInfo loadClassifierInfo() {
		try {
			FileInputStream f_in = new FileInputStream(
					Settings.get("CLASSIFIER_PATH"));
			ObjectInputStream obj_in = new ObjectInputStream(f_in);
			Object obj = obj_in.readObject();
			obj_in.close();
			if (obj instanceof ClassifierInfo) {
				return (ClassifierInfo) obj;
			}
		} catch (FileNotFoundException fnfe) {
			ApplicationHelper.printError("QuestionClassifier: Unable to find trained data", fnfe);
		} catch (IOException ioe) {
			ApplicationHelper.printError("Question Classifier: Unable to read trained data", ioe);
		} catch (ClassNotFoundException cnfe) {
			ApplicationHelper.printError("Question Classifier: Corrupted trained data", cnfe);
		}

		return null;
	}
}
