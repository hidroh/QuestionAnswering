package qa.model;

/**
 * Interface for document model, which represents a document in data set
 */
public interface Document {
	/**
	 * Gets document ID
	 * @return ID of this document
	 */
	String getId();

    String getContent();
}
