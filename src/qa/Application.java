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

		answer(args);

	}

	public static void answer(String[] args) {
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
			// documentIndexer.indexDocuments(Settings.get("DOCUMENT_PATH"));
		}

		// init passage retriever factory
		PassageRetrieverFactory prFactory = new PassageRetrieverFactoryImpl();

		// create answer extractor
		AnswerExtractorFactory aeFactory = new AnswerExtractorFactoryImpl();
		AnswerExtractor answerExtractor = aeFactory.createAnswerExtractor();

		// get answers for each input question
		for (String question : args) {
			// parse question to get expanded query and query type
			QuestionInfo questionInfo = questionParser.parse(question);

			// use search engine to reformulate original query
			String irQuery = searchEngine.search(questionInfo);

			// get set of relevant documents based on reformulated query
			List<Document> relevantDocs = new ArrayList<Document>();
			if (documentIndexer.hasIndexData(Settings.get("INDEX_PATH"))) {
				relevantDocs = documentRetriever.getDocuments(irQuery);
			}

			// initialize variables to store answers
			List<ResultInfo> results = new ArrayList<ResultInfo>();

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
			results.addAll(answerExtractor.extractAnswer(relevantPassages,
					questionInfo, irQuery));

			// print out results for this question
			printResults(question, results);
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

	private static void printResults(String question, List<ResultInfo> results) {
		System.out.printf("\nQ: \"%s\"\n", question);
		System.out.println("A(s):");
		for (ResultInfo resultInfo : results) {
			System.out.printf("[%s]\n%s\n\n", resultInfo.getSupportingDocumentId(), resultInfo.getAnswer());
		}
	}
}
