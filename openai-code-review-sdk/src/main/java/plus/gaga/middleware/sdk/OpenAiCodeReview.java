package plus.gaga.middleware.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.domain.service.impl.OpenAiCodeReviewService;
import plus.gaga.middleware.sdk.infrastructure.git.GitCommand;
import plus.gaga.middleware.sdk.infrastructure.openai.IOpenAI;
import plus.gaga.middleware.sdk.infrastructure.openai.impl.DeepSeekClient;
import plus.gaga.middleware.sdk.infrastructure.weixin.WeiXin;

public class OpenAiCodeReview {

	private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);

	// WX配置
	private String weixin_appid = "wx93a0dd45e2b6040d";
	private String weixin_secret = "6908e678c4ab6aff03aabfe612ccaf21";
	private String weixin_touser = "oDpQ56ZbL9XtcK9BUmQfsVpU3g8M";
	private String weixin_template_id = "sjLWIJLKlKD9cllAijczFNtEMIRqGpAnvIdHOXlemlw";

	// ChatGLM配置
	private String chatglm_apiHost = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
	private String chatglm_apiKeySecret = "";

	// GitHub配置
	private String github_review_log_url;
	private String github_token;

	// 工程配置 - 自动获取
	private String github_project;
	private String github_branch;
	private String github_author;

	public static void main(String[] args) throws Exception {

		GitCommand gitCommand = new GitCommand(
				getEnv("GITHUB_REVIEW_LOG_URI"),
				getEnv("GITHUB_TOKEN"),
				getEnv("COMMIT_PROJECT"),
				getEnv("COMMIT_BRANCH"),
				getEnv("COMMIT_AUTHOR"),
				getEnv("COMMIT_MESSAGE")
		);

		WeiXin weiXin = new WeiXin(
				getEnv("WEIXIN_APPID"),
				getEnv("WEIXIN_SECRET"),
				getEnv("WEIXIN_TOUSER"),
				getEnv("WEIXIN_TEMPLATE_ID")
		);

		// 使用ChatGLM接口
//		IOpenAI openAI = new ChatGLM(
//				getEnv("CHATGLM_APIHOST"),
//				getEnv("CHATGLM_APIKEYSECRET")
//		);
//		OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(gitCommand, openAI, weiXin);

		// 使用deepseek接口
		IOpenAI DeepSeek = new DeepSeekClient(
				"https://api.deepseek.com/chat/completions",
				"sk-39a453e30a36490c9e9b734b72b20a26"
		);
		OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(gitCommand, DeepSeek, weiXin);

		openAiCodeReviewService.exec();

		logger.info("openai-code-review done!");
	}

	private static String getEnv(String key) {
		String value = System.getenv(key);
		if (value == null || value.isEmpty()) {
			throw new RuntimeException(key + " is null");
		}
		return value;
	}

}
