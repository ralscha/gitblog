package ch.rasc.gitblog.dto;

import java.util.List;

public class Commit {

	private List<String> added;
	private List<String> removed;
	private List<String> modified;

	public List<String> getAdded() {
		return this.added;
	}

	public void setAdded(List<String> added) {
		this.added = added;
	}

	public List<String> getRemoved() {
		return this.removed;
	}

	public void setRemoved(List<String> removed) {
		this.removed = removed;
	}

	public List<String> getModified() {
		return this.modified;
	}

	public void setModified(List<String> modified) {
		this.modified = modified;
	}

	@Override
	public String toString() {
		return "Commit [added=" + this.added + ", removed=" + this.removed + ", modified="
				+ this.modified + "]";
	}

}
