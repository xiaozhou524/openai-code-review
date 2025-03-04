package plus.gaga.middleware.sdk;

import com.alibaba.fastjson2.JSON;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.domain.service.IOpenAiCodeReviewService;
import plus.gaga.middleware.sdk.domain.service.impl.OpenAiCodeReviewService;
import plus.gaga.middleware.sdk.infrastructure.git.GitCommand;
import plus.gaga.middleware.sdk.infrastructure.openai.IOpenAI;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.infrastructure.openai.impl.ChatGLM;
import plus.gaga.middleware.sdk.infrastructure.weixin.WeiXin;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;
import plus.gaga.middleware.sdk.types.utils.WXAccessTokenUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

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
//				getEnv("CODE_REVIEW_LOG_URL"),
				"https://github.com/xiaozhou524/openai-code-review-log",
				"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",
//				getEnv("CODE_TOKEN"),
				getEnv("COMMIT_PROJECT"),
				getEnv("COMMIT_BRANCH"),
				getEnv("COMMIT_AUTHOR"),
				getEnv("COMMIT_MESSAGE")
		);

		IOpenAI openAI = new ChatGLM(
				getEnv("CHATGLM_AIPHOST"),
				getEnv("CHATGLM_AIPKEYSECRET")
		);

		WeiXin weiXin = new WeiXin(
				getEnv("WEIXIN_APPID"),
				getEnv("WEIXIN_SECRET"),
				getEnv("WEIXIN_TOUSER"),
				getEnv("WEIXIN_TEMPLATE_ID")
		);

		OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(gitCommand, openAI, weiXin);
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
