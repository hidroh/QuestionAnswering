package qa.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import qa.model.ClassifierInfo;
import qa.model.ClassifierInfoImpl;
import qa.model.QueryTerm;
import qa.model.QuestionInfo;
import qa.model.enumerator.QueryType;
import qa.model.enumerator.QuerySubType;
import qa.helper.ClassifierHelper;
import qa.helper.PosTagger;
import qa.helper.ApplicationHelper;

public class QuestionClassifierImpl implements QuestionClassifier {
	private List<String> stopWords;
	private double threshold = 1.0;
	private int resultLimit = 1;

	public QuestionClassifierImpl() {
	}

	@Override
	public ClassifierInfo train(List<QueryType> queryTypes,
			List<QuerySubType> querySubTypes, List<QuestionInfo> questions) {
		Set<String> v = new HashSet<String>();
		v.addAll(extractVocabulary(questions));
		System.out.printf("Training questions = %d, vocabulary count = %d\n",
				questions.size(), v.size());
		int n = questions.size();

		List<String> strQueryTypes = new ArrayList<String>();
		for (QueryType t : queryTypes) {
			strQueryTypes.add(t.toString());
		}

		Map<String, Double> prior = new HashMap<String, Double>();
		Map<String, Map<String, Double>> condProb = new HashMap<String, Map<String, Double>>();
		boolean IS_SUB_TYPE = false;
		calculatePriorAndCondProb(strQueryTypes, questions, v, prior, condProb,
				n, IS_SUB_TYPE);

		List<String> strQuerySubTypes = new ArrayList<String>();
		for (QuerySubType t : querySubTypes) {
			strQuerySubTypes.add(t.toString());
		}

		Map<String, Double> subPrior = new HashMap<String, Double>();
		Map<String, Map<String, Double>> subCondProb = new HashMap<String, Map<String, Double>>();
		IS_SUB_TYPE = true;
		calculatePriorAndCondProb(strQuerySubTypes, questions, v, subPrior,
				subCondProb, n, IS_SUB_TYPE);
		ClassifierInfo trainingInfo = new ClassifierInfoImpl(v, prior,
				condProb, subPrior, subCondProb);
		return trainingInfo;
	}

	private void calculatePriorAndCondProb(List<String> queryTypes,
			List<QuestionInfo> questions, Set<String> v,
			Map<String, Double> prior,
			Map<String, Map<String, Double>> condProb, int n, boolean isSubType) {
		double test_sum_prior = 0;
		for (String c : queryTypes) {
			ApplicationHelper.printDebug(String.format("c = %s\n", c));
			int sum_t_ct = 0;
			int n_c = countQuestionsInClass(questions, c, isSubType);
			prior.put(c, (double) n_c / n);
			test_sum_prior += (double) n_c / n;
			double test_sum_condprob = 0;
			List<String> text_c = concatQuestionsInclass(questions, c,
					isSubType);
			for (String t : v) {
				Map<String, Double> classTermCount;
				if (condProb.get(t) != null) {
					classTermCount = condProb.get(t);
				} else {
					classTermCount = new HashMap<String, Double>();
				}
				int t_ct = countTerm(text_c, t);
				sum_t_ct += t_ct;
				classTermCount.put(c, (double) t_ct);
				condProb.put(t, classTermCount);
			}

			for (String t : v) {
				Map<String, Double> classTermCount = condProb.get(t);
				condProb.get(t).put(c,
						(classTermCount.get(c) + 1) / (sum_t_ct + v.size()));
				// System.out.println(String.format("condprob[%s,%s] = %f", t,
				// c.toString(), condProb.get(t).get(c)));
				test_sum_condprob += condProb.get(t).get(c);
			}

			assert Math.abs(test_sum_condprob - 1) < 0.00001 : String
					.format("Conditional probabilities given class = '%s' do not sum up to 1: %.2f",
							c, test_sum_condprob);
		}

		assert Math.abs(test_sum_prior - 1) < 0.00001 : String.format(
				"Priors do not sum up to 1: %.2f", test_sum_prior);
	}

	@Override
	public List<String> apply(List<QueryType> queryTypes,
			List<QuerySubType> querySubTypes, ClassifierInfo trainingInfo,
			String question) {
		List<String> terms = extractQueryTerms(trainingInfo.getVocabulary(),
				question);
		List<String> strQueryTypes = new ArrayList<String>();
		for (QueryType t : queryTypes) {
			strQueryTypes.add(t.toString());
		}

		boolean IS_SUB_TYPE = false;
		// String queryTypeString = getClassification(strQueryTypes,
		// trainingInfo,
		// terms, IS_SUB_TYPE);
		List<String> classifiedTypes = getMultiClassification(strQueryTypes,
				trainingInfo, terms, IS_SUB_TYPE);

		// List<String> strQuerySubTypes = new ArrayList<String>();
		// for (QuerySubType t : querySubTypes) {
		// strQuerySubTypes.add(t.toString());
		// }

		List<String> strQuerySubTypes = new ArrayList<String>();
		for (QuerySubType t : querySubTypes) {
			for (String parentType : classifiedTypes) {
				if (t.toString().startsWith(parentType)) {
					strQuerySubTypes.add(t.toString());
					break;
				}
			}
		}

		IS_SUB_TYPE = true;
		// String querySubTypeString = getClassification(strQuerySubTypes,
		// trainingInfo, terms, IS_SUB_TYPE);
		// return new String[] { queryTypeString, querySubTypeString };
		List<String> classifiedSubTypes = getMultiClassification(
				strQuerySubTypes, trainingInfo, terms, IS_SUB_TYPE);

		List<String> results = new ArrayList<String>();
		results.add(classifiedTypes.get(0));
		results.addAll(classifiedSubTypes);
		return results;
	}

	private List<String> getMultiClassification(List<String> queryTypes,
			ClassifierInfo trainingInfo, List<String> terms, boolean isSubType) {
		Map<String, Double> score = calculateScore(queryTypes, trainingInfo,
				terms, isSubType);

		return getArgsMax(score);
	}

	private Map<String, Double> calculateScore(List<String> queryTypes,
			ClassifierInfo trainingInfo, List<String> terms, boolean isSubType) {
		Map<String, Double> score = new HashMap<String, Double>();
		for (String c : queryTypes) {
			Map<String, Double> prior = isSubType ? trainingInfo.getSubPrior()
					: trainingInfo.getPrior();
			score.put(c, Math.log(prior.get(c)));
			for (String t : terms) {
				Map<String, Map<String, Double>> condProb = isSubType ? trainingInfo
						.getSubConditionalProbability() : trainingInfo
						.getConditionalProbability();
				if (condProb.containsKey(t)) {
					score.put(c,
							score.get(c) + Math.log(condProb.get(t).get(c)));

				}
			}
		}
		return score;
	}

	private List<String> extractQueryTerms(Set<String> vocabulary,
			String question) {
		ClassifierHelper helper = ClassifierHelper.getInstance();
		List<QueryTerm> terms = new ArrayList<QueryTerm>();
		terms.addAll(helper.getChunks(question));
		terms.addAll(helper.getNameEntities(question));
		terms.addAll(helper.getQueryTerms(PosTagger.getInstance().tag(question)));
		List<String> extracted = new ArrayList<String>();
		ApplicationHelper.printDebug("{ ");
		for (QueryTerm queryTerm : terms) {
			String term = queryTerm.getText();
			if (isStopWord(term)) {
				continue;
			}

			ApplicationHelper.printDebug(term + ", ");
			if (vocabulary.contains(term)) {
				extracted.add(term);
			}
		}
		ApplicationHelper.printDebug(" }");

		return extracted;
	}

	private Set<String> extractVocabulary(List<QuestionInfo> questions) {
		Set<String> terms = new HashSet<String>();
		for (QuestionInfo question : questions) {
			List<QueryTerm> questionTerms = question.getQuestionTerms();
			for (QueryTerm questionTerm : questionTerms) {
				String term = questionTerm.getText();
				if (!isStopWord(term)) {
					terms.add(term);
				}
			}
		}

		return terms;
	}

	@Override
	public void setStopWords(List<String> stopWords) {
		this.stopWords = stopWords;
	}

	@Override
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public void setResultLimit(int resultLimit) {
		this.resultLimit = resultLimit;
	}

	private boolean isStopWord(String term) {
		if (stopWords == null) {
			return false;
		}

		return stopWords.contains(term);
	}

	private int countQuestionsInClass(List<QuestionInfo> questions,
			String queryType, boolean isSubType) {
		int count = 0;
		for (QuestionInfo question : questions) {
			if (isSubType) {
				if (question.getQuerySubType().toString().equals(queryType)) {
					count++;
				}
			} else {
				if (question.getQueryType().toString().equals(queryType)) {
					count++;
				}
			}
		}

		return count;
	}

	private List<String> concatQuestionsInclass(List<QuestionInfo> questions,
			String queryType, boolean isSubType) {
		List<String> terms = new ArrayList<String>();
		for (QuestionInfo question : questions) {
			boolean concat = false;
			if (isSubType) {
				if (question.getQuerySubType().toString().equals(queryType)) {
					concat = true;
				}
			} else {
				if (question.getQueryType().toString().equals(queryType)) {
					concat = true;
				}
			}

			if (concat) {
				for (QueryTerm term : question.getQuestionTerms()) {
					terms.add(term.getText());
				}
			}
		}

		return terms;
	}

	private int countTerm(List<String> termList, String term) {
		int count = 0;
		for (String t : termList) {
			if (t.equals(term)) {
				count++;
			}
		}

		return count;
	}

	private List<String> getArgsMax(Map<String, Double> score) {
		List<Map.Entry<String, Double>> scoreList = new ArrayList<Map.Entry<String, Double>>(
				score.entrySet());
		Collections.sort(scoreList,
				new Comparator<Map.Entry<String, Double>>() {
					public int compare(Map.Entry<String, Double> o1,
							Map.Entry<String, Double> o2) {
						return ((Comparable<Double>) o2.getValue())
								.compareTo(o1.getValue());
					}
				});

		List<String> results = new ArrayList<String>();
		double scoreThreshold = scoreList.get(0).getValue() / threshold;
		ApplicationHelper.printDebug("Possible classes: ");	
		for (int i = 0; i < scoreList.size() && i < resultLimit
				&& scoreList.get(i).getValue() >= scoreThreshold; i++) {
			results.add(scoreList.get(i).getKey());
			ApplicationHelper.printDebug(scoreList.get(i).getKey() + ", ");
		}
		ApplicationHelper.printDebug("\n");

		return results;
	}
}
