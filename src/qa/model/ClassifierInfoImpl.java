package qa.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class ClassifierInfoImpl implements ClassifierInfo,
		Serializable {
	private Set<String> vocabulary;
	private Map<String, Double> prior;
	private Map<String, Map<String, Double>> condProb;
	private Map<String, Double> subPrior;
	private Map<String, Map<String, Double>> subCondProb;

	public ClassifierInfoImpl() {
		vocabulary = new HashSet<String>();
		prior = new HashMap<String, Double>();
		condProb = new HashMap<String, Map<String, Double>>();
		subPrior = new HashMap<String, Double>();
		subCondProb = new HashMap<String, Map<String, Double>>();
	}

	public ClassifierInfoImpl(Set<String> vocabulary,
			Map<String, Double> prior,
			Map<String, Map<String, Double>> condProb,
			Map<String, Double> subPrior,
			Map<String, Map<String, Double>> subCondProb) {
		this.vocabulary = vocabulary;
		this.prior = prior;
		this.condProb = condProb;
		this.subPrior = subPrior;
		this.subCondProb = subCondProb;
	}

	@Override
	public Set<String> getVocabulary() {
		return vocabulary;
	}

	@Override
	public Map<String, Double> getPrior() {
		return prior;
	}

	@Override
	public Map<String, Map<String, Double>> getConditionalProbability() {
		return condProb;
	}

	@Override
	public Map<String, Double> getSubPrior() {
		return subPrior;
	}

	@Override
	public Map<String, Map<String, Double>> getSubConditionalProbability() {
		return subCondProb;
	}

	public String toString() {
		String str = "\nTraining info\n";
		str += String.format("- vocabulary = %d\n", vocabulary.size());
		str += "- prior:\n";
		for (String pk : prior.keySet()) {
			str += String.format("%-5s: %.2f\n", pk.toString(), prior.get(pk));
		}

		str += "- log conditional probability:\n";
		int count = 0;
		for (String t : condProb.keySet()) {
			if (count < 10) {
				str += String.format("%-20s [ ", t);
				for (String c : condProb.get(t).keySet()) {
					str += String.format("%-5s: %.2f, ", c,
							Math.log(condProb.get(t).get(c)));
				}
				str += " ]\n";

				count++;
			} else {
				str += "...\n";
				break;
			}
		}

		str += "- sub prior:\n";
		count = 0;
		for (String pk : subPrior.keySet()) {
			str += String.format("%-20s: %.2f", pk.toString(), subPrior.get(pk));
			count++;
			if (count % 3 == 0) {
				str += "\n";
			} else {
				str += "\t\t";
			}
		}

		str += "\n- log sub conditional probability:\n";
		count = 0;
		for (String t : subCondProb.keySet()) {
			if (count < 10) {
				str += String.format("%-20s [ ", t);
				int innerCount = 0;
				for (String c : subCondProb.get(t).keySet()) {
					if (innerCount < 5) {
					str += String.format("%-5s: %.2f, ", c,
							Math.log(subCondProb.get(t).get(c)));
					}
					
					innerCount++;
				}
				str += "... ]\n";

				count++;
			} else {
				str += "...\n";
				break;
			}
		}

		return str;
	}

}
