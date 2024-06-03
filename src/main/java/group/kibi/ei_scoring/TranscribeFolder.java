package group.kibi.ei_scoring;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterate over a folder and transcribe all .wav audio files with OpenAI's
 * Whisper service. It requires an API key from OpenAI.
 */
public class TranscribeFolder {
	static String transcription;

	static Logger logger = LoggerFactory.getLogger(TranscribeFolder.class);
	
	public static void main(String[] args) throws IOException {
		//Check if user pass in the folder to be transcribed.
		if(args.length != 3) {
			logger.info("Usage: TranscribeFolder API_KEY /path/to/audio/folder /results/folder");
			System.exit(1);
		}
		
		//get apikey
		String apiKey = args[0];
		String audioFolderPath = args[1];
		String resultsFolderPath = args[2];
		
		//Check if the audio folder exists.
		File audioFolder = new File(audioFolderPath);
		if(!audioFolder.exists()) {
			logger.error(String.format("Audio folder '%s' does not exist.", audioFolderPath));
			System.exit(1);
		}

		//check if results folder exists
		File resultsFolder = new File(resultsFolderPath);
		if(!resultsFolder.exists()) {
			logger.info(String.format("Results folder '%s' does not exist. Creating the folder...", audioFolderPath));
			if(resultsFolder.mkdirs()) {
				logger.info("Created.");
			} else {
				System.exit(1);
			}

		}
		
		//Run the transcription
		createWhisperThread(apiKey, audioFolder, resultsFolder).start();

	}
	
	/**
	 * Create a separate thread to run the transcription. This allows restarting
	 * of the thread if somehow the Whisper service throws some errors.
	 * @param apiKey
	 * @param audioFolder
	 * @param resultsFolder
	 * @return
	 */
	private static Thread createWhisperThread(String apiKey, File audioFolder, File resultsFolder) {
		
		Iterator<File> fileIterator =
				FileUtils.iterateFiles(audioFolder, new WildcardFileFilter("*.wav"), TrueFileFilter.INSTANCE);
		long now = System.currentTimeMillis(); //this is used to name the results file so that it doesn't override some existing results file.
		File resultsFile = new File(resultsFolder, "ei_transcriptions_" + now + ".tsv");
		WhisperTranscriber whisperTranscriber = new WhisperTranscriber(apiKey);

		Thread whisperThread = new Thread(new Runnable() {
			@Override
			public void run() {
				logger.info("Creating Whisper transcribe thread to transcribe folder '{}'...", audioFolder.getAbsolutePath());
				while(fileIterator.hasNext()) {
					File file = fileIterator.next();
					String fileName = file.getName();
					logger.info("Transcribing file: " + fileName);

					String transcription;
					try {
						transcription = whisperTranscriber.transcribe(file);
						logger.info("\tTranscription: " + transcription);
						String resultLine = 
								fileName + "\t" + transcription + "\n";
						FileUtils.write(resultsFile, resultLine, "utf-8", true);
						
						//remove the file once it is finished.
//						FileUtils.delete(file);
					} catch (IOException e) {
						logger.error("IOException when writing results file.", e);
					}
				}
				
				//All files transcribed.
				logger.info("Transcription completed. Results written to file: {}.", resultsFile.getAbsolutePath());
			}
			
		});

		whisperThread.setUncaughtExceptionHandler((UncaughtExceptionHandler) new UncaughtExceptionHandler() {
			
			public void uncaughtException(Thread theThread, Throwable cause) {
				//restart thread when error
				logger.error("An unknown exception occurred, restarting the transcription thread...");
				logger.error(cause.getMessage(), cause);
				theThread.start(); 
			}
		});
		
		return 	whisperThread;
	}
	
}
