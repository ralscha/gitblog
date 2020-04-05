package ch.rasc.gitblog.dto;

import java.util.Objects;

public class URLCheck {
	private final String url;

	private final String post;

	private final int status;

	private final String location;

	public URLCheck(String url, String post, int status, String location) {
		this.url = url;
		this.post = post;
		this.status = status;
		this.location = location;
	}

	public String getUrl() {
		return this.url;
	}

	public int getStatus() {
		return this.status;
	}

	public String getPost() {
		return this.post;
	}

	public String getLocation() {
		return this.location;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.location, this.post, this.status, this.url);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof URLCheck)) {
			return false;
		}
		URLCheck other = (URLCheck) obj;
		return Objects.equals(this.location, other.location)
				&& Objects.equals(this.post, other.post) && this.status == other.status
				&& Objects.equals(this.url, other.url);
	}

}
