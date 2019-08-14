package ch.rasc.gitblog.dto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

public class PostMetadata {

	private final boolean draft;

	private final String url;

	private final Path mdFile;

	private final String feedbackUrl;

	private final String title;

	private final List<String> tags;

	private final ZonedDateTime published;

	private final ZonedDateTime updated;

	private final String summary;

	public PostMetadata(Document doc) {
		this.draft = false;
		this.url = doc.get("url");
		this.mdFile = Paths.get(doc.get("path"));
		this.feedbackUrl = this.url.replace("/", "-");
		this.summary = doc.get("summary");
		this.title = doc.get("title");
		this.tags = Arrays.asList(doc.getValues("tags"));

		long millis = doc.getField("published").numericValue().longValue();
		this.published = Instant.ofEpochSecond(millis).atZone(ZoneOffset.UTC);

		IndexableField field = doc.getField("updated");
		if (field != null) {
			millis = field.numericValue().longValue();
			this.updated = Instant.ofEpochSecond(millis).atZone(ZoneOffset.UTC);
		}
		else {
			this.updated = null;
		}
	}

	public PostMetadata(PostHeader header, Path baseDir, Path mdFile) {
		this.draft = header.isDraft();
		this.url = url(baseDir, siblingPath(mdFile, "html"));
		this.mdFile = mdFile;
		this.feedbackUrl = this.url.replace("/", "-");
		this.tags = header.getTags();
		this.summary = header.getSummary();
		this.title = header.getTitle();
		this.published = ZonedDateTime.ofInstant(header.getPublished().toInstant(),
				ZoneOffset.UTC);
		if (header.getUpdated() != null) {
			this.updated = ZonedDateTime.ofInstant(header.getUpdated().toInstant(),
					ZoneOffset.UTC);
		}
		else {
			this.updated = null;
		}
	}

	public Path getMdFile() {
		return this.mdFile;
	}

	public String getFeedbackUrl() {
		return this.feedbackUrl;
	}

	public boolean isDraft() {
		return this.draft;
	}

	public String getUrl() {
		return this.url;
	}

	public String getTitle() {
		return this.title;
	}

	public List<String> getTags() {
		return this.tags;
	}

	public ZonedDateTime getPublished() {
		return this.published;
	}

	public ZonedDateTime getUpdated() {
		return this.updated;
	}

	public String getSummary() {
		return this.summary;
	}

	public static Path siblingPath(Path file, String postfix) {
		String siblingFleName = file.getFileName().toString();

		int lastDot = siblingFleName.lastIndexOf(".");
		if (lastDot != -1) {
			siblingFleName = siblingFleName.substring(0, lastDot) + "." + postfix;
		}
		else {
			siblingFleName = siblingFleName + "." + postfix;
		}

		return file.resolveSibling(siblingFleName);
	}

	public static String url(Path baseDir, Path htmlFile) {
		String url = baseDir.relativize(htmlFile).toString();
		return url.replace("\\", "/");
	}
}
