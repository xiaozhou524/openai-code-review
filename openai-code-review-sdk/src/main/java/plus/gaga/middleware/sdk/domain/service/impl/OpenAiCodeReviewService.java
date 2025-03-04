package plus.gaga.middleware.sdk.domain.service.impl;

import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.domain.service.AbstractOpenAiCodeReviewService;
import plus.gaga.middleware.sdk.infrastructure.git.GitCommand;
import plus.gaga.middleware.sdk.infrastructure.openai.IOpenAI;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import plus.gaga.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import plus.gaga.middleware.sdk.infrastructure.weixin.WeiXin;
import plus.gaga.middleware.sdk.infrastructure.weixin.dto.TemplateMessageDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OpenAiCodeReviewService extends AbstractOpenAiCodeReviewService {

	public OpenAiCodeReviewService(GitCommand gitCommand, IOpenAI openAI, WeiXin weiXin) {
		super(gitCommand, openAI, weiXin);
	}

	@Override
	protected String getDiffCode() {
		// 1. 获取提交代码
		return gitCommand.diff();
	}

	@Override
	protected String codeReview(String diffCode) throws Exception {
		// 2. 开始评审代码

		// openGLM
//		ChatCompletionRequestDTO chatCompletionRequestDTO = new ChatCompletionRequestDTO();
//		chatCompletionRequestDTO.setModel(Model.GLM_4_FLASH.getCode());
//		chatCompletionRequestDTO.setMessages(new ArrayList<ChatCompletionRequestDTO.Prompt>() {
//			{
//				add(new ChatCompletionRequestDTO.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码为: "));
//				add(new ChatCompletionRequestDTO.Prompt("user", diffCode));
//			}
//		});

		// deepSeek
		ChatCompletionRequestDTO chatCompletionRequestDTO = new ChatCompletionRequestDTO();
		chatCompletionRequestDTO.setModel("deepseek-chat");
		chatCompletionRequestDTO.setMessages(new ArrayList<ChatCompletionRequestDTO.Prompt>() {
			{
				add(new ChatCompletionRequestDTO.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码为: "));
				add(new ChatCompletionRequestDTO.Prompt("user", diffCode));
			}
		});


		ChatCompletionSyncResponseDTO responseDTO = openAI.completions(chatCompletionRequestDTO);
		ChatCompletionSyncResponseDTO.Message message = responseDTO.getChoices().get(0).getMessage();
		return message.getContent();
	}

	@Override
	protected String recordCodeReview(String recommend) throws Exception {
		// 3. 记录评审结果：返回日志地址
		return gitCommand.commitAndPush(recommend);
	}

	@Override
	protected void pushMessage(String logUrl) throws Exception {
		// 4. 发送消息通知：日志地址、通知的内容
		Map<String, Map<String, String>> data = new HashMap<>();
		TemplateMessageDTO.put(data, TemplateMessageDTO.TemplateKey.REPO_NAME, gitCommand.getProject());
		TemplateMessageDTO.put(data, TemplateMessageDTO.TemplateKey.BRANCH_NAME, gitCommand.getBranch());
		TemplateMessageDTO.put(data, TemplateMessageDTO.TemplateKey.COMMIT_AUTHOR, gitCommand.getAuthor());
		TemplateMessageDTO.put(data, TemplateMessageDTO.TemplateKey.COMMIT_MESSAGE, gitCommand.getMessage());

		weiXin.sendTemplateMessage(logUrl, data);
	}
}
