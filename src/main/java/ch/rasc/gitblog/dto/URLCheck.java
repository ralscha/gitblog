package ch.rasc.gitblog.dto;

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

}
