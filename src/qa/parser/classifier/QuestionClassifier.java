package qa.parser.classifier;

import java.util.List;

import qa.model.enumerator.QueryType;
import qa.model.ClassifierTrainingInfo;
import qa.model.QuestionInfo;

public interface QuestionClassifier {
	ClassifierTrainingInfo train(List<QueryType> queryTypes,
			List<QuestionInfo> questions);

	QueryType apply(List<QueryType> queryTypes,
			ClassifierTrainingInfo trainingInfo, String question);
}