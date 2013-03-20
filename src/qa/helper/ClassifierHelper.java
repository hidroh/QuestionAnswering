package qa.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qa.model.QueryTerm;
import qa.model.QueryTermImpl;
import qa.model.QuestionInfo;
import qa.model.QuestionInfoImpl;
import qa.model.enumerator.QueryType;
import qa.model.enumerator.QuerySubType;

public class ClassifierHelper {

	private static ClassifierHelper instance;
	private final String re1 = "((?:[a-z][a-z]+))"; // Word 1
	private final String re2 = ":"; // Non-greedy match on filler
	private final String re3 = "((?:[a-z][a-z]+))"; // Word 2
	private final String re4 = " "; // Non-greedy match on filler
	private final String re5 = "((?:.*))"; // Variable Name 1

	private final Pattern p = Pattern.compile(re1 + re2 + re3 + re4 + re5,
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private ClassifierHelper() {

	}

	public static ClassifierHelper getInstance() {
		if (instance == null) {
			instance = new ClassifierHelper();
		}

		return instance;
	}

	public List<QuestionInfo> getAnnotatedData(String corpusPath,
			final String prefix, final String ext) throws Exception {
		if (corpusPath == null) {
			throw new Exception("Corpus path is missing");
		}
		List<QuestionInfo> trainingData = new ArrayList<QuestionInfo>();
		File folder = new File(corpusPath);
		File[] fileList = folder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(ext.toLowerCase())
						&& name.toLowerCase().startsWith(prefix.toLowerCase());
			}
		});
		for (File file : fileList) {
			int questionCount = 0;
			BufferedReader br = null;
			try {
				String line;
				br = new BufferedReader(new FileReader(file));
				while ((line = br.readLine()) != null) {
					questionCount++;
					trainingData.add(getQuestionInfo(line));
				}

				System.out.printf("Data set: %s [ %d questions]\n",
						file.getName(), questionCount);
			} catch (FileNotFoundException e) {
				System.err.println(String.format("Corpus not found: %s/%s*%s",
						corpusPath, prefix, ext));
			} catch (IOException e) {
				System.err.println(String.format(
						"Unable to read corpus: %s/%s*%s", corpusPath, prefix,
						ext));
			}

		}

		return trainingData;
	}

	private QuestionInfo getQuestionInfo(String text) {
		Matcher m = p.matcher(text);
		if (m.find()) {
			String queryType = m.group(1);
			String querySubType = m.group(2);
			String rawQuestion = m.group(3);
			List<QueryTerm> terms = getQueryTerms(PosTagger.getInstance().tag(
					rawQuestion));
			QuestionInfo questionInfo = new QuestionInfoImpl(
					QueryType.valueOf(queryType), QuerySubType.valueOf(String
							.format("%s_%s", queryType, querySubType)), terms,
					rawQuestion);
			// System.out.println(questionInfo);
			return questionInfo;
		} else {
			return null;
		}
	}

	public List<QueryTerm> getQueryTerms(String text) {
		List<QueryTerm> terms = new ArrayList<QueryTerm>();
		Pattern wordPattern = Pattern.compile("\\w+_\\w+",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m = wordPattern.matcher(text);
		while (m.find()) {
			String tagged = m.group();

			Pattern taggedPattern = Pattern.compile("((?:\\w+))_((?:\\w+))",
					Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher taggedMatcher = taggedPattern.matcher(tagged);
			if (taggedMatcher.find()) {
				String word = taggedMatcher.group(1);
				String pos = taggedMatcher.group(2);
//				if (pos.equals("CD") || pos.equals("NNP") || pos.equals("NNPS")
//						|| pos.equals("FW")) {
//					// System.out.print(pos);
					terms.add(new QueryTermImpl(pos));
//				} else {
					// System.out.print(word);
				if (!(pos.equals("CD") || pos.equals("NNP") || pos.equals("NNPS"))) {
					terms.add(new QueryTermImpl(word));
				}
			}

			// System.out.print(" ");
		}
		// System.out.println();

		return terms;
	}

	public List<QueryType> getAllQueryTypes() {
		return Arrays.asList(QueryType.class.getEnumConstants());
	}

	public List<QuerySubType> getAllQuerySubTypes() {
		return Arrays.asList(QuerySubType.class.getEnumConstants());
	}

	public List<String> getStopWords(String path) {
		List<String> stopWords = new ArrayList<String>();

		Pattern wordPattern = Pattern.compile("\\w+", Pattern.CASE_INSENSITIVE
				| Pattern.DOTALL);
		String line;
		try {
			File file = new File(path);
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null) {
				Matcher m = wordPattern.matcher(line);
				while (m.find()) {
					stopWords.add(m.group());
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("[WARNING] Unable to find stopword list");
		} catch (IOException e) {
			System.out.println("[WARNING] Unable to read stopword list");
		}

		return stopWords;
	}
}
