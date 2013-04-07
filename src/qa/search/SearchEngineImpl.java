package qa.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qa.Settings;
import qa.helper.ApplicationHelper;
import qa.model.QuestionInfo;
import qa.model.QueryTerm;
import qa.model.enumerator.QuerySubType;

public class SearchEngineImpl implements SearchEngine {
	
	private static String WEBSEARCH_PARSE_PATTERN_BEG = "\"snippet\": \"";
	private static String WEBSEARCH_PARSE_PATTERN_END = "\",";
	private static String STOP_WORDS_FILE_ERROR = "Stop Words file not found/IO Error";
	
	private class Tuple {
		String term;
		int key;
		
		public Tuple(String s, int k){
			term = s;
			key = k;
		}
	}

	@Override
	public String search(QuestionInfo question) {

		ArrayList<String> parsedSnippets;
		ArrayList<String> taggedNERSnippets;
		ArrayList<String> textSnippets;
		ArrayList<String> prunedSnippets;
		HashMap<String, Integer> term_Freq_Map;

		String queryTerms = GetQueryTerms(question);	

		if(queryTerms == ""){
			return null;
		}

		//Search the web for results
		ArrayList<String> response = SearchWeb(queryTerms);

		//Parse the response to collect useful data
		parsedSnippets = Parser(response, WEBSEARCH_PARSE_PATTERN_BEG, WEBSEARCH_PARSE_PATTERN_END);

		//Remove special characters
		textSnippets = Extract(parsedSnippets);

		//ANSWER EXTRACTION STEPS
		//1. Remove Stop words from textSnippets
		prunedSnippets = RemoveStopWords(textSnippets);

		//2. N-gram mining and ngram-freq
		term_Freq_Map = NGramTiling(prunedSnippets);
		//term_Freq_Map = BoostFrequencyScoring(term_Freq_Map);

		//3. Make it log freq
		term_Freq_Map = LogarithmizeScore(term_Freq_Map);

		//4. NER on textSnippets
		//candidateTerms = ClassifySnippets(textSnippets, question);
		taggedNERSnippets = ClassifySnippets(parsedSnippets, question);

		//5. Extract those of Q-type and add to scoring (do novelty scoring)
		String qType = GetQuestionType(question);		
		term_Freq_Map = ImproveScoring_MatchQuestionType(term_Freq_Map, taggedNERSnippets, qType);
	//	System.out.println("Exit freq score");
		term_Freq_Map = ImproveScoring_DocumentRank(term_Freq_Map, prunedSnippets);
	//	System.out.println("Exit");
		//6. Ranking and return top 5;
		String answer = RankAnwers(term_Freq_Map);

		return answer;
	}


	private HashMap<String, Integer> ImproveScoring_DocumentRank(
			HashMap<String, Integer> term_Freq_Map,
			ArrayList<String> prunedSnippets) {
		int freq = 0;
	//	System.out.println("enter");
		//TODO: get only uni-gram for this, for now all the n-grams are taken
		Set<String> tileSet = term_Freq_Map.keySet();
		int rank = 0;
		
		for( String snip: prunedSnippets){		
			++rank;
			for( String tile : tileSet) {				
				if( snip.contains(tile)){
					freq = term_Freq_Map.get(tile);
					freq = freq*((int)Math.log10(20.0/rank));
					term_Freq_Map.put(tile,  freq);
				}
			}
		}
		
		return term_Freq_Map;
	}

	private String GetQuestionType(QuestionInfo question) {
		String type = "";
		
		switch(question.getQueryType()){
			case ABBR: type = "ORGANIZATION";break;
			case DESC: type = "NIL";break;
			case HUM : type = "PERSON";break;
			case LOC : type = "LOCATION";break;
			case ENTY: type = "NIL";break;
			case NUM : 
				switch(question.getQuerySubType()){
					case NUM_date: type = "DATE";break;
					case NUM_perc: type = "PERCENT";break;
					case NUM_money: type = "MONEY";break;
					case NUM_period: type = "TIME";break;
					default: type = "MONEY";
				}
			break;
			default: type = "";
		}

		return type;
	}


	private String RankAnwers(
			HashMap<String, Integer> term_Freq_Map) {
		
		String ans = "";
		Set<String> set_of_ngrams = term_Freq_Map.keySet();
		
		List<Tuple> list = new ArrayList<Tuple>();
		
		for(String temp:set_of_ngrams){
			Tuple t = new Tuple(temp, term_Freq_Map.get(temp));
			list.add(t);
		}
		
		Collections.sort(list, new Comparator<Tuple>() {			
			public int compare(Tuple arg0, Tuple arg1) {
				return arg1.key - arg0.key;
			}
		});
		

		int count = 8;
		for(Tuple t:list){
			if(count-- >= 0){
				ans = ans + t.term + " ";
			}
			else {
				break;
			}
			//System.out.println(t.term + ":" + t.key);
		}
		
		String finalAns = RemoveDuplicates(ans);		
		ApplicationHelper.printDebug("Query Terms: " + finalAns + "\n");
		
		return finalAns;
	}


	private HashMap<String, Integer> ImproveScoring_MatchQuestionType(
			HashMap<String, Integer> term_Freq_Map,
			ArrayList<String> taggedNERSnippets, String questionType) {
		//System.out.println("Enter freq score");
		ArrayList<String> candidateAnsList;	
		String startTag = "<" + questionType + ">";
		String endTag = "</" + questionType + ">";
		candidateAnsList = Parser(taggedNERSnippets, startTag, endTag);
		
		int freq = 0, threshold = 47;
		String big = "", small = "";
		
		Set<String> tileSet = term_Freq_Map.keySet();
		for( String candidateAns : candidateAnsList){
			candidateAns = candidateAns.toLowerCase();
		//	System.out.println("looping: " + temp);
			for(String item : tileSet){
				
				if(candidateAns.length() > item.length()){
					big = candidateAns;
					small = item;
				}
				else {
					big = item;
					small = candidateAns;
				}
				
				if(big.contains(small) == true){
					freq = term_Freq_Map.get(candidateAns);
					freq = freq + threshold;
					term_Freq_Map.put(candidateAns,freq);
				}
				
			}
		}
		
	//	System.out.println("finish loop");
		return term_Freq_Map;
	}


	private HashMap<String, Integer> LogarithmizeScore(
			HashMap<String, Integer> term_Freq_Map) {

		int raw_freq;
		Set<String> set_of_ngrams = term_Freq_Map.keySet();
		for( String temp : set_of_ngrams){
			raw_freq = term_Freq_Map.get(temp);
			term_Freq_Map.put(temp,(int)(Math.log10(1+raw_freq)*100));
		}

		/*
		for( String temp : set_of_ngrams){
			System.out.println(temp + " : " + term_Freq_Map.get(temp));		
		}
		*/
		
		return term_Freq_Map;
	}


	//Forming a single string out of the question terms
	private String GetQueryTerms(QuestionInfo question) {		
		String concatTerms = "When was TREC found " + " wiki";
		
		if(question == null) {
			return concatTerms;
		}
	
		List<QueryTerm> termList = question.getQuestionTerms();
		
		concatTerms = "";
		for(QueryTerm term : termList){
			concatTerms += (term.getText() + " ");
		}		
		
		//concatTerms = concatTerms + MapQueryType(question);
		concatTerms = MapSubQueryType(question) + " " + concatTerms + " wiki";
		//concatTerms = RemoveDuplicates(concatTerms);
		
		ApplicationHelper.printDebug("Web Query Terms: " + concatTerms + "\n");

		return concatTerms;
	}
	
	//Map the question type to add to query for better results
	private String MapQueryType(QuestionInfo q){
		String type = "";
		
		switch(q.getQueryType()){
			case ABBR:type = "abbreviation";break;
			case DESC:type = "";break;
			case ENTY:type = "";break;
			case HUM :type = "person";break;
			case LOC :type = "location";break;
			case NUM :type = "";break;
			default: type = "";
		}
		
		return type;
	}
	
	private String MapSubQueryType(QuestionInfo q){
		MappingHelper Mapper = new MappingHelper();
		HashMap<QuerySubType, String> map = Mapper.getMap();		
		return map.get(q.getQuerySubType());
	}
	
	private String RemoveDuplicates(String line){
		String noDuplicates = "";
		
		HashSet<String> set = new HashSet<String>();
		String[] split = line.split(" ");
		for(String temp:split){
			temp = temp.trim();
			set.add(temp);
		}
		
		for(String temp:set){
			noDuplicates = noDuplicates + temp + " ";
		}
		
		return noDuplicates;
	}
	
	//Searching the web for results
	private ArrayList<String> SearchWeb(String queryTerms) {

		String url = "https://www.googleapis.com/customsearch/v1?"; 
		String apiKey = Settings.get("GOOGLE_API_KEY"); 
		String csEngineId = Settings.get("GOOGLE_ENGINE_ID"); 
			
		String encodedQueryString = "", request = "", queryResponse = "";		
		InputStream response = null;
		Scanner sc;
		int count = 1;

		try {			
			encodedQueryString = String.format("%s", URLEncoder.encode(queryTerms,"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		String temp_request = url + "key=" + apiKey + "&cx=" + csEngineId + 
				"&q=" + encodedQueryString + "&num=10";

		while(count < 2) {

			//change result start index for each request
			request = temp_request + "&start=" + ((count -1)*10 + 1);			
			count++;

			try {
				URLConnection searchConn = new URL(request).openConnection();
				response = searchConn.getInputStream();		
				sc = new Scanner(response);

				while(sc.hasNext()){
					queryResponse += sc.nextLine() + "\n";
				};
			} catch (IOException e) {
				e.printStackTrace();
			}				
		}
		
		String[] list = queryResponse.split("kind");
		ArrayList<String> responseList = new ArrayList<String>();
		
		for(String item: list){
			responseList.add(item);
		}

		return responseList;
	}
	
	private ArrayList<String> Parser(ArrayList<String>list_to_parse, String pattern_beg, 
			String pattern_end){

		ArrayList<String> parsed_list = new ArrayList<String>();
		int beg, end, length_pattern_beg;
		length_pattern_beg = pattern_beg.length();

		for(String item : list_to_parse){

			while( item.length() > 0) {
				beg = -1; end = -1;
				beg = item.indexOf(pattern_beg);
				if(beg == -1) {
					break;
				}
				end = item.indexOf(pattern_end, beg);
				if(beg != -1 && end != -1) {
					parsed_list.add(item.substring(beg + length_pattern_beg, end));
					item = item.substring(end);
				}
				else {
					break;
				}
			}
		}
		return parsed_list;
	}

	//Extracts the words only from web snippet such that
	//special characters are removed
	private ArrayList<String> Extract(ArrayList<String> parsedSnippets) {
		ArrayList<String> modifiedList = new ArrayList<String>();
		String matched = "", temp = "";
		Pattern pattern = Pattern.compile("\\w+",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m;
		for( String list_item: parsedSnippets){
			temp = "";
			m = pattern.matcher(list_item);
			
			while (m.find()) {
				matched = m.group();
				temp = temp + matched + " ";
			}
			modifiedList.add(temp);
		}
		return modifiedList;
	}

	//Function to remove the stop words from web snippets before ranking is done
	//so that stop words aren't ranked.
	private ArrayList<String> RemoveStopWords(ArrayList<String> textSnippets) {
		String regex = "";

		ArrayList<String> rem;
		String path = System.getProperty("user.dir");
		File f = new File(path, Settings.get("STOPWORD_LIST_PATH_SEARCH"));
		/*
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = br.readLine();
			
			while(line!=null){
				regex = regex + " " + line + " |";
				line = br.readLine();
			}
			br.close();			
			regex = regex.substring(0, regex.length()-1);
			
		} catch (FileNotFoundException e) {
			ApplicationHelper.printError(STOP_WORDS_FILE_ERROR);			
		} catch (IOException e) {
			ApplicationHelper.printError(STOP_WORDS_FILE_ERROR);
		}
		
		rem = new ArrayList<String>();
		for(String item: textSnippets){
			item = " " + item + " ";
			item = item.replaceAll(regex, " ").trim();
			rem.add(item);
			System.out.println(item);
		}		
		return rem;	
		*/
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = "";
			line = br.readLine();
			
			while(line!=null){
				regex = " "+line+" ";
				rem = new ArrayList<String>();
				for( String temp: textSnippets){
					temp = " " + temp + " ";
					temp = temp.replaceAll(regex, " ");
					rem.add(temp.trim());
				}	
				textSnippets = rem;
				line = br.readLine();
			}
			br.close();
		
		} catch (FileNotFoundException e) {
			ApplicationHelper.printError(STOP_WORDS_FILE_ERROR);
		} catch (IOException e) {
			ApplicationHelper.printError(STOP_WORDS_FILE_ERROR);
		}
		
		/*
		for(String item:textSnippets){
			System.out.println(item);
		}
		*/
		return textSnippets;
	}
	
	
	private HashMap<String, Integer> NGramTiling(ArrayList<String> parsedSnippets) {
		HashMap<String, Integer> term_Freq_Map = new HashMap<String, Integer>();

		String split[] = null;
		String complete = "";

		//split into individual words
		for(String list_item: parsedSnippets){
			complete += list_item;
		}

		split = complete.split(" ");

		if(split == null){
			return null;
		}

		String key = "";
		//uni-gram tiling and frequency
		for(int i = 0; i < split.length; i++) {	
			key = split[i];
			key = key.trim();
			key = key.toLowerCase(Locale.ENGLISH);
			//System.out.println(key);
			if(!term_Freq_Map.containsKey(key)){
				term_Freq_Map.put(key, 1);
			}
			else {
				Integer freq = term_Freq_Map.get(key);
				freq = freq + 1;
				term_Freq_Map.put(key, freq);

			}
		}

		//bi-gram tiling and frequency
		for( int i = 0 ; i < split.length - 1; i++){
			key = split[i] + " " + split[i+1];
			key = key.trim();
			key = key.toLowerCase(Locale.ENGLISH);
			//System.out.println(key);
			if(!term_Freq_Map.containsKey(key)){
				term_Freq_Map.put(key, 1);
			}
			else {
				Integer freq = term_Freq_Map.get(key);
				freq = freq + 1;
				term_Freq_Map.put(key, freq);

			}
		}

		//tri-gram tiling and frequency
		for( int i = 0 ; i < split.length - 2; i++){
			key = split[i] + " " + split[i+1] + " " + split[i+2];
			//System.out.println(key);
			key = key.trim();
			key = key.toLowerCase(Locale.ENGLISH);
			if(!term_Freq_Map.containsKey(key)){
				term_Freq_Map.put(key, 1);
			}
			else {
				Integer freq = term_Freq_Map.get(key);
				freq = freq + 1;
				term_Freq_Map.put(key, freq);

			}
		}

		return term_Freq_Map;
	}

	private ArrayList<String> ClassifySnippets(
			ArrayList<String> parsedSnippets, QuestionInfo q) {

		ArrayList<String> annotatedSnippets = new ArrayList<String>();

		Classifier c = new Classifier();
		annotatedSnippets = c.Classify(parsedSnippets);

		/*
		for(String t : annotatedSnippets){
					System.out.println(t);
		}
		*/
		return annotatedSnippets;
	}
}
