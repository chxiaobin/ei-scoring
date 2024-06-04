package group.kibi.ei_scoring.scorer.demo;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;

/**
 * This is for demonstrating scoring 'be-passive' responses.
 */
public class BePassiveDemo {

	//The target sentence, which is the sentence the test taker hears.
	private String target; 
	
	//The target word's lemma. In this case, it is the main verb in the
	//be-passive structure. The algorithm will find out whether the response
	//contains the target word (lemma). 
	private String targetLemma; 
	
	//The target conjugated form of 'be'. The algorithm will see if the test
	//taker used 'be' in the target form, i.e., if 'be' is conjugated correctly
	//or not.
	private String targetBeForm;

	//use a logger to output the scoring results onto the screen or file
	Logger logger = LoggerFactory.getLogger(getClass());

	//This main function is for testing the algorithm on some example responses.
	//The BePassiveDemo class can be used score as many sentences as needed
	//following the usage pattern shown in this main function.
	public static void main(String[] args) throws IOException, URISyntaxException {

		//Initiate the scorer with the target sentence, the lemma of the target
		//verb, and the target conjugation of 'be'.
	    BePassiveDemo scorer = new BePassiveDemo("Children should not be allowed "
	    		+ "to stay out late with their friends", "allow", "be");
	    
	    //Try the scorer with one response
	    String testResponse = "Children should not be agreed to stay out late "
	    		+ "with their friends.";
	    scorer.logger.info("Trying the scorer on a response: " + testResponse);
	    int testScore = scorer.getScore(testResponse);

	    //output the test run result
	    scorer.logger.info("The response should be given score 1. The algorithm's "
	    		+ "calculated score is: " + testScore);

	    //test completed, now apply the scorer on a lot of responses
	    scorer.logger.info("Applying the scorer on data from Kim & Godfroid (2023)...");

	    //load transcribed response, which is stored in the
	    //src/main/resources/data folder of this code repository
	    URL resourceFolderUrl = Thread.currentThread().getContextClassLoader()
	    		.getResource("data");
	    File resourceFolder = new File(resourceFolderUrl.toURI());
	    
	    //file storing the transcribed responses
	    File responseFile =new File(resourceFolder, "be_passive210_id.tsv");
	    
	    //file where the scores are to be stored
	    File scoreFile =new File(resourceFolder, "scores_be_passive210.tsv");
	    
	    //read the responses from the response file
	    List<String> responseLines = FileUtils.readLines(responseFile, "UTF-8");
	    responseLines.remove(0);//remove heading line
	    
	    //result lines, which will be written to the result file
	    List<String> resultLines = new ArrayList<>();
	    resultLines.add("id\ttranscription\tscore"); //add heading line

	    //iterate over the lines in the response file, getting the response from
	    //the second column in the TSV file and score them
		for (String responseLine : responseLines) {
			//Get the 2nd column (column index 1), which is the transcription of
			//the response
			String[] columns = responseLine.split("\t");
			if (columns.length > 1) {
				String response = columns[1];
				scorer.logger.info("Scoring response: " + response);
				int score = scorer.getScore(response);
				scorer.logger.info("\tScore: " + score);

				// Assuming you want to keep the original data and add a new column
				String resultLine = responseLine + "\t" + score;
				resultLines.add(resultLine);
			}
		}
		
		//pay attention to the output to see where the results file is stored
		scorer.logger.info("Writing results to file: " + scoreFile);
		FileUtils.writeLines(scoreFile, resultLines);
		scorer.logger.info("Results written successfully."); 

	}
	
	public BePassiveDemo(String target, String targetLemma, String targetBeForm) {
		this.target = target;
		this.targetLemma = targetLemma;
		this.targetBeForm = targetBeForm;
	}

	/**
	 * The scoring algorithm is implemented here. The function takes a response
	 * and return a score for that response.
	 * @param response
	 * @return
	 */
	public int getScore(String response) {
		// score 1: no error. No need to do NLP processing.
		logger.info("\tChecking exact match...");
		if (target.equals(response.trim())) {
			logger.info("\tResponse matches target. Give score 1.");
			return 1;
		}

		// Use the CoreNLP simple API, which is the fastest way to do NLP
		// processing, but without a lot of customization. It meets our scoring
		// task requirements though.
		logger.info("\tProcessing response with Corenlp...");
		Sentence sent = new Sentence(response);

		// get all the tokens and lemmas, POS tags. CoreNLP use the PennTreebank
		// tag set.
		List<String> lemmas = sent.lemmas();
		List<Token> tokens = sent.tokens();
		List<String> posTags = sent.posTags();
		
		// score 1: be + verb PP, but stem of the verb PP is not correct (e.g.
		// spelling mistakes), Case 4 in Table 2 of the paper.
		logger.info("\tChecking Case 4...");

		// iterate all the lemmas, try to find 'be'
		for (int i = 0; i < lemmas.size(); i++) {
			String lemma = lemmas.get(i);
			if (lemma.equals(targetBeForm)) { 
				// found 'be'
				logger.info("\t\tFound lemma 'be' at position {}", i);

				// Get the next token, see if it is verb PP, which is annotated
				// as VBN (see PTB tagset). We also try to account for the
				// situation where there is one additional word between 'be' and
				// the PP verb, e.g. 'is not allowed'. Limitation: this only
				// takes into account the 'immediate' next token (or next two
				// tokens). So it does not account for the situation when there
				// are more additional words between the 'be' and the PP verb,
				// e.g., 'Children are usually not supposed to go out at night.'
				// To account for such situations, more advanced rules need to
				// be implemented. For example, using Tregex or Tree Regular
				// Expressions will make it possible to account for these kinds
				// of more complex situations. But it is beyond the scope of
				// this tutorial to introduce the more advanced approaches.
				// Interested readers are encouraged to explore Tregex
				// themselves.
				if (i <= lemmas.size() - 1) { // avoid out of range
					int nextIdx = i + 1;
					//account for situation like 'is not allowed' as well
					if (posTags.get(nextIdx).equals("VBN") || posTags.get(nextIdx + 1).equals("VBN")) { 
						logger.info("\t\tNext token is also verb in Past Participle, returning "
								+ "score 1.");
						return 1;
					}
					logger.info("\t\tNo verb in PP form found following 'be'.", i);
				}
			}
		}
		
		
		//The following steps all result in a score of 0. Actually it is not
		//necessary to do the following steps if our scoring scale is binary as
		//described in the paper. Nonetheless, we keep the code to showcase how
		//other rules can be written as a reference.

		// score 0: use the stem in PP form, but no "be" or "be" in wrong form
		logger.info("\tChecking Case 3...");
		for (int i = 0; i < lemmas.size(); i++) {
			String lemma = lemmas.get(i);
			if (lemma.equals(targetLemma) && "VBN".equals(posTags.get(i))) {
				logger.info("\t\tFound target lemma in VBN form at position {}", i);
				if (i == 0) { // if the stem is the first word, there is no other words (expecting 'be').
					return 0;
				}

				//check if previous token 'be'
				int prevIdx = i - 1;
				if (tokens.get(prevIdx).word() != targetBeForm) {
					logger.info("\t\tPrevious token not target 'be' form, returning score 0...");
					return 0;
				}
			}

			if (lemma.equals(targetLemma) && posTags.get(i) != "VBN" && lemmas.get(i - 1).equals("be")) {
				logger.info("\t\tFound be + taget lemma but not in VBN. Returing score 0...");
				return 0;
			}
		}

		// all other possibilities
		return 0;
	}

}
