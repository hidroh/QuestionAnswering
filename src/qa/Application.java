package qa;

import java.util.ArrayList;
import java.util.List;

import qa.Settings;
import qa.helper.ApplicationHelper;
import qa.extractor.AnswerExtractor;
import qa.factory.AnswerExtractorFactory;
import qa.factory.AnswerExtractorFactoryImpl;
import qa.factory.DocumentIndexerFactory;
import qa.factory.DocumentIndexerFactoryImpl;
import qa.factory.DocumentRetrieverFactory;
import qa.factory.DocumentRetrieverFactoryImpl;
import qa.factory.PassageRetrieverFactory;
import qa.factory.PassageRetrieverFactoryImpl;
import qa.factory.QuestionParserFactory;
import qa.factory.QuestionParserFactoryImpl;
import qa.factory.SearchEngineFactory;
import qa.factory.SearchEngineFactoryImpl;
import qa.indexer.DocumentIndexer;
import qa.model.Document;
import qa.model.QuestionInfo;
import qa.model.ResultInfo;
import qa.model.Passage;
import qa.parser.QuestionParser;
import qa.search.DocumentRetriever;
import qa.search.PassageRetriever;
import qa.search.SearchEngine;

public class Application {
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

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

		answer(args, null, false);

	}

	public static void answer(String[] args, List<String> sampleAnswers, boolean color) {
		// use factory pattern to create components so that we can easily
		// swap their underlying implementations later without changing
		// this code

		// create question parser
		QuestionParserFactory qpFactory = new QuestionParserFactoryImpl();
		QuestionParser questionParser = qpFactory.createQuestionParser();

		// create search engine
		SearchEngineFactory seFactory = new SearchEngineFactoryImpl();
		SearchEngine searchEngine = seFactory.createSearchEngine();

		// create document indexer
		DocumentIndexerFactory diFactory = new DocumentIndexerFactoryImpl();
		DocumentIndexer documentIndexer = diFactory.createDocumentIndexer();

		// create document retriever
		DocumentRetrieverFactory drFactory = new DocumentRetrieverFactoryImpl();
		DocumentRetriever documentRetriever = drFactory
				.createDocumentRetriever();

		// index documents if they have not been indexed
		if (!documentIndexer.hasIndexData(Settings.get("INDEX_PATH"))) {
			ApplicationHelper.printWarning(" No indexed data found, please check configuration or run IndexerApplication.");
		}

		// init passage retriever factory
		PassageRetrieverFactory prFactory = new PassageRetrieverFactoryImpl();

		// create answer extractor
		AnswerExtractorFactory aeFactory = new AnswerExtractorFactoryImpl();
		AnswerExtractor answerExtractor = aeFactory.createAnswerExtractor();

		// get answers for each input question
		for (int i = 0; i < args.length; i++) {
			String question = args[i];
			String sampleAnswer = null;
			if (sampleAnswers != null) {
				sampleAnswer = sampleAnswers.get(i);
			}

			// parse question to get expanded query and query type
			QuestionInfo questionInfo = questionParser.parse(question);

			// use search engine to reformulate original query
			String irQuery = ApplicationHelper.stripPunctuation(questionInfo.getRaw());
			if (ApplicationHelper.QUERY_REFORMULATION) {
				irQuery = searchEngine.search(questionInfo);
			}

			// get set of relevant documents based on reformulated query
			List<Document> relevantDocs = new ArrayList<Document>();
			if (documentIndexer.hasIndexData(Settings.get("INDEX_PATH"))) {
				relevantDocs = documentRetriever.getDocuments(irQuery);
			}

			// from this set of document, narrow down result set by
			// filtering
			// only passages that possibly contain answer type
			List<Passage> relevantPassages = new ArrayList<Passage>();
			for (Document document : relevantDocs) {
				// create passage retriever
				PassageRetriever passageRetriever = prFactory.createPassageRetriever(document);

				relevantPassages.addAll(passageRetriever.getPassages(irQuery));
			}

			// extract ranked answers from relevant passages
			List<ResultInfo> results = answerExtractor.extractAnswer(relevantPassages,
					questionInfo, irQuery);

			// print out results for this question
			printResults(questionInfo, sampleAnswer, results, color);
		}
	}

	private static void printUsage() {
		System.out
				.println("Usage: java qa.Application <options> \"<question1>\" \"<question2>\"... ");
		System.out.println();
		System.out
				.println("Run configuration stored in 'Application.properties'");
		System.out
				.println("Example: java Application \"Where is Milan ?\" \"Who developed the Macintosh computer ?\" ");
	}

	private static void printResults(QuestionInfo questionInfo, String sampleAnswer, List<ResultInfo> results, boolean color) {
		if (color) {
			System.out.printf("\nQ: "+ANSI_CYAN+"\"%s\""+ANSI_RESET+" [%s]\n", questionInfo.getRaw(), questionInfo.getMultiClassification());
		} else {
			System.out.printf("\nQ: \"%s\" [%s]\n", questionInfo.getRaw(), questionInfo.getMultiClassification());
		}

		if (sampleAnswer != null) {
			if (color) {
				System.out.printf("A(s): " + ANSI_GREEN + "%s" + ANSI_RESET + "\n", sampleAnswer);	
			} else {
				System.out.printf("A(s): %s\n", sampleAnswer);
			}
			
		}

		System.out.print("R(s):");
		for (ResultInfo resultInfo : results) {
			if (color && !resultInfo.getSupportingDocumentId().equals("alt")) {
				System.out.printf(ANSI_RED + "%s"+ANSI_RESET+" [%s]; ", resultInfo.getAnswer(), resultInfo.getSupportingDocumentId());				
			} else {
				System.out.printf("%s [%s]; ", resultInfo.getAnswer(), resultInfo.getSupportingDocumentId());			
			}
		}
		System.out.println();
		System.out.println("-----------");
	}
}
