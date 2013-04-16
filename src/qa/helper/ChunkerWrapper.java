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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;

public class ChunkerWrapper {
	private static ChunkerWrapper instance;
	private final String[] stopPOSList = new String[] {"PP", "SBAR", "ADVP", "ADJP" ,"CONJP"};
	private final String[] stopWordList = new String[] {"I", "You", "We", "They", "He", "She", "It", "His", "Her", "Their", "Your", "Me", "Our", "Ours", "Theirs", "Hers", "Mine", "Him", "Me", "Yours", "Its", "Is", "Are", "Was", "Were", "Wikipedia", "the official Web site", "Who", "What", "When", "Where", "Why", "How", "s", "t", "do", "did", "done", "don", "Create a Facebook Profile", "Your World", "get", "got", "gets", "does", "a", "an", "that", "which", "there", "here", "result", "results"};


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

	public List<String> getChunks(String text) {
		String chunks = chunk(text);
		String stopPOSPattern = "\\[("+ApplicationHelper.join(stopPOSList, "|")+") (\\w+ )+\\]";
		String stopWordPattern = "(?i)\\[\\w+ ("+ApplicationHelper.join(stopWordList, "|")+") \\]";
		chunks = chunks.replaceAll(stopPOSPattern, "").replaceAll(stopWordPattern, "");
		List<String> chunkText = new ArrayList<String>();
		Pattern chunkPattern = Pattern.compile("\\[(\\w+ )+\\]",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m = chunkPattern.matcher(chunks);
		while (m.find()) {
			String chunk = m.group();
			chunk = chunk.replaceAll("\\[\\w+", "").replaceAll("\\]", "").trim();
			chunkText.add(chunk);
		}

		return chunkText;
	}

    public Set<String> getSortedChunks(String result) {
        Set<String> results = new HashSet<String>();
        List<String> chunks = getChunks(result);
        Map<String, Integer> chunkCount = new HashMap<String, Integer>();
        for (String chunk : chunks) {
            if (!chunkCount.containsKey(chunk)) {
                chunkCount.put(chunk, 0);
            }

            chunkCount.put(chunk, chunkCount.get(chunk) + 1);
        }

        chunkCount = sort(chunkCount);
        for (Map.Entry<String, Integer> entry : chunkCount.entrySet()) {
            if (entry.getValue() > 1) {
                results.add(entry.getKey());
            }
        }

        return results;
    }

	private List<String> getChunksForAnswer(String text) {
		String chunks = chunk(text);
		String stopPOSPattern = "\\[("+ApplicationHelper.join(stopPOSList, "|")+") (\\w+ )+\\]";
		String stopWordPattern = "(?i)\\[\\w+ ("+ApplicationHelper.join(stopWordList, "|")+") \\]";
		chunks = chunks.replaceAll(stopPOSPattern, "").replaceAll(stopWordPattern, "");
    	String[] posPOSList = new String[] {"NP"};
		List<String> chunkText = new ArrayList<String>();
		String answerPOSPattern = "\\[("+ApplicationHelper.join(posPOSList, "|")+") (\\w+ )+\\]";
		Pattern chunkPattern = Pattern.compile(answerPOSPattern,
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m = chunkPattern.matcher(chunks);
		while (m.find()) {
			String chunk = m.group();
			chunk = chunk.replaceAll("\\[\\w+", "").replaceAll("\\]", "").trim();
			chunkText.add(chunk);
		}

		return chunkText;
	}

    public Set<String> getSortedChunksForAnswer(String result) {
        Set<String> results = new HashSet<String>();
        List<String> chunks = getChunksForAnswer(result);
        Map<String, Integer> chunkCount = new HashMap<String, Integer>();
        for (String chunk : chunks) {
            if (!chunkCount.containsKey(chunk)) {
                chunkCount.put(chunk, 0);
            }

            chunkCount.put(chunk, chunkCount.get(chunk) + 1);
        }
        chunkCount = sort(chunkCount);
        for (Map.Entry<String, Integer> entry : chunkCount.entrySet()) {
            results.add(entry.getKey());
        }

        return results;
    }

    private Map<String, Integer> sort(Map<String, Integer> input) {
        ValueComparator bvc =  new ValueComparator(input);
        TreeMap<String,Integer> sortedMap = new TreeMap<String,Integer>(bvc);
        sortedMap.putAll(input);
        return sortedMap;
    }

    class ValueComparator implements Comparator<String> {

        Map<String, Integer> base;
        public ValueComparator(Map<String, Integer> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.    
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }
}