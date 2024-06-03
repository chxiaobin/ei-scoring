package group.kibi.ei_scoring.scorer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;

/**
 * This class demonstrates a scorer with a different scoring scale.
 */
public class ThirdPersonSingularScorer {

	//the target corrected prompt
	private String target; 

	//the targeted verb
	private String targetLemma; 
	private String targetLemmaForm;

	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		ThirdPersonSingularScorer scorer = 
				new ThirdPersonSingularScorer("Everyone loves to read comic books as a child.",
				"love", "loves");

		int testScore1 = scorer.getScore("Everyone loves to read comic books as a child.");
		scorer.logger.info("Should be: 2, Got: " + testScore1);
		
		int testScore2 = scorer.getScore("Everyone love to read comic books as a child.");
		scorer.logger.info("Should be: 1, Got: " + testScore2);

		int testScore3 = scorer.getScore("Everyone likes to read comic books.");
		scorer.logger.info("Should be: 0, Got: " + testScore3);

	}
	
	public ThirdPersonSingularScorer(String target, String targetLemma, String targetLemmaForm) {
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
		logger.info("Checking score 2...");
		if (target.equals(response.trim())) {
			logger.info("Response matches target. Give score of 2");
			return 2;
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

		// score 1: use the stem in PP form, but no "be" or "be" in wrong form
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
