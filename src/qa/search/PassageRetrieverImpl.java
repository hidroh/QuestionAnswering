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
    private Directory dir;
    private final String DELIMETER = ".";
    private qa.model.Document document;

    public PassageRetrieverImpl(qa.model.Document document) {
        this.document = document;
        try {
            File file = new File(Settings.get("PASSAGE_INDEX_PATH"), document.getId());
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();

                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(document.getContent());
                bw.close();
            }
        } catch (IOException e) {
        }

        sa = new StandardAnalyzer(Version.LUCENE_41);
        try {
            dir = new MMapDirectory(new File(Settings.get("PASSAGE_INDEX_PATH")));
        } catch (IOException e) {
            System.err.println("Unable to init indexed directory");
        }
    }

    @Override
    public List<Passage> getPassages(String answerInfo) {
        return new ArrayList<Passage>();
    }

    private void indexDocument(String documentPath) {
        Collection<File> allDocs = new ArrayList<File>();
        addFiles(new File(documentPath), allDocs);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_41, sa);

        try {
            iw = new IndexWriter(dir, config);
            for (File file : allDocs) {
                parseFile(file);
            }
            iw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void parseFile(File file) throws IOException {
        Scanner scanner = new Scanner(file);
        Document doc = null;
        String docno = null;
        ArrayList<Document> docs = null;

        while (scanner.hasNextLine()) {
            String nextLine = scanner.nextLine();
            
            if (nextLine.contains("<DOC>")) {
                
                docs = new ArrayList<Document>();
            
            } else if (nextLine.contains("</DOC>")) {
                iw.addDocuments(docs);
                doc = null;
                docno = null;
                docs = null;    
                
            } else if (nextLine.contains("<DOCNO>")
                    && nextLine.contains("</DOCNO>")) {

                docno = nextLine.substring(7, nextLine.length() - 8).trim();

            } else if (nextLine.equals("<TEXT>") && docno != null) { 
                // The body of text of the document. We will save every
                // paragraph as in a separate document in Lucene.

                StringBuilder sbParagraph = null; // text of a paragraph will
                                                    // later be
                // added here.
                boolean containedParagraph = false;

                while (!(nextLine = scanner.nextLine()).equals("</TEXT>")) {
                    // loop until we reach the end of text

                    if (nextLine.contains("<P>")) {
                        // beginning of paragraph

                        sbParagraph = new StringBuilder();
                        doc = new Document();
                        sbParagraph.append(nextLine);
                        sbParagraph.append("\n");
                        containedParagraph = true;

                    } else if (nextLine.contains("</P>")) {
                        // end of paragraph.
                        sbParagraph.append(nextLine);
                        String tmp = sbParagraph.substring(3, sbParagraph.length() - 4).trim();

                        doc.add(new TextField("TEXT", tmp, Field.Store.YES));
                        doc.add(new TextField("DOCNO", docno, Field.Store.YES));
                        doc.add(new StringField("FILENAME", file.getAbsolutePath(), Field.Store.YES));
                        //System.out.println(file.getAbsolutePath());

                        docs.add(doc);
                        
                    } else if (containedParagraph) {
                        sbParagraph.append(nextLine);
                        sbParagraph.append("\n");
                    }
                }

            } else {

                // Do nothing
            }
        }

        System.out.println(file.getName());
        scanner.close();
    }

    private void addFiles(File documentPath, Collection<File> allDocs) {
        File[] children = documentPath.listFiles();

        if (children != null) {
            for (File child : children) {
                if (child.isFile() && !child.getName().contains(DELIMETER)) {
                    allDocs.add(child);
                } else {
                    addFiles(child, allDocs);
                }
            }
        }
    }
}
