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
		int n = questions.size();

		for (QueryType c : queryTypes) {
			int sum_t_ct = 0;
			int n_c = countQuestionsInClass(questions, c);
			prior.put(c, (double) n_c / n);
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
			}
		}

		return trainingInfo;
	}

	@Override
	public QueryType apply(List<QueryType> queryTypes,
			ClassifierTrainingInfo trainingInfo, String question) {
		// TODO Auto-generated method stub
		return null;
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

}
