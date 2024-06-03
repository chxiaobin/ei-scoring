package group.kibi.ei_scoring.scorer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;


/**
 * This scorer demonstrates how rules for a different scoring scale can be
 * written. Here a scale of 5 scores (0-4) are used.
 */
public class BePassiveScorer {

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
		BePassiveScorer scorer = new BePassiveScorer("Children should not be allowed to stay out late with their friends.",
				"allow", "be");

		int testScore1 = scorer.getScore("Children should not be allowed to stay out late with their friends.");
		scorer.logger.info("Should be: 4, Got: " + testScore1);
		
		int testScore2 = scorer.getScore("Children should not be agreed to stay out late with their friends.");
		scorer.logger.info("Should be: 3, Got: " + testScore2);

		int testScore3 = scorer.getScore("Children should not allowed to stay out late with their friends.");
		scorer.logger.info("Should be: 2, Got: " + testScore3);

		int testScore4 = scorer.getScore("Children should not allow to stay out late with their friends.");
		scorer.logger.info("Should be: 1, Got: " + testScore4);

		int testScore5 = scorer.getScore("Children should stay out late with their friends.");
		scorer.logger.info("Should be: 0, Got: " + testScore5);

	}
	
	public BePassiveScorer(String target, String targetLemma, String targetBeForm) {
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
		// score 4: no error. No need to do NLP processing.
		logger.info("Checking score 4...");
		if (target.equals(response.trim())) {
			logger.info("Response matches target. Give score of 4");
			return 4;
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

		// score 3: be + verb PP, but stem of the verb pp is not correct (e.g.
		// spelling mistakes)
		// iterate all the lemmas, try to find 'be'
		logger.info("Checking score 3...");
		for (int i = 0; i < lemmas.size(); i++) {
			String lemma = lemmas.get(i);
			if (lemma.equals("be")) { // found 'be'
				logger.info("Found lemma 'be' at position {}", i);
				// get the next token, see if it is verb PP, which is annotated as VBN (see PTB
				// tagset)
				if (i <= lemmas.size() - 1) { // avoid out of range
					int nextIdx = i + 1;
					if (posTags.get(nextIdx).equals("VBN")) {
						logger.info("Next token is also VBN, returning score 3.");
						return 3;
					}
				}

			}
		}

		// score 2: use the stem in PP form, but no "be" or "be" in wrong form
		logger.info("Checking score 2...");
		for (int i = 0; i < lemmas.size(); i++) {
			String lemma = lemmas.get(i);
			if (lemma.equals(targetLemma) && "VBN".equals(posTags.get(i))) {
				logger.info("Found target lemma in VBN form at position {}", i);
				if (i == 0) { // if the stem is the first word, there is no other words (expecting 'be').
					return 2;
				}

				int prevIdx = i - 1;
				logger.info(tokens.get(prevIdx).word());
				if (tokens.get(prevIdx).word() != targetBeForm) {
					logger.info("Previous token not target 'be' form, returning score 2...");
					return 2;
				}
			}

			if (lemma.equals(targetLemma) && posTags.get(i) != "VBN" && lemmas.get(i - 1).equals("be")) {
				logger.info("Found be + taget lemma but not in VBN. Returing score 2...");
				return 2;
			}
		}

		// socre 1: has the target stem, but not in PP and no 'be'
		logger.info("Checking score 1...");
		if (lemmas.contains(targetLemma)) {
			logger.info("Found target lemma, returing score 1...");
			return 1;
		}

		// all other possibilities
		return 0;
	}

}
