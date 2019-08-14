package ch.rasc.gitblog.dto;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

public class GitChange {
	private final ChangeType changeType;

	private final String newPath;

	private final String oldPath;

	public GitChange(ChangeType changeType, String newPath, String oldPath) {
		this.changeType = changeType;
		this.newPath = !"/dev/null".equals(newPath) ? newPath : null;
		this.oldPath = !"/dev/null".equals(oldPath) ? oldPath : null;
	}

	public ChangeType getChangeType() {
		return this.changeType;
	}

	public String getNewPath() {
		return this.newPath;
	}

	public String getOldPath() {
		return this.oldPath;
	}

	@Override
	public String toString() {
		return "GitChange [changeType=" + this.changeType + ", newPath=" + this.newPath
				+ ", oldPath=" + this.oldPath + "]";
	}

}
