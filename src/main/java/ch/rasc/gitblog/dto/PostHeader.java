package ch.rasc.gitblog.dto;

import java.util.Date;
import java.util.List;

public class PostHeader {
	private String title;
	private List<String> tags;
	private boolean draft;
	private String summary;
	private Date published;
	private Date updated;

	public Date getPublished() {
		return this.published;
	}

	public void setPublished(Date published) {
		this.published = published;
	}

	public Date getUpdated() {
		return this.updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getTags() {
		return this.tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public boolean isDraft() {
		return this.draft;
	}

	public void setDraft(boolean draft) {
		this.draft = draft;
	}

	public String getSummary() {
		return this.summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Override
	public String toString() {
		return "PostHeader [title=" + this.title + ", tags=" + this.tags + ", draft="
				+ this.draft + ", summary=" + this.summary + ", published="
				+ this.published + ", updated=" + this.updated + "]";
	}

}
