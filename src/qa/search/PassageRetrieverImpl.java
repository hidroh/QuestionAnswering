package qa.search;

import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.document.StringField;

import qa.Settings;
import qa.helper.ApplicationHelper;
import qa.model.Passage;
import qa.model.PassageImpl;

public class PassageRetrieverImpl implements PassageRetriever {
    private IndexWriter iw;
    private StandardAnalyzer sa;
    private Directory indexDirectory;
    private qa.model.Document document;
    private String indexPath;

    public PassageRetrieverImpl(qa.model.Document document) {
        this.document = document;
        indexPath = Settings.get("PASSAGE_INDEX_PATH") + document.getId();
        File dir = new File(indexPath);
        if (!dir.exists()) {
            dir.mkdir();    
        } else if (Integer.parseInt(Settings.get("FORCE_REINDEX")) == 1) {
			if (dir.isDirectory()) {
				for (File f : dir.listFiles()) {
					f.delete();
				}
			}
			
        	dir.delete();
        	dir.mkdir();
        }

        sa = new StandardAnalyzer(Version.LUCENE_41);
        try {
            indexDirectory = new MMapDirectory(new File(indexPath));
        } catch (IOException e) {
            ApplicationHelper.printError("Unable to init passage indexed directory");
        }
    }

    @Override
    public List<Passage> getPassages(String answerInfo) {
        try {
            if (!hasIndexData()) {
                indexDocument();
            }

            return query(answerInfo);
        } catch (Exception e) {
            ApplicationHelper.printError("Unable to retrieve passages");
        }

        return new ArrayList<Passage>();
    }

    private boolean hasIndexData() {
        if (Integer.parseInt(Settings.get("FORCE_REINDEX")) == 1) {
            return false;
        }

        File file = new File(indexPath);
        if(file.isDirectory()){
            if(file.list().length > 0) {
                return true;
            }
        }

        return false;
    }

    private void indexDocument() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_41, sa);

        iw = new IndexWriter(indexDirectory, config);

        ArrayList<Document> docs = new ArrayList<Document>();
        String content = processDocumentContent();

        Pattern sentencePattern = Pattern.compile("((?:.*))",
                Pattern.CASE_INSENSITIVE);
        Matcher m = sentencePattern.matcher(content);
        Queue<String> passage = new LinkedList<String>();
        int passageSize = Integer.parseInt(Settings.get("PASSAGE_SENTENCES"));
        while (m.find()) {
            String sentence = m.group();
            if (sentence.trim().length() == 0) {
                continue;
            }

            if (passage.size() < passageSize) {
                passage.offer(sentence);
            } else {
                passage.poll();
                passage.offer(sentence);
            }

            if (passage.size() == passageSize || m.groupCount() == 1) {
                String passageString = getPassageSentences(passage);
                Document doc = new Document();
                doc.add(new TextField("PASSAGE", passageString, Field.Store.YES));
                doc.add(new StringField("DOCID", document.getId(), Field.Store.YES));
                docs.add(doc);
            }
        }
        
        iw.addDocuments(docs);

        iw.close(); 
    }

    private String processDocumentContent() {
        String content = document.getContent();

        int contentType = 0;
        if (content.indexOf("<P>") > -1 && content.indexOf("</P>") > -1) {
            contentType = 1;
        }

        if (contentType == 0) {
            return content.replace("\t", "|||").replace("\n", " ").replace("|||", "\n");
        } else {
            return content.replace("<P>", "\n").replace("</P>", "|||").replace("\n", " ").replace("|||", "\n");
        }
    }

    private String getPassageSentences(Queue<String> passage) {
        String passageString = "";
        for (String sentence : passage) {
            passageString += sentence;
        }

        return passageString;
    }

    private List<Passage> query(String queryString) throws Exception {
        List<Passage> results = new ArrayList<Passage>();
        ScoreDoc[] topHits;
        Query query = new QueryParser(Version.LUCENE_41, "PASSAGE", sa)
                .parse(queryString);

        IndexReader ir = DirectoryReader.open(indexDirectory);
        ApplicationHelper.printDebug(String.format("Total passages indexed: %d\n", ir.numDocs()));
        IndexSearcher is = new IndexSearcher(ir);
        TopScoreDocCollector collector = TopScoreDocCollector.create(
                Integer.parseInt(Settings.get("PASSAGE_HITS")), true);
        is.search(query, collector);
        topHits = collector.topDocs().scoreDocs;

        ApplicationHelper.printDebug(String.format("Found %d passage hits for %s\n", topHits.length, document.getId()));
        float cutOffScore = -1;
        if (topHits.length > 0) {
            cutOffScore = topHits[0].score * Float.parseFloat(Settings.get("PASSAGE_HIT_THRESHOLD"));
            ApplicationHelper.printDebug(String.format("Cut off score = %f\n", cutOffScore));
        }
        for (int i = 0; i < topHits.length; i++) {
            Document d = is.doc(topHits[i].doc);
            if (topHits[i].score >= cutOffScore) {
                ApplicationHelper.printDebug(String.format("-----%f-----\n%s\n", topHits[i].score, d.get("PASSAGE")));
                results.add(new PassageImpl(d.get("DOCID"), d.get("PASSAGE")));
            } else {
                break;
            }
        }

        return results;
    }
}
