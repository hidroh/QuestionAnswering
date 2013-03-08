package qa.model;

import java.util.Map;
import java.util.Set;

public interface ClassifierInfo {
	Set<String> getVocabulary();

	Map<String, Double> getPrior();

	Map<String, Map<String, Double>> getConditionalProbability();

    Map<String, Double> getSubPrior();

    Map<String, Map<String, Double>> getSubConditionalProbability();
}
