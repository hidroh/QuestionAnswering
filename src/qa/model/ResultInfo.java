package qa.model;

/**
 * Interface for result information model, which contains information for
 * result to a question, including answer and document from which that
 * answer comes from
 */
public interface ResultInfo {
	/**
	 * Get answer for this result
	 * @return exact answer
	 */
	String getAnswer();
	
	/**
	 * Get supporting document for this result
	 * @return supporting document id
	 */
	String getSupportingDocumentId();
}
