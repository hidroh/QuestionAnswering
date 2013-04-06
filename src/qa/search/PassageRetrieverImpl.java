package qa.search;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.BufferedWriter;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.MMapDirectory;

import qa.Settings;
import qa.model.Passage;

public class PassageRetrieverImpl implements PassageRetriever {
    private IndexWriter iw;
    private StandardAnalyzer sa;
    private Directory indexDirectory;
    private final String DELIMETER = ".";
    private qa.model.Document document;
    private String indexPath;
    private String documentPath;

    public PassageRetrieverImpl(qa.model.Document document) {
        this.document = document;
        documentPath = Settings.get("PASSAGE_DOCUMENT_PATH") + File.separator + document.getId();
        indexPath = Settings.get("PASSAGE_INDEX_PATH") + document.getId();
        try {
            File file = new File(Settings.get("PASSAGE_DOCUMENT_PATH"), document.getId());
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();

                File dir = new File(indexPath);
                dir.mkdir();

                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(document.getContent());
                bw.close();
            }
        } catch (IOException e) {
        }

        sa = new StandardAnalyzer(Version.LUCENE_41);
        try {
            indexDirectory = new MMapDirectory(new File(indexPath));
        } catch (IOException e) {
            System.err.println("Unable to init indexed directory");
        }
    }

    @Override
    public List<Passage> getPassages(String answerInfo) {
        if (!hasIndexData()) {
            indexDocument();
        }

        return new ArrayList<Passage>();
    }

    private boolean hasIndexData() {
        File file = new File(indexPath);
        if(file.isDirectory()){
            if(file.list().length > 0) {
                return true;
            }
        }

        return false;
    }

    private void indexDocument() {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_41, sa);

        try {
            iw = new IndexWriter(indexDirectory, config);
            parseDocument(document);
            iw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void parseDocument(qa.model.Document document) throws IOException {
        Document doc = null;
        ArrayList<Document> docs = null;

        docs = new ArrayList<Document>();

        iw.addDocuments(docs);

        doc.add(new TextField("TEXT", "", Field.Store.YES));

        docs.add(doc);
    }
}
