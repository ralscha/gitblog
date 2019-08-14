package ch.rasc.gitblog.dto;

import java.util.List;

public class SearchResults {
	private final List<PostMetadata> posts;

	private final String query;

	private final List<YearNavigation> years;

	public SearchResults(List<PostMetadata> posts, String query, List<YearNavigation> years) {
		this.posts = posts;
		this.query = query;
		this.years = years;
	}

	public List<PostMetadata> getPosts() {
		return this.posts;
	}

	public String getQuery() {
		return this.query;
	}

	public List<YearNavigation> getYears() {
		return this.years;
	}

}
