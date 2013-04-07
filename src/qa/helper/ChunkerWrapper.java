package qa.helper;

import LBJ2.nlp.seg.SegmentTagPlain;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

public class ChunkerWrapper {
	private static ChunkerWrapper instance;

	private ChunkerWrapper() {
	}

	public static ChunkerWrapper getInstance() {
		if (instance == null) {
			instance = new ChunkerWrapper();
		}

		return instance;
	}

	public String chunk(String text) {
		writeToFile(text);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		SegmentTagPlain.main(new String[] {
				"edu.illinois.cs.cogcomp.lbj.chunk.Chunker", "input.txt" });
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

		File file = new File("input.txt");
		if (file.exists()) {
			file.delete();
		}

		return baos.toString();
	}

	private void writeToFile(String text) {
		try {
			File file = new File("input.txt");
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(text);
			bw.close();
		} catch (IOException e) {

		}
	}
}