package qa.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import qa.model.enumerator.QueryType;

public class ClassifierTrainingInfoImpl implements ClassifierTrainingInfo {
	private Set<QueryTerm> vocabulary;
	private Map<QueryType, Double> prior;
	private Map<QueryTerm, Map<QueryType, Double>> condProb;
	
	public ClassifierTrainingInfoImpl() {
		vocabulary = new HashSet<QueryTerm>();
		prior = new HashMap<QueryType, Double>();
		condProb = new HashMap<QueryTerm, Map<QueryType, Double>>();
	}
	
	@Override
	public Set<QueryTerm> getVocabulary() {
		return vocabulary;
	}

	@Override
	public Map<QueryType, Double> getPrior() {
		return prior;
	}

	@Override
	public Map<QueryTerm, Map<QueryType, Double>> getConditionalProbability() {
		return condProb;
	}

	public String toString() {
		String str = "[Training info]\n";
		String vStr = "";
		for (QueryTerm v : vocabulary) {
			vStr += v.getText() + ", ";
		}
		str += "- vocabulary = {" + vStr + "}\n";
		str += "- prior:\n";
		for (QueryType pk : prior.keySet()) {
			str += pk.toString() + ": " + prior.get(pk) + "\n";
		}
		
		str += "- conditional probability:\n";
		for (QueryTerm ck : condProb.keySet()) {
			int sum = 0;
			for (QueryType pk : condProb.get(ck).keySet()) {sum += condProb.get(ck).get(pk);
				str += ck.getText() + "," + pk.toString() + ": " + condProb.get(ck).get(pk) + "\n";
			}System.out.println(sum);
		}
		
		return "";
	}
}
