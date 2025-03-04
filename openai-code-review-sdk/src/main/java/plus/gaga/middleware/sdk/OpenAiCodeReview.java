package plus.gaga.middleware.sdk;

import com.alibaba.fastjson2.JSON;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import plus.gaga.middleware.sdk.domain.model.ChatCompletionRequest;
import plus.gaga.middleware.sdk.domain.model.ChatCompletionSyncResponse;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;
import plus.gaga.middleware.sdk.types.utils.WXAccessTokenUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class OpenAiCodeReview {

	public static void main(String[] args) throws Exception {
		System.out.println("openai 代码评审，测试执行");

		String token = System.getenv("GITHUB_TOKEN");
		if (token == null || token.isEmpty()) {
			throw new RuntimeException("token is null");
		}

		// 1. 代码检出
		ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
		processBuilder.directory(new File("."));

		Process process = processBuilder.start();

		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;

		StringBuilder diffCode = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			diffCode.append(line);
		}

		int exitCode = process.waitFor();
		System.out.println("Exited with code:" + exitCode);

		System.out.println("diff code：" + diffCode.toString());

		// 2. chatglm代码评审
		String log = codeReview(diffCode.toString());
		System.out.println("code review:" + log);

		// 3. 写入评审日志
		String logUrl = writeLog(token, log);
		System.out.println("writeLog:" + logUrl);

		// 4. 消息通知
		pushMessage(logUrl);
		System.out.println("pushMessage:" + logUrl);
	}

	private static String codeReview(String diffCode) throws IOException {
		String aipKeySecret = "336d5b9823404b2ea94dd64f623f672f.2L79s4Z5SfgnKWym";
		String token = BearerTokenUtils.getToken(aipKeySecret);

		URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Authorization", "Bearer " + token);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
		connection.setDoOutput(true);

		ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
		chatCompletionRequest.setModel(Model.GLM_4_FLASH.getCode());
		chatCompletionRequest.setMessages(new ArrayList<ChatCompletionRequest.Prompt>() {{
			add(new ChatCompletionRequest.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码为: "));
			add(new ChatCompletionRequest.Prompt("user", diffCode));
		}});

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = JSON.toJSONString(chatCompletionRequest).getBytes(StandardCharsets.UTF_8);
			os.write(input);
		}

		int responseCode = connection.getResponseCode();
		System.out.println(responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;

		StringBuilder content = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}

		in.close();
		connection.disconnect();

		System.out.println("评审结果：" + content);

		ChatCompletionSyncResponse response = JSON.parseObject(content.toString(), ChatCompletionSyncResponse.class);
		return response.getChoices().get(0).getMessage().getContent();
	}

	private static String writeLog(String token, String log) throws Exception {

		Git git = Git.cloneRepository()
				.setURI("https://github.com/xiaozhou524/openai-code-review-log.git")
				.setDirectory(new File("repo"))
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
				.call();

		String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		File dateFolder = new File("repo/" + dateFolderName);
		if (!dateFolder.exists()) {
			dateFolder.mkdirs();
		}

		String fileName = generateRandomString(12) + ".md";
		File newFile = new File(dateFolder, fileName);
		try(FileWriter writer = new FileWriter(newFile)) {
			writer.write(log);
		}

		git.add().addFilepattern(dateFolderName + "/" + fileName).call();
		git.commit().setMessage("Add new file via GitHub Actions").call();
		git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, "")).call();

		System.out.println("Changes have been pushed to the repository.");

		return "https://github.com/xiaozhou524/openai-code-review-log/blob/master/" + dateFolderName + "/" + fileName;
	}

	private static String generateRandomString(int length) {
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(characters.charAt(random.nextInt(characters.length())));
		}
		return sb.toString();
	}

	private static void pushMessage(String logUrl) {
		String accessToken = WXAccessTokenUtils.getAccessToken();

		Message message = new Message();
		message.put("project", "评审");
		message.put("review", logUrl);
		message.setUrl(logUrl);

		String url = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken);
		sendPostRequest(url, JSON.toJSONString(message));
	}

	public static class Message {
		private String touser = "oDpQ56ZbL9XtcK9BUmQfsVpU3g8M";
		private String template_id = "gNHicdhw-z74M2q_Vb64KcTbZOfA9kr9vpi5rX7VKdE";
		private String url = "https://github.com/xiaozhou524/openai-code-review-log/blob/main/2025-03-03/Y8LNSZqYaaFt.md";
		private Map<String, Map<String, String>> data = new HashMap<>();

		public void put(String key, String value) {
			data.put(key, new HashMap<String, String>() {
				{
					put("value", value);
				}
			});
		}

		public String getTouser() {
			return touser;
		}

		public void setTouser(String touser) {
			this.touser = touser;
		}

		public String getTemplate_id() {
			return template_id;
		}

		public void setTemplate_id(String template_id) {
			this.template_id = template_id;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public Map<String, Map<String, String>> getData() {
			return data;
		}

		public void setData(Map<String, Map<String, String>> data) {
			this.data = data;
		}
	}

	private static void sendPostRequest(String urlString, String jsonBody) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json; utf-8");
			conn.setRequestProperty("Accept", "application/json");
			conn.setDoOutput(true);

			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
				String response = scanner.useDelimiter("\\A").next();
				System.out.println(response);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
