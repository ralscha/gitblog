package ch.rasc.gitblog.dto;

public class PostContent {
	private final PostMetadata metadata;

	private final String markdown;

	private final String html;

	public PostContent(PostMetadata metadata, String markdown, String html) {
		this.metadata = metadata;
		this.markdown = markdown;
		this.html = html;
	}

	public PostMetadata getMetadata() {
		return this.metadata;
	}

	public String getMarkdown() {
		return this.markdown;
	}

	public String getHtml() {
		return this.html;
	}

}
