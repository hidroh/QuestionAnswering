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
        Map<String, String> answerInfo = new HashMap<String, String>();
        List<ResultInfo> results = new ArrayList<ResultInfo>();
        for (Passage passage : passages) {
            List<String> nameEntities = NeRecognizer.getInstance().getNameEntities(passage.getContent());
            for (String entity : nameEntities) {
                answerInfo.put(entity, passage.getDocumentId());
            }
        }

        try {
            String taggedAnswer = getAnswer(new ArrayList<String>(answerInfo.keySet()), questionInfo, irQuery);
            if (taggedAnswer.length() > 0) {
                results.add(new ResultInfoImpl(stripTag(taggedAnswer), answerInfo.get(taggedAnswer)));    
            }

            // adding alternative results for reference
            String alt = "{";
            for (String alternative : answerInfo.keySet()) {
                alt += stripTag(alternative) + "; ";
            }
            alt += "}";
            results.add(new ResultInfoImpl(alt, "alt"));    

        } catch (Exception e) {
            ApplicationHelper.printError("Answer Extractor: Unable to rank answers", e);
        }

        return results;
    }

    private String stripTag(String tagged) {
        return tagged.replaceAll("<\\w+>", "").replaceAll("</\\w+>", "").trim();
    }

    private String getAnswer(List<String> nameEntities, QuestionInfo info, String irQuery) throws Exception {
        List<String> taggedAnswers = mapAnswer(nameEntities, info);
        if (taggedAnswers.size() > 0) {
            return rankAnswers(taggedAnswers, irQuery);
        }
        
        return "";
    }

    private String rankAnswers(List<String> taggedAnswers, String irQuery) throws Exception {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_41, sa);
        iw = new IndexWriter(dir, config);

        for (String taggedAnswer : taggedAnswers) {
            String answer = stripTag(taggedAnswer);
            if (irQuery.toLowerCase().contains(answer.toLowerCase())) {
                continue; // do not consider answer that is part of question
            }
            String webResult = searchEngine.search(irQuery + " " + answer);
            addIndex(taggedAnswer, webResult);
        }

        iw.close();

        return getTopResult(irQuery, taggedAnswers.size());
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

    private List<String> mapAnswer(List<String> answers, QuestionInfo info) {
        List<String> results = new ArrayList<String>();
        List<String> types = new ArrayList<String>();
        if (Integer.parseInt(Settings.get("CLASSIFIER_LIMIT")) == 1) {
            types.addAll(getEntityType(info));
        } else {
            types.addAll(getMultiEntityType(info));
        }

        for (String answer : answers) {
            if (types.size() > 0) {
                for (String type : types) {
                    if (answer.contains("<" + type + ">")) {
                        if (!results.contains(answer)) {
                            results.add(answer);
                        }
                    }
                }
            } else {
                if (!results.contains(answer)) {
                    results.add(answer);
                }                
            }
        }

        return results;
    }

    private List<String> getEntityType(QuestionInfo info) {
        List<String> types = new ArrayList<String>();
        switch (info.getQueryType()) {
            case LOC: // Location
                types.add("LOCATION");
                break;
            case HUM:
                switch (info.getQuerySubType()) {
                    case HUM_ind: // Person
                    case HUM_desc:
                    case HUM_title:
                        types.add("PERSON");
                        break;
                    case HUM_gr: // Organization
                        types.add("ORGANIZATION");
                        break;
					default:
						break;
                }
                break;
            case NUM:
                switch (info.getQuerySubType()) {
                    case NUM_date: // Time / Date
                    case NUM_period:
                        types.add("TIME");
                        types.add("DATE");
                        break;
                    case NUM_money: // Money
                        types.add("MONEY");
                        break;
                    case NUM_perc: // Percent
                        types.add("PERCENT");
                        break;
					default:
						break;
                }
                break;
			default:
				break;
        }

        return types;
    }

    private List<String> getMultiEntityType(QuestionInfo info) {
        String[] classified = info.getMultiClassification().split(" ");
        Set<String> types = new HashSet<String>();
        for (String c : classified) {
            if (!c.contains("_")) {
                QueryType qt = QueryType.valueOf(c);
                switch (qt) {
                    case LOC:
                        types.add("LOCATION");
                        break;
                    case DESC:
                        types.add("LOCATION");
                        types.add("PERSON");
                        types.add("ORGANIZATION");
                    case HUM:
                    case ABBR:
                        types.add("PERSON");
                        types.add("ORGANIZATION");
                        break;
                    case NUM:
                        types.add("TIME");
                        types.add("DATE");
                        types.add("MONEY");
                        types.add("PERCENT");
                        break;
                    case ENTY:
                    default:
                        break;
                }
            } else {
                QuerySubType qst = QuerySubType.valueOf(c);
                switch (qst) {
                    case ABBR_abb:
                    case ABBR_exp:
                        types.add("LOCATION");
                        types.add("ORGANIZATION");
                        types.add("PERSON");
                        break;
                    case DESC_def:
                    case DESC_desc:
                    case DESC_manner:
                    case DESC_reason:
                        types.add("LOCATION");
                        types.add("PERSON");
                        types.add("ORGANIZATION");
                        break;
                    case HUM_desc:
                        types.add("PERSON");
                        types.add("ORGANIZATION");
                        break;
                    case HUM_ind:
                    case HUM_title:
                        types.add("PERSON");
                        break;
                    case HUM_gr:
                        types.add("ORGANIZATION");
                        break;
                    case LOC_city:
                    case LOC_country:
                    case LOC_mount:
                    case LOC_other:
                    case LOC_state:
                        types.add("LOCATION");
                        break;
                    case NUM_date:
                    case NUM_period:
                        types.add("TIME");
                        types.add("DATE");
                        break;
                    case NUM_count:
                    case NUM_money:
                        types.add("MONEY");
                        break;
                    case NUM_perc:
                        types.add("PERCENT");
                        break;
                    default:
                        break;
                }
            }
        }

        return new ArrayList<String>(types);
    }
}
