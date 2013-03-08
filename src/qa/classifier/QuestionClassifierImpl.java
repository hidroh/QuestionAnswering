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

public class QuestionClassifierImpl implements QuestionClassifier {
	private boolean suppressLog = false;

	public QuestionClassifierImpl(boolean suppressLog) {
		this.suppressLog = suppressLog;
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
			System.out.println(String.format("c = %s", c));
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
	public QueryType apply(List<QueryType> queryTypes,
			ClassifierInfo trainingInfo, String question) {
		List<String> terms = extractQueryTerms(trainingInfo.getVocabulary(),
				question);
		Map<QueryType, Double> score = new HashMap<QueryType, Double>();
		for (QueryType c : queryTypes) {
			score.put(c, Math.log(trainingInfo.getPrior().get(c)));
			for (String t : terms) {
				if (trainingInfo.getConditionalProbability().containsKey(t)) {
					score.put(
							c,
							score.get(c)
									+ Math.log(trainingInfo
											.getConditionalProbability().get(t)
											.get(c)));

				}
			}
		}

		return getArgMax(score);
	}

	public List<String> extractQueryTerms(Set<String> vocabulary,
			String question) {
		ClassifierHelper helper = ClassifierHelper.getInstance();
		List<QueryTerm> terms = helper.getQueryTerms(question);
		List<String> extracted = new ArrayList<String>();
		if (!suppressLog)
			System.out.print("{ ");
		for (QueryTerm queryTerm : terms) {
			String term = queryTerm.getText();
			if (!suppressLog)
				System.out.print(term + ", ");
			if (vocabulary.contains(term)) {
				extracted.add(term);
			}
		}
		if (!suppressLog)
			System.out.println(" }");

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

	private boolean isStopWord(String term) {
		// TODO Auto-generated method stub
		return false;
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

	private QueryType getArgMax(Map<QueryType, Double> score) {
		List<Map.Entry<QueryType, Double>> scoreList = new ArrayList<Map.Entry<QueryType, Double>>(
				score.entrySet());
		Collections.sort(scoreList,
				new Comparator<Map.Entry<QueryType, Double>>() {
					public int compare(Map.Entry<QueryType, Double> o1,
							Map.Entry<QueryType, Double> o2) {
						return ((Comparable<Double>) o2.getValue())
								.compareTo(o1.getValue());
					}
				});

		if (!suppressLog) {
			for (Map.Entry<QueryType, Double> e : scoreList) {
				System.out.printf("  %-5s => %.2f\n", e.getKey(), e.getValue());
			}
		}

		return scoreList.get(0).getKey();
	}

	private List<QueryType> getArgsMax(Map<QueryType, Double> score, double t,
			int k) {
		List<Map.Entry<QueryType, Double>> scoreList = new ArrayList<Map.Entry<QueryType, Double>>(
				score.entrySet());
		Collections.sort(scoreList,
				new Comparator<Map.Entry<QueryType, Double>>() {
					public int compare(Map.Entry<QueryType, Double> o1,
							Map.Entry<QueryType, Double> o2) {
						return ((Comparable<Double>) o2.getValue())
								.compareTo(o1.getValue());
					}
				});

		List<QueryType> results = new ArrayList<QueryType>();
		double threshold = scoreList.get(0).getValue() / t;
		for (int i = 0; i < scoreList.size() && i < k
				&& scoreList.get(i).getValue() > threshold; i++) {
			results.add(scoreList.get(i).getKey());
			System.out.println(scoreList.get(i).getKey());
		}

		return results;
	}
}
