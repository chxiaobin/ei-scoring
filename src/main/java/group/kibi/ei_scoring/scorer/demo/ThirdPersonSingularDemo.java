package group.kibi.ei_scoring.scorer.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;

public class ThirdPersonSingularDemo {

	//the target corrected prompt
	private String target; 

	//the targeted verb
	private String targetLemma; 
	private String targetLemmaForm;

	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		ThirdPersonSingularDemo scorer = 
				new ThirdPersonSingularDemo("Everyone loves to read comic books as a child.",
				"love", "loves");

		String filePath = "/Users/Xiaoyi/Desktop/sla/automated_scoring/3rd_person/3rd_person_id.tsv";
	    String outputPath = "/Users/Xiaoyi/Desktop/sla/automated_scoring/3rd_person/scores_3rd_person201.tsv";

	    List<String> sentences = new ArrayList<>();
	    List<String> results = new ArrayList<>();

	    int columnIndex = 1;

	    try (BufferedReader br = new BufferedReader(new FileReader(filePath));
	         BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
	        String line;

	        while ((line = br.readLine()) != null) {
	            String[] columns = line.split("\t");
	            if (columns.length > columnIndex) {
	                sentences.add(columns[columnIndex]);
	                int testScore = scorer.getScore(columns[columnIndex]);
	                // Assuming you want to keep the original data and add a new column
	                String resultLine = line + "\t" + testScore;
	                results.add(resultLine);
	            }
	        }

	        // Writing results to the new file
	        for (String result : results) {
	            bw.write(result);
	            bw.newLine();
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	
	public ThirdPersonSingularDemo(String target, String targetLemma, String targetLemmaForm) {
		this.target = target;
		this.targetLemma = targetLemma;
		this.targetLemmaForm = targetLemmaForm;
	}

	/**
	 * For scoring a response to a prompt targeting third-person singular -s
	 * 
	 * @param response  the student's response to the prompt
	 * @return
	 */
	public int getScore(String response) {
		// score 2: no error. No need to do NLP processing.
		logger.info("Checking score 1...");
		if (target.equals(response.trim())) {
			logger.info("Response matches target. Give score of 1");
			return 1;
		}

		// Use the corenlp simple API, which is the fastest way to do NLP
		// processing, but without a lot of customization. It meets our scoring
		// task requirements though.
		logger.info("Processing response with Corenlp...");
		Sentence sent = new Sentence(response);

		// get all the tokens and lemmas, POS tags. Corenlp use the Penntreebank
		// tag set.
		List<String> lemmas = sent.lemmas();
		List<Token> tokens = sent.tokens();
		List<String> posTags = sent.posTags();

		// score 2: lemma correct but contains spelling error or not in the right form
//		logger.info("Checking score 1...");
//		for (int i = 0; i < lemmas.size(); i++) {
//			String lemma = lemmas.get(i);
//			if (lemma.equals(targetLemma) && lemma.endsWith("s") && 
//					!tokens.get(i).equals(targetLemmaForm)) { 
//				logger.info("Found target lemma at position {}, correct ending, but its form does not equal target form.", i);
//				return 1;
//			}
//		}

		// Please update the comments below:
		// score 1: used the target lemma, but the token not in the target form
		logger.info("Checking score 1...");
		for (int i = 0; i < lemmas.size(); i++) {
			String lemma = lemmas.get(i);
			if (lemma.equals(targetLemma) && !tokens.get(i).equals(targetLemmaForm)) { 
				logger.info("Found target lemma at position {}, but its form does not equal target form.", i);
				return 1;
			}
		}

		// socre 0: not target lemma
		logger.info("Checking score 0...");
		if (!lemmas.contains(targetLemma)) {
			logger.info("Did not find target lemma, returning score 0...");
			return 0;
		}

		// all other possibilities
		return 0;
	}

}
