package qa.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import qa.model.enumerator.QueryType;

@SuppressWarnings("serial")
public class ClassifierInfoImpl implements ClassifierInfo,
		Serializable {
	private Set<String> vocabulary;
	private Map<QueryType, Double> prior;
	private Map<String, Map<QueryType, Double>> condProb;

	public ClassifierInfoImpl() {
		vocabulary = new HashSet<String>();
		prior = new HashMap<QueryType, Double>();
		condProb = new HashMap<String, Map<QueryType, Double>>();
	}

	public ClassifierInfoImpl(Set<String> vocabulary,
			Map<QueryType, Double> prior,
			Map<String, Map<QueryType, Double>> condProb) {
		this.vocabulary = vocabulary;
		this.prior = prior;
		this.condProb = condProb;
	}

	@Override
	public Set<String> getVocabulary() {
		return vocabulary;
	}

	@Override
	public Map<QueryType, Double> getPrior() {
		return prior;
	}

	@Override
	public Map<String, Map<QueryType, Double>> getConditionalProbability() {
		return condProb;
	}

	public String toString() {
		String str = "\nTraining info\n";
		str += String.format("- vocabulary = %d\n", vocabulary.size());
		str += "- prior:\n";
		for (QueryType pk : prior.keySet()) {
			str += String.format("%-5s: %.2f\n", pk.toString(), prior.get(pk));
		}

		str += "- log conditional probability:\n";
		int count = 0;
		for (String t : condProb.keySet()) {
			if (count < 20) {
				str += String.format("%-20s [ ", t);
				for (QueryType c : condProb.get(t).keySet()) {
					str += String.format("%-5s: %.2f, ", c.toString(),
							Math.log(condProb.get(t).get(c)));
				}
				str += " ]\n";

				count++;
			} else {
				str += "...\n";
				break;
			}
		}

		return str;
	}

}
