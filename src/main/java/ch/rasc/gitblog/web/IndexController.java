package ch.rasc.gitblog.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import ch.rasc.gitblog.component.LuceneService;
import ch.rasc.gitblog.dto.PostMetadata;
import ch.rasc.gitblog.dto.SearchResults;
import ch.rasc.gitblog.dto.YearNavigation;

@Controller
public class IndexController {

	private final Template indexTemplate;

	private final LuceneService luceneService;

	public IndexController(Mustache.Compiler mustacheCompiler,
			LuceneService luceneService) throws IOException {
		this.luceneService = luceneService;

		ClassPathResource cpr = new ClassPathResource("/templates/index.mustache");
		try (InputStream is = cpr.getInputStream();
				InputStreamReader isr = new InputStreamReader(is,
						StandardCharsets.UTF_8);) {
			this.indexTemplate = mustacheCompiler.withFormatter(new Mustache.Formatter() {
				@Override
				public String format(Object value) {
					if (value instanceof ZonedDateTime) {
						return ((ZonedDateTime) value).format(this._fmt);
					}
					return String.valueOf(value);
				}

				protected DateTimeFormatter _fmt = DateTimeFormatter
						.ofPattern("MMMM dd, yyyy", Locale.ENGLISH);
			}).compile(isr);
		}
	}

	@GetMapping({ "/", "/index.html" })
	public ResponseEntity<?> index(
			@RequestParam(name = "tag", required = false) String tag,
			@RequestParam(name = "query", required = false) String query,
			@RequestParam(name = "year", required = false) String yearString) {

		Integer year = null;
		if (StringUtils.hasText(yearString)) {
			try {
				year = Integer.parseInt(yearString);
			}
			catch (NumberFormatException e) {
				// ignore this
			}
		}

		Set<Integer> years = this.luceneService.getPublishedYears();
		List<YearNavigation> yearNavigation;

		List<PostMetadata> posts;
		String queryString = null;
		if (StringUtils.hasText(tag)) {
			queryString = "tags:" + tag;
			posts = this.luceneService.searchWithTag(tag);

			yearNavigation = years.stream().map(y -> new YearNavigation(y, false))
					.collect(Collectors.toList());
		}
		else if (StringUtils.hasText(query)) {
			posts = this.luceneService.searchWithQuery(query);
			queryString = query;

			yearNavigation = years.stream().map(y -> new YearNavigation(y, false))
					.collect(Collectors.toList());
		}
		else if (year != null) {
			posts = this.luceneService.getPostsOfYear(year);

			final int queryYear = year;
			yearNavigation = years.stream().map(y -> new YearNavigation(y, y == queryYear))
					.collect(Collectors.toList());
		}
		else {
			int currentYear = LocalDate.now().getYear();
			posts = this.luceneService.getPostsOfYear(currentYear);

			if (posts.isEmpty()) {
				currentYear = currentYear - 1;
				posts = this.luceneService.getPostsOfYear(currentYear);
			}

			final int queryYear = currentYear;
			yearNavigation = years.stream()
					.map(y -> new YearNavigation(y, y == queryYear))
					.collect(Collectors.toList());
		}

		SearchResults result = new SearchResults(posts, queryString, yearNavigation);

		String indexHtml = this.indexTemplate.execute(result);

		return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
				.cacheControl(CacheControl.noCache()).body(indexHtml);
	}
}
