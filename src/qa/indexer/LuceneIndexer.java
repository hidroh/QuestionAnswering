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
import qa.helper.ApplicationHelper;

public class LuceneIndexer implements DocumentIndexer {
	private IndexWriter iw;
	private StandardAnalyzer sa;
	private Directory dir;

	public LuceneIndexer() {
		sa = new StandardAnalyzer(Version.LUCENE_41);
		try {
			dir = new MMapDirectory(new File(Settings.get("INDEX_PATH")));
		} catch (IOException e) {
			ApplicationHelper.printError("Unable to init indexed directory", e);
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
	public void indexDocuments(String documentPath) throws Exception {
		File documentDir = new File(documentPath);
		if (!(documentDir.exists() && documentDir.isDirectory())) {
			throw new Exception("Document Indexer: Unable to find document directory.");
		}

		Collection<File> allDocs = new ArrayList<File>();
		addFiles(documentDir, allDocs);
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_41, sa);

		try {
			iw = new IndexWriter(dir, config);
			for (File file : allDocs) {
				indexFile(file);
			}
			iw.close();
		} catch (IOException e) {
			ApplicationHelper.printError("Document Indexer: Unable to locate raw documents", e);
		}
	}

	private void indexFile(File file) throws IOException {
		if (file.getName().startsWith(".")) {
			return;
		}
		
		ApplicationHelper.printDebug(String.format("Indexing %s\n", file.getName()));
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
			} else if (nextLine.contains("<TEXT>") && docno != null) { 
				StringBuilder sb = new StringBuilder();
				do {
					nextLine = scanner.nextLine();
					sb.append(nextLine);
				} while (!nextLine.contains("</TEXT>"));

				doc = new Document();
				doc.add(new TextField("TEXT", sb.toString().replace("</TEXT>", "").replace("<P>", "").replace("</P>", "").trim(), Field.Store.YES));
				doc.add(new TextField("DOCNO", docno, Field.Store.YES));
				doc.add(new StringField("FILENAME", file.getName(), Field.Store.YES));

				docs.add(doc);
			} else {
				// Do nothing
			}
		}

		scanner.close();

	}

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
				if (child.isFile()) {
					allDocs.add(child);
				} else {
					addFiles(child, allDocs);
				}
			}
		}
	}
}