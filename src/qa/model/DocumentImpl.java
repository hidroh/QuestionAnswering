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
        return id;
    }

    @Override
    public String getContent() {
        return content;
    }

    public String toString() {
        String output = "";
        output += String.format("[Doc id: %s]\n", id);
        output += String.format("{\n%s\n}\n", content);
        output += "--------EOD---------\n";
        return output;
    }
}
