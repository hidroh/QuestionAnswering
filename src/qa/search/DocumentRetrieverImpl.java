package qa.search;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import qa.helper.ApplicationHelper;
import qa.indexer.ValueComparator;
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
			ApplicationHelper.printError("Unable to load indexed data");
		}
	}

	@Override
	public List<qa.model.Document> getDocuments(String queryString) { // List<QueryTerm>
																	// query
		if (indexDir == null) {
			return new ArrayList<qa.model.Document>();
		}

		List<String> result = new ArrayList<String>();
		HashMap<String, Integer> docHits = new HashMap<String, Integer>();

		ScoreDoc[] topHits;
		try {
			Query query = new QueryParser(Version.LUCENE_41, "TEXT", sa)
					.parse(queryString);

			IndexReader ir = DirectoryReader.open(indexDir);
	        ApplicationHelper.printDebug(String.format("Total documents indexed: %d\n", ir.numDocs()));
			IndexSearcher is = new IndexSearcher(ir);
			TopScoreDocCollector collector = TopScoreDocCollector.create(
					NUM_HITS, true);
			is.search(query, collector);
			topHits = collector.topDocs().scoreDocs;

			ApplicationHelper.printDebug(String.format("Found %d document hits\n", topHits.length));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < topHits.length; ++i) {
				
				Document d = is.doc(topHits[i].doc);
				
				sb.append(d.get("FILENAME"));
				sb.append(";");
				sb.append(d.get("DOCNO"));
				
				if (docHits.containsKey(sb.toString())){
					docHits.put(sb.toString(), docHits.get(sb.toString()) + 1);

				} else {
					docHits.put(sb.toString(), 1);
				}
				ApplicationHelper.printDebug(String.format("Document id = %s; File name = %s\n", d.get("DOCNO"), d.get("FILENAME")));
				result.add(d.get("TEXT"));
				sb = new StringBuilder();
			}

		} catch (Exception e) {
			ApplicationHelper.printError(e);
		}

		// result contains the paragraphs with the hits
		return printResult(result, docHits);
	}

	private List<qa.model.Document> printResult(List<String> result, HashMap<String, Integer> map) {
		int i = 0;
		
		ValueComparator bvc = new ValueComparator(map);
		TreeMap<String,Integer> sortedMap = new TreeMap<String,Integer>(bvc);
		
		sortedMap.putAll(map);
		
		Map.Entry<String, Integer> entry = sortedMap.firstEntry();
		String val = entry.getKey();
		int hits = entry.getValue();
		ApplicationHelper.printDebug(val + " "+ hits + "\n");
		
		sortedMap.remove(val);
		
		ArrayList<String> ans = new ArrayList<String>();
		ans.add(val);
		
		// while next entry is within HIT_THRESHOLD % of this one, return it aswell
		while (!sortedMap.isEmpty()){
			Map.Entry<String, Integer> tmp = sortedMap.firstEntry();
			if (Double.parseDouble(Settings.get("HIT_THRESHOLD"))*hits > entry.getValue()){
				break;
			} else {
				ans.add(tmp.getKey());
				sortedMap.remove(tmp.getKey());
			}
		}
		
		List<qa.model.Document> documentList = new ArrayList<qa.model.Document>();
		for (String s : ans){
			String[] tmp = s.split(";");
			try {
				qa.model.Document doc = new qa.model.DocumentImpl(tmp[1], findDocument(tmp[1], tmp[0]));
				documentList.add(doc);
			} catch (FileNotFoundException e) {
				ApplicationHelper.printError(e);
			}
		}

		return documentList;
	}

	private String findDocument(String docId, String filePath) throws FileNotFoundException {
		Scanner scanner = new Scanner(new File(filePath));
		StringBuilder sb = new StringBuilder();
		while(scanner.hasNextLine()){
			String next = scanner.nextLine();
			if (next.contains(docId)){
				while (scanner.hasNextLine()){
					next = scanner.nextLine();
					if (next.contains("<TEXT>")){
						sb.append(next.replace("<TEXT>", "").replace("<P>", "").replace("</P>", ""));
						sb.append("\n");
						
						while (!next.contains("</TEXT>")){
							next = scanner.nextLine();
							sb.append(next.replace("</TEXT>", "").replace("<P>", "").replace("</P>", ""));
							sb.append("\n");
						}
						scanner.close();
						return sb.toString().trim();
					}
				}
			}
		}
		scanner.close();
		return null;
	}
}
