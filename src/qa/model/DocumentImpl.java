package qa.model;

public class DocumentImpl implements Document {
    private String id;
    private String content;

    public DocumentImpl(String id, String content) {
        this.id = id;
        this.content = content;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getContent() {
        return null;
    }
}
