package qa.indexer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import qa.Settings;
//import qa.indexer.DocumentIndexer;
//import qa.model.Document;
//import qa.model.QueryTerm;

public class LuceneIndexer {

	public static void main(String[] args) {
		try {
			dir = new MMapDirectory(new File(DIRECTORY_PATH));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		importDocuments(Settings.get("DOCUMENT_PATH"));
		String debug = "lenihan";

		List<String> ans = getDocuments(debug);
	}

	public void setIndexer() { // DocumentIndexer indexer)
		// TODO Auto-generated method stub

	}

	static int NUM_HITS = 10; // Number of hits to be displayed

	public static List<String> getDocuments(String queryString) { // List<QueryTerm>
																	// query

		List<String> result = new ArrayList<String>();
		HashMap<String, Integer> docHits = new HashMap<String, Integer>();

		ScoreDoc[] topHits;
		try {
			Query query = new QueryParser(Version.LUCENE_41, "TEXT", sa)
					.parse(queryString);

			IndexReader ir = IndexReader.open(dir);
			IndexSearcher is = new IndexSearcher(ir);
			TopScoreDocCollector collector = TopScoreDocCollector.create(
					NUM_HITS, true);
			is.search(query, collector);
			topHits = collector.topDocs().scoreDocs;

			System.out.println("Found " + topHits.length + " hits.");
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
				System.out.println("Document id = " + d.get("DOCNO"));
				result.add(d.get("TEXT"));
				sb = new StringBuilder();
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		// result contains the paragraphs with the hits
		return printResult(result, docHits);
	}

	private static ArrayList<String> printResult(List<String> result, HashMap<String, Integer> map) {
		int i = 0;
		for (String s : result) {
			System.out.println(++i + ": " + s);
		}
		
		ValueComparator bvc = new ValueComparator(map);
		TreeMap<String,Integer> sortedMap = new TreeMap<String,Integer>(bvc);
		
		sortedMap.putAll(map);
		
		Map.Entry<String, Integer> entry = sortedMap.firstEntry();
		String val = entry.getKey();
		int hits = entry.getValue();
		System.out.println(val + " "+ hits);
		
		System.out.println(sortedMap.remove(val));
		
		ArrayList<String> ans = new ArrayList<String>();
		ans.add(val);
		System.out.println(sortedMap);
		
		// while next entry is within 90% of this one, return it aswell
		while (!sortedMap.isEmpty()){
			Map.Entry<String, Integer> tmp = sortedMap.firstEntry();
			if (0.9*hits > entry.getValue()){
				break;
			} else {
				ans.add(tmp.getKey());
				sortedMap.remove(tmp.getKey());
			}
			//System.out.println(sortedMap.size());
		}
		
		ArrayList<String> documentList = new ArrayList<String>();
		for (String s : ans){
			String[] tmp = s.split(";");
			try {
				documentList.add(findDocument(tmp[1], tmp[0]));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(documentList);
		return documentList;
	}

	private static String findDocument(String docId, String filePath) throws FileNotFoundException {
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
						return sb.toString().trim();
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param documentPath
	 *            The location of the AQUAINT corpus folder
	 */
	static IndexWriter iw;
	static StandardAnalyzer sa = new StandardAnalyzer(Version.LUCENE_41);
	static Directory dir;
	static String DIRECTORY_PATH = Settings.get("INDEX_PATH");

	public static void importDocuments(String documentPath) {

		Collection<File> allDocs = new ArrayList<File>();
		addFiles(new File(documentPath), allDocs);
		//addFiles(new File("C:/Users/Vladan/Documents/trec sample"), allDocs);
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

	// TODO Every paragraph will contain name of the document where it came from
	// and classified text. Decide if to include further information
	private static void parseFile(File file) throws IOException {
		Scanner scanner = new Scanner(file);
		Document doc = null;
		String docno = null;
		ArrayList<Document> docs = null;
		//StringBuilder sbText = null;

		while (scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			
			if (nextLine.contains("<DOC>")) {
				
				docs = new ArrayList<Document>();
				//sbText = new StringBuilder();
			
			} else if (nextLine.contains("</DOC>")) {
				//System.out.println(sbText.toString());
//				for (Document d : docs){
//					d.add(new StringField("DOC", sbText.toString(), Field.Store.YES));
//				}
				
				iw.addDocuments(docs);
				doc = null;
				docno = null;
				docs = null;	
//				sbText = null;
				
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
						
//						sbText.append(tmp);
//						sbText.append("\n\n");
//						
//						//System.out.println(sbText.toString());

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

	// static AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier
	// .getClassifierNoExceptions("C:/Users/Vladan/Documents/stanford-ner-2012-11-11/classifiers/english.all.3class.distsim.crf.ser.gz");
	// // TODO

	// static String PERSON = "<PERSON>";
	// static String ORGANIZATION = "<ORGANIZATION>";
	// static String LOCATION = "<LOCATION>";

	/**
	 * Looks to classify terms in the text and adds the classified term to the
	 * lucene document
	 * 
	 * @param sb
	 *            String to be classified.
	 * @param doc
	 *            Lucene Document to add the classified terms to.
	 * @return False if there were no terms that were successfully classified,
	 *         true otherwise.
	 */
	// private static boolean classifyText(StringBuilder sb, Document doc) {
	//
	// // System.out.println(classifier.classifyToString(sb.toString()));
	// String classified = sb.substring(3, sb.length() - "</P>".length());
	// classified = classifier.classifyWithInlineXML(classified).trim();
	//
	// boolean isClassified = false;
	// if (classified.contains(PERSON)) {
	// // System.out.println(classified);
	// doc.add(new TextField(PERSON, classified, Field.Store.YES));
	// isClassified = true;
	// }
	// if (classified.contains(ORGANIZATION)) {
	// // classified = classified.substring("<P>".length(),
	// // "</P>".length()).trim();
	// // System.out.println(classified);
	// doc.add(new TextField(ORGANIZATION, classified, Field.Store.YES));
	// isClassified = true;
	// }
	// if (classified.contains(LOCATION)) {
	// // classified = classified.substring("<P>".length(),
	// // "</P>".length()).trim();
	// // System.out.println(classified);
	// doc.add(new TextField(LOCATION, classified, Field.Store.YES));
	// isClassified = true;
	// }
	// return isClassified;
	// }

	/**
	 * Adds all files in the documentPath, and all its subdirectories to the
	 * allDocs collection.
	 * 
	 * @param documentPath
	 * @param allDocs
	 */

	static String DELIMETER = ".";

	private static void addFiles(File documentPath, Collection<File> allDocs) {
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
	// public static Document addDocInfo(Document doc){
	// doc.add(new Field("Title", ))
	//
	// return doc;
	// }
}