package qa.indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

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

public class LuceneIndexer implements DocumentIndexer {
	private IndexWriter iw;
	private StandardAnalyzer sa;
	private Directory dir;
	private final String DELIMETER = ".";

	public LuceneIndexer() {
		sa = new StandardAnalyzer(Version.LUCENE_41);
		try {
			dir = new MMapDirectory(new File(Settings.get("INDEX_PATH")));
		} catch (IOException e) {
			System.err.println("Unable to init indexed directory");
		}
	}

	@Override
	public boolean hasIndexData(String indexPath) {
		File file = new File(indexPath);
		if(file.isDirectory()){
			if(file.list().length > 0) {
				return true;
			}
		}

		return false;
	}

	@Override
	/**
	 * @param documentPath
	 *            The location of the AQUAINT corpus folder
	 */
	public void indexDocuments(String documentPath) {

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
	private void parseFile(File file) throws IOException {
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
	// public static Document addDocInfo(Document doc){
	// doc.add(new Field("Title", ))
	//
	// return doc;
	// }
}