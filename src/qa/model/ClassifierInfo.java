package qa.model;

import java.util.Map;
import java.util.Set;

import qa.model.enumerator.QueryType;

public interface ClassifierInfo {
	Set<String> getVocabulary();

	Map<QueryType, Double> getPrior();

	Map<String, Map<QueryType, Double>> getConditionalProbability();
}
