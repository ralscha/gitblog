package ch.rasc.gitblog;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {

	private String blogTitle;

	private String blogDescription;

	private String blogAuthor;

	private String baseUrl;

	private String gitRepository;

	private String gitRepositoryUser;

	private String gitRepositoryPassword;

	private String workDir;

	private String prismJsWorkdir;

	private String prismJsDownloadUrl;

	private String prismJsVersion;

	private String webhookSecret;

	private String luceneDir;

	private String feedbackFromEmail;

	private String feedbackToEmail;

	private String awsAccessKey;

	private String awsSecretKey;

	private String awsBucket;

	private String ignoreUrlList;

	private String brotliCmd;

	public String getBlogTitle() {
		return this.blogTitle;
	}

	public void setBlogTitle(String blogTitle) {
		this.blogTitle = blogTitle;
	}

	public String getBlogDescription() {
		return this.blogDescription;
	}

	public void setBlogDescription(String blogDescription) {
		this.blogDescription = blogDescription;
	}

	public String getBlogAuthor() {
		return this.blogAuthor;
	}

	public void setBlogAuthor(String blogAuthor) {
		this.blogAuthor = blogAuthor;
	}

	public String getBaseUrl() {
		return this.baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getGitRepository() {
		return this.gitRepository;
	}

	public void setGitRepository(String gitRepository) {
		this.gitRepository = gitRepository;
	}

	public String getWorkDir() {
		return this.workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	public String getWebhookSecret() {
		return this.webhookSecret;
	}

	public void setWebhookSecret(String webhookSecret) {
		this.webhookSecret = webhookSecret;
	}

	public String getLuceneDir() {
		return this.luceneDir;
	}

	public void setLuceneDir(String luceneDir) {
		this.luceneDir = luceneDir;
	}

	public String getPrismJsWorkdir() {
		return this.prismJsWorkdir;
	}

	public void setPrismJsWorkdir(String prismJsWorkdir) {
		this.prismJsWorkdir = prismJsWorkdir;
	}

	public String getPrismJsDownloadUrl() {
		return this.prismJsDownloadUrl;
	}

	public void setPrismJsDownloadUrl(String prismJsDownloadUrl) {
		this.prismJsDownloadUrl = prismJsDownloadUrl;
	}

	public String getPrismJsVersion() {
		return this.prismJsVersion;
	}

	public void setPrismJsVersion(String prismJsVersion) {
		this.prismJsVersion = prismJsVersion;
	}

	public String getFeedbackFromEmail() {
		return this.feedbackFromEmail;
	}

	public void setFeedbackFromEmail(String feedbackFromEmail) {
		this.feedbackFromEmail = feedbackFromEmail;
	}

	public String getFeedbackToEmail() {
		return this.feedbackToEmail;
	}

	public void setFeedbackToEmail(String feedbackToEmail) {
		this.feedbackToEmail = feedbackToEmail;
	}

	public String getGitRepositoryUser() {
		return this.gitRepositoryUser;
	}

	public void setGitRepositoryUser(String gitRepositoryUser) {
		this.gitRepositoryUser = gitRepositoryUser;
	}

	public String getGitRepositoryPassword() {
		return this.gitRepositoryPassword;
	}

	public void setGitRepositoryPassword(String gitRepositoryPassword) {
		this.gitRepositoryPassword = gitRepositoryPassword;
	}

	public String getAwsAccessKey() {
		return this.awsAccessKey;
	}

	public void setAwsAccessKey(String awsAccessKey) {
		this.awsAccessKey = awsAccessKey;
	}

	public String getAwsSecretKey() {
		return this.awsSecretKey;
	}

	public void setAwsSecretKey(String awsSecretKey) {
		this.awsSecretKey = awsSecretKey;
	}

	public String getAwsBucket() {
		return this.awsBucket;
	}

	public void setAwsBucket(String awsBucket) {
		this.awsBucket = awsBucket;
	}

	public String getIgnoreUrlList() {
		return this.ignoreUrlList;
	}

	public void setIgnoreUrlList(String ignoreUrlList) {
		this.ignoreUrlList = ignoreUrlList;
	}

	public String getBrotliCmd() {
		return this.brotliCmd;
	}

	public void setBrotliCmd(String brotliCmd) {
		this.brotliCmd = brotliCmd;
	}

}
