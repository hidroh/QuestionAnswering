package qa;

import java.util.ArrayList;
import java.util.List;

import qa.Settings;
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
import qa.model.AnswerInfo;
import qa.model.Document;
import qa.model.Passage;
import qa.model.QuestionInfo;
import qa.model.ResultInfo;
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

		answer(args);

	}

	private static void answer(String[] args) {
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

		// create document retriever, associate it with document indexer and
		// import data set
		DocumentRetrieverFactory drFactory = new DocumentRetrieverFactoryImpl();
		DocumentRetriever documentRetriever = drFactory
				.createDocumentRetriever();
		documentRetriever.setIndexer(documentIndexer);
		documentRetriever.importDocuments(Settings.get("DOCUMENT_PATH"));

		// create passage retriever
		PassageRetrieverFactory prFactory = new PassageRetrieverFactoryImpl();
		PassageRetriever passageRetriever = prFactory.createPassageRetriever();

		// create answer extractor
		AnswerExtractorFactory aeFactory = new AnswerExtractorFactoryImpl();
		AnswerExtractor answerExtractor = aeFactory.createAnswerExtractor();

		// get answers for each input question
		for (String question : args) {
			// parse question to get expanded query and query type
			QuestionInfo questionInfo = questionParser.parse(question);

			// use search engine to search for possible answers and rank
			// them
			searchEngine.search(questionInfo); // TODO: use searchEngine in the next line
			List<AnswerInfo> rankedAnswers = new ArrayList<AnswerInfo>();

			// initialize variables to store answers
			List<ResultInfo> results = new ArrayList<ResultInfo>();

			// for each answer returned from search engine, we try to
			// project
			// it to given data set by doing document retrieval using terms
			// from the answer
			for (AnswerInfo answerInfo : rankedAnswers) {
				// get set of relevant documents based on answer query
				List<Document> relevantDocs = documentRetriever
						.getDocuments(answerInfo.getAnswerTerms());

				// from this set of document, narrow down result set by
				// filter
				// only passages that possibly contain answer type
				List<Passage> relevantPassages = new ArrayList<Passage>();
				for (Document document : relevantDocs) {
					relevantPassages.addAll(passageRetriever.getPassages(
							document, answerInfo));
				}

				// extract ranked answers from relevant passages
				results.addAll(answerExtractor.extractAnswer(relevantPassages,
						questionInfo, answerInfo));
			}

			// print out results for this question
			printResults(question, results);
		}
	}

	private static void printUsage() {
		System.out
				.println("Usage: java Application <options> \"<question1>\" \"<question2>\"... ");
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
			System.out.printf("[%-5s] %s\n", resultInfo.getSupportingDocument()
					.getId(), resultInfo.getAnswer());
		}
	}
}
