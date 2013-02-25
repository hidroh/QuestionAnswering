package qa.model;

import java.util.Map;
import java.util.Set;

import qa.model.enumerator.QueryType;

public interface ClassifierTrainingInfo {
	Set<QueryTerm> getVocabulary();

	Map<QueryType, Double> getPrior();

	Map<QueryTerm, Map<QueryType, Double>> getConditionalProbability();
}
