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

public class BePassiveDemo {

	private String target; 
	private String targetLemma; 
	private String targetBeForm;

	// third person singular s
	static String sents1 = "Everyone love to read comic books as a child";
	static String sents2 = "A good teacher make learning a joy for students.";
	static String sents3 = "Technology plays an important role in language learning nowadays.";
	static String sents4 = "Regular exercise helps people maintain a normal weight.";

	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {

	    BePassiveDemo scorer = new BePassiveDemo("Children should not be allowed to stay out late with their friends", "allow", "be");

	    String filePath = "/Users/Xiaoyi/Desktop/sla/automated_scoring/be_passive/be_passive210_id.tsv";
	    String outputPath = "/Users/Xiaoyi/Desktop/sla/automated_scoring/be_passive/scores_be_passive210.tsv";

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
	
	public BePassiveDemo(String target, String targetLemma, String targetBeForm) {
		this.target = target;
		this.targetLemma = targetLemma;
		this.targetBeForm = targetBeForm;
	}

	/**
	 * For scoring a response to a prompt targeting be-passive.
	 * 
	 * @param response     the student's response to the prompt
	 * @param target       the target corrected prompt
	 * @param targetLemma  the targeted verb
	 * @param targetBeForm the 'be' form in the target
	 * @return
	 */
	public int getScore(String response) {
		// score 1: no error. No need to do NLP processing.
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
		
		// score 1: be + verb PP, but stem of the verb pp is not correct (e.g.
		// spelling mistakes)
		// iterate all the lemmas, try to find 'be'
		logger.info("Checking score 1...");
		for (int i = 0; i < lemmas.size(); i++) {
			String lemma = lemmas.get(i);
			if (lemma.equals("be")) { // found 'be'
				logger.info("Found lemma 'be' at position {}", i);
				// get the next token, see if it is verb PP, which is annotated as VBN (see PTB
				// tagset). Limitation: this only takes into account the
				// 'immediate' next token. So it does not account for the
				// situation when there are additional words between the 'be'
				// and the PP verb, e.g., 'Children are not supposed to go out at night.'
				// To account for such situations, more advanced rules need to
				// be implemented. For example, using Tregex or Tree Regular
				// Expressions will make it possible to account for these kinds
				// of the more complex situations.
				if (i <= lemmas.size() - 1) { // avoid out of range
					int nextIdx = i + 1;
					if (posTags.get(nextIdx).equals("VBN")) {
						logger.info("Next token is also VBN, returning score 1.");
						return 1;
					}
				}

			}
		}
		
		
		// score 0: use the stem in PP form, but no "be" or "be" in wrong form
		logger.info("Checking score 1...");
		for (int i = 0; i < lemmas.size(); i++) {
			String lemma = lemmas.get(i);
			if (lemma.equals(targetLemma) && "VBN".equals(posTags.get(i))) {
				logger.info("Found target lemma in VBN form at position {}", i);
				if (i == 0) { // if the stem is the first word, there is no other words (expecting 'be').
					return 0;
				}

				int prevIdx = i - 1;
				logger.info(tokens.get(prevIdx).word());
				if (tokens.get(prevIdx).word() != targetBeForm) {
					logger.info("Previous token not target 'be' form, returning score 0...");
					return 0;
				}
			}

			if (lemma.equals(targetLemma) && posTags.get(i) != "VBN" && lemmas.get(i - 1).equals("be")) {
				logger.info("Found be + taget lemma but not in VBN. Returing score 0...");
				return 0;
			}
		}

		// score 0: has the target stem, but not in PP and no 'be'
		logger.info("Checking score 0...");
		if (lemmas.contains(targetLemma)) {
			logger.info("Found target lemma, returing score 0...");
			return 0;
		}

		// all other possibilities
		return 0;
	}

}
