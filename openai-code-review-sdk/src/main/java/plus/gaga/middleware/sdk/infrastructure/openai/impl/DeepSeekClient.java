package plus.gaga.middleware.sdk.infrastructure.openai.impl;

import com.alibaba.fastjson2.JSON;
import plus.gaga.middleware.sdk.infrastructure.openai.IOpenAI;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DeepSeekClient implements IOpenAI {

	private final String apiHost;

	private final String apiKey;

	public DeepSeekClient(String apiHost, String apiKey) {
		this.apiHost = apiHost;
		this.apiKey = apiKey;
	}

	@Override
	public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
		URL url = new URL(apiHost);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = JSON.toJSONString(requestDTO).getBytes(StandardCharsets.UTF_8);
			os.write(input);
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuilder content = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}

		in.close();
		connection.disconnect();

		return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
	}
}
