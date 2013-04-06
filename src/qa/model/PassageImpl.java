package qa.model;

public class PassageImpl implements Passage {
    private String docId;
    private String content;

    public PassageImpl(String docId, String content) {
        this.docId = docId;
        this.content = content;
    }

    public String getDocumentId() {
        return docId;
    }

    public String getContent() {
        return content;
    }
}
