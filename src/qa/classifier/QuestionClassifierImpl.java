package qa.classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import qa.model.ClassifierTrainingInfo;
import qa.model.ClassifierTrainingInfoImpl;
import qa.model.QueryTerm;
import qa.model.QuestionInfo;
import qa.model.enumerator.QueryType;
import qa.helper.ClassifierHelper;

public class QuestionClassifierImpl implements QuestionClassifier {
	private boolean suppressLog = false;

	public QuestionClassifierImpl(boolean suppressLog) {
		this.suppressLog = suppressLog;
	}

	@Override
	public ClassifierTrainingInfo train(List<QueryType> queryTypes,
			List<QuestionInfo> questions) {
		Set<String> v = new HashSet<String>();
		Map<QueryType, Double> prior = new HashMap<QueryType, Double>();
		Map<String, Map<QueryType, Double>> condProb = new HashMap<String, Map<QueryType, Double>>();
		v.addAll(extractVocabulary(questions));
		System.out.printf("Training questions = %d, vocabulary count = %d\n",
				questions.size(), v.size());
		int n = questions.size();

		double test_sum_prior = 0;
		for (QueryType c : queryTypes) {
			System.out.println(String.format("c = %s", c.toString()));
			int sum_t_ct = 0;
			int n_c = countQuestionsInClass(questions, c);
			prior.put(c, (double) n_c / n);
			test_sum_prior += (double) n_c / n;
			double test_sum_condprob = 0;
			List<String> text_c = concatQuestionsInclass(questions, c);
			for (String t : v) {
				Map<QueryType, Double> classTermCount;
				if (condProb.get(t) != null) {
					classTermCount = condProb.get(t);
				} else {
					classTermCount = new HashMap<QueryType, Double>();
				}
				int t_ct = countTerm(text_c, t);
				sum_t_ct += t_ct;
				classTermCount.put(c, (double) t_ct);
				condProb.put(t, classTermCount);
			}

			for (String t : v) {
				Map<QueryType, Double> classTermCount = condProb.get(t);
				condProb.get(t).put(c,
						(classTermCount.get(c) + 1) / (sum_t_ct + v.size()));
				// System.out.println(String.format("condprob[%s,%s] = %f", t,
				// c.toString(), condProb.get(t).get(c)));
				test_sum_condprob += condProb.get(t).get(c);
			}

			assert Math.abs(test_sum_condprob - 1) < 0.00001 : String
					.format("Conditional probabilities given class = '%s' do not sum up to 1",
							c.toString());
		}

		assert Math.abs(test_sum_prior - 1) < 0.00001 : "Priors do not sum up to 1";
		ClassifierTrainingInfo trainingInfo = new ClassifierTrainingInfoImpl(v,
				prior, condProb);
		return trainingInfo;
	}

	@Override
	public QueryType apply(List<QueryType> queryTypes,
			ClassifierTrainingInfo trainingInfo, String question) {
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
			QueryType queryType) {
		int count = 0;
		for (QuestionInfo question : questions) {
			if (question.getQueryType() == queryType) {
				count++;
			}
		}

		return count;
	}

	private List<String> concatQuestionsInclass(List<QuestionInfo> questions,
			QueryType queryType) {
		List<String> terms = new ArrayList<String>();
		for (QuestionInfo question : questions) {
			if (question.getQueryType() == queryType) {
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
		QueryType classifiedType = null;
		Double maxScore = Double.NEGATIVE_INFINITY;
		for (QueryType queryType : score.keySet()) {
			if (!suppressLog)
				System.out.printf("  %-5s => %.2f\n", queryType.toString(),
						score.get(queryType));
			if (score.get(queryType) > maxScore) {
				maxScore = score.get(queryType);
				classifiedType = queryType;
			}
		}

		return classifiedType;
	}
}
