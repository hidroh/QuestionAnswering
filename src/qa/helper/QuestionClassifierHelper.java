package qa.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qa.model.QueryTerm;
import qa.model.QueryTermImpl;
import qa.model.QuestionInfo;
import qa.model.QuestionInfoImpl;
import qa.model.enumerator.QueryType;

public class QuestionClassifierHelper {
	private static QuestionClassifierHelper instance;
	private final String re1 = "((?:[a-z][a-z]+))"; // Word 1
	private final String re2 = ":"; // Non-greedy match on filler
	private final String re3 = "((?:[a-z][a-z]+))"; // Word 2
	private final String re4 = " "; // Non-greedy match on filler
	private final String re5 = "((?:.*))"; // Variable Name 1

	private final Pattern p = Pattern.compile(re1 + re2 + re3 + re4 + re5,
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private QuestionClassifierHelper() {

	}

	public static QuestionClassifierHelper getInstance() {
		if (instance == null) {
			instance = new QuestionClassifierHelper();
		}

		return instance;
	}

	public List<QuestionInfo> getTrainingData(String corpusPath) {
		List<QuestionInfo> trainingData = new ArrayList<QuestionInfo>();
		File folder = new File(corpusPath);
		File[] fileList = folder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".label");
			}
		});
		for (File file : fileList) {
			Scanner scanner;
			try {
				scanner = new Scanner(file);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					trainingData.add(getQuestionInfo(line));
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		return trainingData;
	}

	private QuestionInfo getQuestionInfo(String text) {
		Matcher m = p.matcher(text);
		if (m.find()) {
			String queryType = m.group(1);
//			String subQueryType = m.group(2);
			String rawQuestion = m.group(3);
			List<QueryTerm> terms = getQueryTerms(rawQuestion);
			QuestionInfo questionInfo = new QuestionInfoImpl(QueryType.valueOf(queryType), terms);
			return questionInfo;
		} else {
			return null;
		}
	}

	private List<QueryTerm> getQueryTerms(String text) {
		List<QueryTerm> terms = new ArrayList<QueryTerm>();
		Pattern wordPattern = Pattern.compile("\\w+",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m = wordPattern.matcher(text);
		while (m.find()) {
			terms.add(new QueryTermImpl(m.group()));
		}
		
		return terms;
	}

	public List<QueryType> getAllQueryTypes() {
		return Arrays.asList(QueryType.class.getEnumConstants());
	}
}
