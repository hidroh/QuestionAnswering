package qa.search;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import qa.helper.ApplicationHelper;
import qa.Settings;

public class DocumentRetrieverImpl implements DocumentRetriever {
	private StandardAnalyzer sa;
	private Directory indexDir;
	private int NUM_HITS; // Number of hits to be displayed

	public DocumentRetrieverImpl() {
		NUM_HITS = Integer.parseInt(Settings.get("DOCUMENT_HITS"));
		sa = new StandardAnalyzer(Version.LUCENE_41);
		try {
			indexDir = new MMapDirectory(new File(Settings.get("INDEX_PATH")));
		} catch (IOException e) {
			ApplicationHelper.printError("Document Retriever: Unable to load indexed data", e);
		}
	}

	@Override
	public List<qa.model.Document> getDocuments(String queryString) {
		if (indexDir == null) {
			return new ArrayList<qa.model.Document>();
		}


		try {
			List<qa.model.Document> results = new ArrayList<qa.model.Document>();
			Query query = new QueryParser(Version.LUCENE_41, "TEXT", sa)
					.parse(queryString);

			IndexReader ir = DirectoryReader.open(indexDir);
	        ApplicationHelper.printDebug(String.format("Total documents indexed: %d\n", ir.numDocs()));
			IndexSearcher is = new IndexSearcher(ir);
			TopScoreDocCollector collector = TopScoreDocCollector.create(
					NUM_HITS, true);
			is.search(query, collector);
			ScoreDoc[] topHits = collector.topDocs().scoreDocs;

			ApplicationHelper.printDebug(String.format("Found %d document hits\n", topHits.length));
			double cutOffScore = -1;
			if (topHits.length > 0) {
				cutOffScore = Double.parseDouble(Settings.get("HIT_THRESHOLD")) * topHits[0].score;
			}
			ApplicationHelper.printDebug(String.format("Cut off score = %f\n", cutOffScore));
			for (int i = 0; i < topHits.length; i++) {
				if (topHits[i].score >= cutOffScore) {
					Document d = is.doc(topHits[i].doc);
					ApplicationHelper.printDebug(
						String.format(
							"[%f] Document id = %s; File name = %s\n", 
							topHits[i].score,
							d.get("DOCNO"),
							d.get("FILENAME")
						)
					);
					results.add(new qa.model.DocumentImpl(d.get("DOCNO"), d.get("TEXT")));
				} else {
					break;
				}
			}

			return results;
		} catch (ParseException e) {
			ApplicationHelper.printError("Document Retriever: Unable to parse Lucene query", e);
		} catch (IOException e) {
			ApplicationHelper.printError("Document Retriever: Unable to read from index", e);
		}

		return new ArrayList<qa.model.Document>();
	}
}
