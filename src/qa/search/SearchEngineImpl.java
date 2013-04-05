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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qa.Settings;
import qa.model.QuestionInfo;
import qa.model.QueryTerm;;

public class SearchEngineImpl implements SearchEngine {

	private class Tuple {
		String term;
		int key;
		
		public Tuple(String s, int k){
			term = s;
			key = k;
		}
	}
	//HashMap score_Map;

	@Override
	public String search(QuestionInfo question) {

		ArrayList<String> parsedSnippets;
		ArrayList<String> candidateTerms;
		ArrayList<String> textSnippets;
		ArrayList<String> prunedSnippets;
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
		textSnippets = Extract(parsedSnippets);
		
		//ANSWER EXTRACTION STEPS
		//1. Remove Stop words from textSnippets
		prunedSnippets = RemoveStopWords(textSnippets);

		//2. N-gram mining and ngram-freq
		term_Freq_Map = NGramTiling(prunedSnippets);

		//3. Make it log freq
		term_Freq_Map = LogarithmizeScore(term_Freq_Map);

		//4. NER on textSnippets
		candidateTerms = ClassifySnippets(textSnippets, question);
		//candidateTerms = ClassifySnippets(parsedSnippets, question);

		//5. Extract those of Q-type and add to scoring (do novelty scoring)
		term_Freq_Map = ImproveScoring(term_Freq_Map, candidateTerms, "PERSON");

		//6. Ranking and return top 5;
		String answer = RankAnwers(term_Freq_Map);

		return answer;
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
		

		int count = 5;
		for(Tuple t:list){
			if(count-- >= 0){
				ans = ans + t.term + " ";
			}
			else {
				break;
			}
		//	System.out.println(t.term + ":" + t.key);
		}
		
		HashSet<String> set = new HashSet<String>();
		String[] split = ans.split(" ");
		for(String temp:split){
			temp = temp.trim();
			set.add(temp);
		}
		
		String finalAns = "";
		for(String temp:set){
			finalAns = finalAns + temp + " ";
		}

		System.out.println(finalAns);
		return finalAns;
	}


	private HashMap<String, Integer> ImproveScoring(
			HashMap<String, Integer> term_Freq_Map,
			ArrayList<String> candidateTerms, String questionType) {

		ArrayList<String> candidateAns;	
		String startTag = "<" + questionType + ">";
		String endTag = "</" + questionType + ">";
		candidateAns = Parser(candidateTerms, startTag, endTag);
		int freq = 0, threshold = 47;;
		
		for( String temp : candidateAns){
			temp = temp.toLowerCase();
			if(term_Freq_Map.get(temp) != null){
				freq = term_Freq_Map.get(temp);
				freq = freq+threshold;
				term_Freq_Map.put(temp,freq);
				//System.out.println(temp + ":" +freq );
			}
		}
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
		String apiKey = Settings.get("GOOGLE_API_KEY"); 
		String csEngineId = Settings.get("GOOGLE_ENGINE_ID"); 
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

	//Extracts the text
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


	private ArrayList<String> RemoveStopWords(ArrayList<String> textSnippets) {
		String regex = "";

		ArrayList<String> rem;
		String path = System.getProperty("user.dir");
		File f = new File(path, Settings.get("STOPWORD_LIST_PATH_SEARCH"));
		
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
			
			e.printStackTrace();
		} catch (IOException e) {
		
			e.printStackTrace();
		}

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
