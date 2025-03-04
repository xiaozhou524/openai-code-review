package plus.gaga.middleware.sdk.infrastructure.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.types.utils.RandomStringUtiles;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GitCommand {

	private final Logger logger = LoggerFactory.getLogger(GitCommand.class);

	private final String githubReviewLogUrl;
	private final String githubToken;
	private final String project;
	private final String branch;
	private final String author;
	private final String message;

	public GitCommand(String githubReviewLogUrl, String githubToken, String project, String branch, String author, String message) {
		this.githubReviewLogUrl = githubReviewLogUrl;
		this.githubToken = githubToken;
		this.project = project;
		this.branch = branch;
		this.author = author;
		this.message = message;
	}

	public String diff(){
		try {
			// 获取最近一次提交的哈希值
			ProcessBuilder logProcessBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%H");
			logProcessBuilder.directory(new File("."));
			Process logProcess = logProcessBuilder.start();
			BufferedReader logReader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
			String latestCommitHash = logReader.readLine();
			int logExitCode = logProcess.waitFor();
			if (logExitCode != 0) {
				throw new RuntimeException("Failed to get latest commit hash, exit code: " + logExitCode);
			}

			// 获取差异
			ProcessBuilder diffProcessBuilder = new ProcessBuilder("git", "diff", latestCommitHash + "^", latestCommitHash);
			diffProcessBuilder.directory(new File("."));
			Process diffProcess = diffProcessBuilder.start();
			BufferedReader diffReader = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()));
			StringBuilder diffCode = new StringBuilder();
			String line;
			while ((line = diffReader.readLine()) != null) {
				diffCode.append(line).append("\n");
			}
			int diffExitCode = diffProcess.waitFor();
			if (diffExitCode != 0) {
				throw new RuntimeException("Failed to get diff, exit code: " + diffExitCode);
			}
			return diffCode.toString();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Failed to execute git command", e);
		}
	}

	public String commitAndPush(String recommend) throws Exception {
		Git git = Git.cloneRepository()
				.setURI(githubReviewLogUrl+".git")
				.setDirectory(new File("repo"))
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, ""))
				.call();

		// 创建分支
		String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		File dateFolder = new File("repo/" + dateFolderName);
		if (!dateFolder.exists()) {
			dateFolder.mkdirs();
		}

		// md文件名字
		String fileName = project + "-" + branch + "-" + author + System.currentTimeMillis()
				+ "-" + RandomStringUtiles.generateRandomString(4) + ".md";
		File newFile = new File(dateFolder, fileName);
		try(FileWriter writer = new FileWriter(newFile)) {
			writer.write(recommend);
		}

		// 提交内容
		git.add().addFilepattern(dateFolderName + "/" + fileName).call();
		git.commit().setMessage("add code review new file" + fileName).call();
		git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, "")).call();

		logger.info("openao-code-review git commit and push done! {}", fileName);

		// "https://github.com/xiaozhou524/openai-code-review-log/blob/master/"
		return  githubReviewLogUrl + "/blob/master/" + dateFolderName + "/" + fileName;
	}

	public String getProject() {
		return project;
	}

	public String getBranch() {
		return branch;
	}

	public String getAuthor() {
		return author;
	}

	public String getMessage() {
		return message;
	}
}
