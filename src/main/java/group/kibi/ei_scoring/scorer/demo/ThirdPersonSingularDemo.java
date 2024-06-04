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
 * This is for demonstrating scoring '3rd person singular -s' EI responses.
 */
public class ThirdPersonSingularDemo {

	//The target sentence, which is the sentence the test taker hears.
	private String target; 

	//the targeted verb
	private String targetLemma; 
	
	//the corrected form of the targeted verb
	private String targetLemmaForm;

	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) throws URISyntaxException, IOException {
		//Initiate the scorer with the target sentence, the lemma of the target
		//verb, and the correct form of the targeted verb.
		ThirdPersonSingularDemo scorer = 
				new ThirdPersonSingularDemo("Everyone loves to read comic books as a child.",
				"love", "loves");

	    //test completed, now apply the scorer on a lot of responses
	    scorer.logger.info("Applying the scorer on data from Kim & Godfroid (2023)...");

	    //load transcribed response, which is stored in the
	    //src/main/resources/data folder of this code repository
	    URL resourceFolderUrl = Thread.currentThread().getContextClassLoader()
	    		.getResource("data");
	    File resourceFolder = new File(resourceFolderUrl.toURI());
	    
	    //file storing the transcribed responses
	    File responseFile =new File(resourceFolder, "3rd_person_id.tsv");
	    
	    //file where the scores are to be stored
	    File scoreFile =new File(resourceFolder, "scores_3rd_person_id.tsv");
	    
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

	
	public ThirdPersonSingularDemo(String target, String targetLemma, String targetLemmaForm) {
		this.target = target;
		this.targetLemma = targetLemma;
		this.targetLemmaForm = targetLemmaForm;
	}

	/**
	 * The scoring algorithm is implemented here. The function takes a response
	 * and return a score for that response.
	 * 
	 * @param response  the student's response to the prompt
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

		// get all the tokens and lemmas, POS tags. Corenlp use the Penntreebank
		// tag set.
		List<String> lemmas = sent.lemmas();
		List<Token> tokens = sent.tokens();
		List<String> posTags = sent.posTags();

		// score 1: used -s form, either using the target verb or other verbs in
		// the 3rd person singular form
		logger.info("\tChecking verb used in 3rd person singular form...");
		for (int i = 0; i < lemmas.size(); i++) {
			if (tokens.get(i).equals(targetLemmaForm) || posTags.get(i).startsWith("VBZ")) {
				logger.info("\tFound target lemma in correct form or main verb in 3rd person "
						+ "singular at position {}", i);
				return 1;
			}
		}

		// all other possibilities
		return 0;
	}

}
