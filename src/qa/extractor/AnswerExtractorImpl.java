package qa.extractor;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import qa.search.web.WebSearchApplication;
import qa.Settings;
import qa.helper.ChunkerWrapper;
import qa.model.QuestionInfo;
import qa.model.ResultInfo;
import qa.model.Passage;
import qa.model.ResultInfoImpl;
import qa.helper.NeRecognizer;
import qa.helper.ApplicationHelper;
import qa.model.enumerator.QueryType;
import qa.model.enumerator.QuerySubType;

public class AnswerExtractorImpl implements AnswerExtractor {
    private IndexWriter iw;
    private StandardAnalyzer sa;
    private Directory dir;
    private WebSearchApplication searchEngine;

    public AnswerExtractorImpl(WebSearchApplication searchEngine) {
        this.searchEngine = searchEngine;
        sa = new StandardAnalyzer(Version.LUCENE_41);
        try {
            File indexDir = new File(Settings.get("ANSWER_INDEX_PATH"));
            if (!indexDir.exists()) {
                indexDir.mkdir();
            }

            if (indexDir.isDirectory()) {
                for (File f : indexDir.listFiles()) {
                    f.delete();
                }
            }

            dir = new MMapDirectory(indexDir);
        } catch (IOException e) {
            ApplicationHelper.printError("Unable to init indexed directory", e);
        }
    }

    public List<ResultInfo> extractAnswer(List<Passage> passages, 
            QuestionInfo questionInfo,
            String irQuery) {
        List<ResultInfo> results = new ArrayList<ResultInfo>();
        Map<String, String> answerInfo = getAnswers(passages);

        try {
            String answer = rankAnswers(new ArrayList<String>(answerInfo.keySet()), questionInfo);
            if (answer.length() > 0) {
                results.add(new ResultInfoImpl(answer, answerInfo.get(answer)));    
            }

            results.add(new ResultInfoImpl(getAlternatives(answerInfo), "alt"));    

        } catch (Exception e) {
            ApplicationHelper.printError("Answer Extractor: Unable to rank answers", e);
        }

        return results;
    }

    private Map<String, String> getAnswers(List<Passage> passages) {
        Map<String,String> answerInfo = new HashMap<String, String>();
        List<ResultInfo> results = new ArrayList<ResultInfo>();
        for (Passage passage : passages) {
            List<String> nameEntities = NeRecognizer.getInstance().getNameEntities(passage.getContent());
            // List<String> nameEntities = new ArrayList<String>();
            // nameEntities.addAll(ChunkerWrapper.getInstance().getSortedChunksForAnswer(passage.getContent()));
            for (String entity : nameEntities) {
                if (!answerInfo.containsKey(stripTag(entity))) {
                    answerInfo.put(stripTag(entity), passage.getDocumentId());
                }
            }
        }

        ApplicationHelper.printlnDebug(String.format("Possible answers = %s", answerInfo.keySet().toString()));
        return answerInfo;
    }

    private String getAlternatives(Map<String, String> answerInfo) {
    // adding alternative results for reference
        String alt = "{";
        alt += ApplicationHelper.join(answerInfo.keySet().toArray(new String[0]), "; ");
        alt += "}";
        return alt;
    }

    private String stripTag(String tagged) {
        return tagged.replaceAll("<\\w+>", "").replaceAll("</\\w+>", "").trim();
    }

    private String rankAnswers(List<String> answers, QuestionInfo info) throws Exception {
        if (answers.size() == 0) {
            return "";
        }

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_41, sa);
        iw = new IndexWriter(dir, config);
        ApplicationHelper.printlnDebug("Search web for: ");
        
        // answers.addAll(info.getExpandedTerms());
        String webQuery = ApplicationHelper.join(info.getQueryTerms(), " ");

        for (String answer : answers) {
            if (info.getRaw().toLowerCase().contains(answer.toLowerCase())) {
                continue; // do not consider answer that is part of question
            }
            ApplicationHelper.printlnDebug(String.format("\"%s\"", webQuery + " " + answer));
            String webResult = searchEngine.search(webQuery + " " + answer);
            addIndex(answer, webResult);
        }

        iw.close();

        String irQuery = webQuery;
        if (info.getExpandedTerms() != null) {
            irQuery += " " + ApplicationHelper.join(info.getExpandedTerms(), " ");
        }
        
        return getTopResult(irQuery, answers.size());
    }

    private void addIndex(String answer, String webResult) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("ID", answer, Field.Store.YES));
        doc.add(new TextField("TEXT", webResult, Field.Store.YES));

        iw.addDocument(doc);
    }

    private String getTopResult(String queryString, int limit) throws Exception {
        Query query = new QueryParser(Version.LUCENE_41, "TEXT", sa)
                .parse(queryString);

        IndexReader ir = DirectoryReader.open(dir);
        IndexSearcher is = new IndexSearcher(ir);
        TopScoreDocCollector collector = TopScoreDocCollector.create(
                limit, true);
        is.search(query, collector);
        ScoreDoc[] topHits = collector.topDocs().scoreDocs;
        
        for (ScoreDoc sc : topHits) {
            ApplicationHelper.printDebug(String.format("%f %s\n", sc.score, is.doc(sc.doc).get("ID")));
        }

        if (topHits.length >0) {
            return is.doc(topHits[0].doc).get("ID");
        }

        return "";
    }
}
