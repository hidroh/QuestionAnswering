package qa.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

import qa.model.AnswerInfo;
import qa.model.QuestionInfo;
import qa.model.QueryTerm;;

public class SearchEngineImpl implements SearchEngine {

	
	//HashMap score_Map;

	@Override
	public List<AnswerInfo> search(QuestionInfo question) {

		ArrayList<String> parsedSnippets;
		ArrayList<String> candidateTerms;
		ArrayList<AnswerInfo> answerTerms;
		ArrayList<String> textSnippets;
		HashMap<String, Integer> term_Freq_Map;

		String queryTerms = GetQueryTerms(question);	

		if(queryTerms == ""){
			return null;
		}

		//Search the web for results
		String response = SearchWeb(queryTerms);

		//Parse the response to collect useful data
		parsedSnippets = Parser(response, "web_search");

		//Remove special characters
		textSnippets = Normalizer(parsedSnippets);
		
		//ANSWER EXTRACTION STEPS
		//1. Remove Stop words from textSnippets
		textSnippets = RemoveStopWords(textSnippets);
		
		//2. N-gram mining and ngram-freq
		term_Freq_Map = NGramTiling(textSnippets);
		
		//3. Make it log freq
		term_Freq_Map = LogarithmizeScore(term_Freq_Map);
		
		//4. NER on textSnippets
		candidateTerms = ClassifySnippets(textSnippets, question);
		
		//5. Extract those of Q-type and add to scoring (do novelty scoring)
		term_Freq_Map = ImproveScoring(term_Freq_Map, candidateTerms);
		
		//6. Ranking and return top 5;
		answerTerms = RankAnwers(term_Freq_Map);

		return answerTerms;
		
		//N-gram tiling of results
		//nGramTiling(parsedSnippets);
		
		//Remove stop words from String before nGramTiling

		//Perform NER on parsed data
		//classifiedTerms = ClassifySnippets(parsedSnippets, question);

		//Match with Appropriate Question Type
		//??

		//Populate the Answer List
		//answerTerms = PopulateAnswerTerms();

		//return answerTerms;
	}


	private ArrayList<AnswerInfo> RankAnwers(
			HashMap<String, Integer> term_Freq_Map) {
		
		//Integer count = 0;
		Set<String> set_of_ngrams = term_Freq_Map.keySet();
		for( String temp : set_of_ngrams){
			if(term_Freq_Map.get(temp) != 0){
				System.out.println(temp + ":" + term_Freq_Map.get(temp));
			}
		}
			
		return null;
	}


	private HashMap<String, Integer> ImproveScoring(
			HashMap<String, Integer> term_Freq_Map,
			ArrayList<String> candidateTerms) {
		
		ArrayList<String> candidateAns;		
		candidateAns = Parser(candidateTerms, "<PERSON>", "</PERSON>");
		
		for( String temp : candidateAns){
			//System.out.println(temp);
			temp = temp.toLowerCase();
			if(term_Freq_Map.get(temp) != null){
				int raw_freq = term_Freq_Map.get(temp);
				term_Freq_Map.put(temp,raw_freq + 10);
			}
			
		}
				
		return term_Freq_Map;
	}


	private HashMap<String, Integer> LogarithmizeScore(
			HashMap<String, Integer> term_Freq_Map) {
		
		Integer raw_freq = 0;
		Set<String> set_of_ngrams = term_Freq_Map.keySet();
		for( String temp : set_of_ngrams){
			raw_freq = term_Freq_Map.get(temp);
			term_Freq_Map.put(temp,(int)(Math.log10((Integer)raw_freq)*100));
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
		String concatTerms = "first woman killed Vietnam War" + " wiki";
		if(question == null) {
			return concatTerms;
		}
		List<QueryTerm> termList = question.getQuestionTerms();

		for(QueryTerm term : termList){
			concatTerms += (term.getText() + " ");
		}		
		return concatTerms;
	}

	//Searching the web for results
	private String SearchWeb(String queryTerms) {
		
	//	/*
		String url = "https://www.googleapis.com/customsearch/v1?"; 
		String apiKey = "AIzaSyD2lOtpqFCu3_0sjAKQLmgJlC278TJAyPA"; 
		String csEngineId = "005140197492265105536:wir43wq3mru"; 
	//	*/
		
		/*
		String url = "https://www.googleapis.com/customsearch/v1?";
		String apiKey = "AIzaSyBbJR-3TekdQrgFO1WRKUTra_NSzMl_DOE";
		String csEngineId = "003683717859680101160:n33_ckvstos";
		*/
		
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
		return queryResponse;
	}

	//Function to get the required information depending on what is parsed-parse_type
	private ArrayList<String> Parser(String response, String parse_type) {
		
		String[] list;
		ArrayList<String> list_to_parse = new ArrayList<String>();
		String pattern_beg = "", pattern_end = "";

		if(parse_type.compareTo("web_search") == 0){
			list = response.split("kind");
			pattern_beg = "\"snippet\": \"";
			pattern_end = "\"," ;
		}
		else {
			list = response.split(parse_type);
			//put in pattern type
		}
		
		for(String item: list){
			list_to_parse.add(item);
		}

		return Parser(list_to_parse, pattern_beg, pattern_end);
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
					//System.out.println(item);
					parsed_list.add(item.substring(beg + length_pattern_beg, end));
					item = item.substring(end);
				}
				else {
					break;
				}
			}
		}
		
		/*
		for(String item: parsed_list){
			System.out.println(item + "\n");
		}
		*/
		
		return parsed_list;
	}

	//Normalizes the text
	private ArrayList<String> Normalizer(ArrayList<String> parsedSnippets) {
		ArrayList<String> modifiedList = new ArrayList<String>();

		for( String list_item: parsedSnippets){
			modifiedList.add(list_item.replaceAll("[:()\",\';.\\-]", ""));
		}
		
		/*
		for(String item:modifiedList){
			System.out.println(item);
		}
		*/
		
		return modifiedList;
	}


	private ArrayList<String> RemoveStopWords(ArrayList<String> textSnippets) {

		String regex = "the | a | on | of | by | who | or | to | he | she | in | was | and | for | is | her | they | him ";
		ArrayList<String> rem = new ArrayList<String>();
		for( String temp: textSnippets){
			temp = temp.replaceAll(regex, "");
			rem.add(temp);
		}
		
		return rem;
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

	private ArrayList<AnswerInfo> PopulateAnswerTerms() {
		// TODO Auto-generated method stub
		return null;
	}

	private ArrayList<String> ClassifySnippets(
			ArrayList<String> parsedSnippets, QuestionInfo q) {

		ArrayList<String> annotatedSnippets = new ArrayList<String>();

		Classifier c = new Classifier();
		annotatedSnippets = c.Classify(parsedSnippets);
		
		
		for(String t : annotatedSnippets){
		//		System.out.println(t);
		}
		
		
		/*
		switch(q.getQueryType()){
			case LOC: break;
			case HUM: break;
			default: break;
		}
		 */
		return annotatedSnippets;
	}
}


		// Pattern pattern = Pattern.compile("\\w+",
		// 		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		// Matcher m = pattern.matcher('text');
		// while (m.find()) {
		// 	String matched = m.group();
		// }