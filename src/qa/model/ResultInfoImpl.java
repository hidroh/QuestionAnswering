package qa.model;

public class ResultInfoImpl implements ResultInfo {
    private String answer;
    private String docId;

    public ResultInfoImpl(String answer, String docId) {
        this.answer = answer;
        this.docId = docId;
    }

    public String getAnswer() {
        return answer;
    }
    
    public String getSupportingDocumentId() {
        return docId;
    }
}
