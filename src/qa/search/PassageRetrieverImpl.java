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

import qa.Settings;

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
            System.err.println("Unable to init indexed directory");
        }
    }

    @Override
    public List<String> getPassages(String answerInfo) {
        try {
            if (!hasIndexData()) {
                indexDocument();
            }

            return query(answerInfo);
        } catch (Exception e) {
            System.err.println("Unable to retrieve passages");
        }

        return new ArrayList<String>();
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
        ArrayList<Document> docs = new ArrayList<Document>();

        String content = document.getContent().replace("\n\n\n", "|||").replace("\n", " ").replace("|||", "\n");

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

            if (passage.size() == passageSize) {
                String passageString = getPassageSentences(passage);
                Document doc = new Document();
                doc.add(new TextField("PASSAGE", passageString, Field.Store.YES));
                docs.add(doc);
            }
        }
        
        iw.addDocuments(docs);        
    }

    private String getPassageSentences(Queue<String> passage) {
        String passageString = "";
        for (String sentence : passage) {
            passageString += sentence;
        }

        return passageString;
    }

    private List<String> query(String queryString) throws Exception {
        List<String> results = new ArrayList<String>();
        ScoreDoc[] topHits;
        Query query = new QueryParser(Version.LUCENE_41, "PASSAGE", sa)
                .parse(queryString);

        IndexReader ir = DirectoryReader.open(indexDirectory);
        System.out.printf("Total passages indexed: %d\n", ir.numDocs());
        IndexSearcher is = new IndexSearcher(ir);
        TopScoreDocCollector collector = TopScoreDocCollector.create(
                Integer.parseInt(Settings.get("PASSAGE_HITS")), true);
        is.search(query, collector);
        topHits = collector.topDocs().scoreDocs;

        System.out.printf("Found %d passage hits\n", topHits.length);
        float cutOffScore = -1;
        if (topHits.length > 0) {
            cutOffScore = topHits[0].score * Float.parseFloat(Settings.get("PASSAGE_HIT_THRESHOLD"));
            System.out.printf("Cut off score = %f\n", cutOffScore);
        }
        for (int i = 0; i < topHits.length; ++i) {
            Document d = is.doc(topHits[i].doc);
            if (topHits[i].score >= cutOffScore) {
                System.out.printf("-----%f-----\n%s\n", topHits[i].score, d.get("PASSAGE"));
                results.add(d.get("PASSAGE"));
            }
        }

        return results;
    }
}
