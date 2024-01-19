package group.kibi.ei_scoring;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Using OpenAI's Whisper model for speech transcription. The API documentation
 * can be found at: https://platform.openai.com/docs/guides/speech-to-text
 */
public class WhisperTranscriber {

	static final MediaType MEDIA_TYPE_WAV = MediaType.parse("audio/wav");
	static final String WHISPER_SERVICE_URL = "https://api.openai.com/v1/audio/transcriptions";
	String apiKey;

	OkHttpClient mOkHttpClient;

	public WhisperTranscriber(String apiKey) {
		System.out.println("Initializing Whisper transcriber...");
		this.apiKey = apiKey;
		// Initialize an HTTP client, which will be used to access the Whisper
		// service. The client can be reused, so initialize it in constructor.
		mOkHttpClient = new OkHttpClient().newBuilder().connectTimeout(15, TimeUnit.SECONDS).build();
	}

	public String transcribe(File audioFile) throws IOException {
		System.out.println("Transcribing file: " + audioFile.getAbsolutePath());

		// Construct the request body, which sets the parameters for our
		// transcription needs. In the request body, we need to provide Whisper
		// with the file we want it to transcribe, as well as the ASR model to
		// use for the transcription. Currently only the 'whisper-1' model is
		// available. We can optionally also set the other parameters according
		// to the API reference at:
		// https://platform.openai.com/docs/api-reference/audio/createTranscription
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("model", "whisper-1").addFormDataPart("language", "en")
				.addFormDataPart("file", audioFile.getName(), RequestBody.create(audioFile, MEDIA_TYPE_WAV)).build();

		// Create the HTTP request, which requires authentication. So we need to
		// send a request with the 'Authorization' header, whose value is set to
		// the API key we get from OpenAI. More information about authentication
		// can be found at:
		// https://platform.openai.com/docs/api-reference/authentication
		Request request = new Request.Builder()
				.url(WHISPER_SERVICE_URL)
				.header("Authorization", "Bearer " + apiKey)
				.post(requestBody) // send a POST request
				.build();

		//Execute the request with the HTTP client. The transcription will be
		//returned in a Response object, which consists of response status code
		//telling whether the request was successful or not and a response body
		//with the returned data.
		try (Response response = mOkHttpClient.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected response code " + response);
			}

			//Get the response body, which contains the transcription in json format.
			String responseBody = response.body().string();

			//Convert the json format to a java object for easy extraction of
			//the transcription.
			WhisperResponse whisperResponse = new Gson().fromJson(responseBody, WhisperResponse.class);
			
			//Everything's OK. Return the transcription.
			return whisperResponse.getText();
		}
	}

	/**
	 * For testing the WhisperTranscriber. It takes a file argument and
	 * transcribes the file.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		//Check if user pass in the file to be analyzed.
		if(args.length != 2) {
			System.out.println("Usage: WhisperTranscriber API_KEY /path/to/wav/file.wav");
			System.exit(1);
		}
		
		//get apikey
		String apiKey = args[0];
		
		//Check if the audio file exists.
		String filePath = args[1];
		File audioFile = new File(filePath);
		if(!audioFile.exists()) {
			System.out.println(String.format("File '%s' does not exist.", filePath));
			System.exit(1);
		}

		//Transcribe the file.
		WhisperTranscriber transcriber = new WhisperTranscriber(apiKey);
		String transcription = transcriber.transcribe(audioFile);
		System.out.println("Transcription: " + transcription);

	}

	/**
	 * Whisper returns a json object with a 'text' key whose value is the
	 * transcription. We therefore use this class for converting the returned
	 * json object to a Java object. Then we can retrieve the transcription easily.
	 */
	static class WhisperResponse {
		private String text;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}
}
