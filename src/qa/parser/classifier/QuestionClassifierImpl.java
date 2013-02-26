package qa.parser.classifier;

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
import qa.helper.QuestionClassifierHelper;

public class QuestionClassifierImpl implements QuestionClassifier {

	@Override
	public ClassifierTrainingInfo train(List<QueryType> queryTypes,
			List<QuestionInfo> questions) {
		ClassifierTrainingInfo trainingInfo = new ClassifierTrainingInfoImpl();
		Set<QueryTerm> v = trainingInfo.getVocabulary();
		Map<QueryType, Double> prior = trainingInfo.getPrior();
		Map<QueryTerm, Map<QueryType, Double>> condProb = trainingInfo
				.getConditionalProbability();
		v.addAll(extractVocabulary(questions));
		System.out.println(String.format("Training questions = %d, vocabulary count = %d", questions.size(), v.size()));
		int n = questions.size();

		double test_sum_prior = 0;
		for (QueryType c : queryTypes) {
			System.out.println(String.format("c = %s", c.toString()));
			int sum_t_ct = 0;
			int n_c = countQuestionsInClass(questions, c);
			prior.put(c, (double) n_c / n);
			test_sum_prior += (double) n_c / n;
			// System.out.println(String.format("prior[%s] = %f", c.toString(), prior.get(c)));
			double test_sum_condprob = 0;
			List<QueryTerm> text_c = concatQuestionsInclass(questions, c);
			for (QueryTerm t : v) {
				condProb.put(t, new HashMap<QueryType, Double>());
				int t_ct = countTerm(text_c, t);
				sum_t_ct += t_ct;
				condProb.get(t).put(c, (double) t_ct);
			}

			for (QueryTerm t : v) {
				Map<QueryType, Double> classTermCount = condProb.get(t);
				condProb.get(t).put(c,
						(classTermCount.get(c) + 1) / (sum_t_ct + v.size()));
				// System.out.println(String.format("condprob[%s,%s] = %f", t.getText(), c.toString(), condProb.get(t).get(c)));
				test_sum_condprob += condProb.get(t).get(c);
			}

			assert Math.abs(test_sum_condprob - 1) < 0.00001 : String.format("Conditional probabilities given class = '%s' do not sum up to 1", c.toString());
		}

		assert Math.abs(test_sum_prior - 1) < 0.00001 : "Priors do not sum up to 1";

		return trainingInfo;
	}

	@Override
	public QueryType apply(List<QueryType> queryTypes,
			ClassifierTrainingInfo trainingInfo, String question) {
		QuestionClassifierHelper helper = QuestionClassifierHelper.getInstance();
		List<QueryTerm> terms = helper.getQueryTerms(question);
		Map<QueryType, Double> score = new HashMap<QueryType, Double>();
		for (QueryType c : queryTypes) {
			score.put(c, Math.log(trainingInfo.getPrior().get(c)));
			for (QueryTerm t : terms) {
				double log_p_t_c = 0;
				if (trainingInfo.getConditionalProbability().containsKey(t)) {
					log_p_t_c = Math.log(trainingInfo.getConditionalProbability().get(t).get(c));

				} 

				score.put(c, score.get(c) + log_p_t_c);
			}
		}

		return getArgMax(score);
	}

	private Set<QueryTerm> extractVocabulary(List<QuestionInfo> questions) {
		Set<QueryTerm> terms = new HashSet<QueryTerm>();
		for (QuestionInfo question : questions) {
			List<QueryTerm> questionTerms = question.getQuestionTerms();
			for (QueryTerm questionTerm : questionTerms) {
				if (!isStopWord(questionTerm)) {
					terms.add(questionTerm);
				}
			}
		}

		return terms;
	}

	private boolean isStopWord(QueryTerm term) {
		// TODO Auto-generated method stub
		return false;
	}

	private int countQuestionsInClass(List<QuestionInfo> questions, QueryType queryType) {
		int count = 0;
		for (QuestionInfo question : questions) {
			if (question.getQueryType() == queryType) {
				count++;
			}
		}
		
		return count;
	}

	private List<QueryTerm> concatQuestionsInclass(List<QuestionInfo> questions, QueryType queryType) {
		List<QueryTerm> terms = new ArrayList<QueryTerm>();
		for (QuestionInfo question : questions) {
			if (question.getQueryType() == queryType) {
				terms.addAll(question.getQuestionTerms());
			}
		}
		
		return terms;
	}

	private int countTerm(List<QueryTerm> termList, QueryTerm term) {
		int count = 0;
		for (QueryTerm t : termList) {
			if (t.getText() == term.getText()) {
				count++;
			}
		}
		
		return count;
	}

	private QueryType getArgMax(Map<QueryType, Double> score) {
		QueryType classifiedType = null;
		Double maxScore = -100000.0;
		System.out.println(maxScore);
		for (QueryType queryType : score.keySet()) {
			System.out.println(queryType.toString() + "=>" + score.get(queryType));
			if (score.get(queryType) > maxScore) {
				maxScore = score.get(queryType);
				classifiedType = queryType;
			}
		}

		return classifiedType;
	}
}
