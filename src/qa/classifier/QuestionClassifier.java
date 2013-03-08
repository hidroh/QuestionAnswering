package qa.classifier;

import java.util.List;

import qa.model.enumerator.QueryType;
import qa.model.ClassifierInfo;
import qa.model.QuestionInfo;

public interface QuestionClassifier {
	ClassifierInfo train(List<QueryType> queryTypes,
			List<QuestionInfo> questions);

	QueryType apply(List<QueryType> queryTypes,
			ClassifierInfo trainingInfo, String question);
}