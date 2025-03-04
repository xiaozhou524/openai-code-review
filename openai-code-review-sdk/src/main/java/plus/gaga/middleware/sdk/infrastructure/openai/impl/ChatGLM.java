package plus.gaga.middleware.sdk.infrastructure.openai.impl;

import com.alibaba.fastjson2.JSON;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.infrastructure.openai.IOpenAI;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ChatGLM implements IOpenAI {

	private final String aipHost;

	private final String aipKeySecret;

	public ChatGLM(String aipHost, String aipKeySecret) {
		this.aipHost = aipHost;
		this.aipKeySecret = aipKeySecret;
	}

	@Override
	public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
		String token = BearerTokenUtils.getToken(aipKeySecret);

		URL url = new URL(aipHost);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Authorization", "Bearer " + token);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
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

//		System.out.println("评审结果：" + content);

		return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
	}
}
