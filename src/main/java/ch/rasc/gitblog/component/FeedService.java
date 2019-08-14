package ch.rasc.gitblog.component;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndCategoryImpl;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedOutput;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import ch.rasc.gitblog.dto.PostMetadata;
import ch.rasc.gitblog.feed.AtomNSModule;
import ch.rasc.gitblog.feed.AtomNSModuleImpl;
import ch.rasc.gitblog.feed.CustomFeedEntry;
import ch.rasc.gitblog.feed.CustomSyndEntry;
import ch.rasc.gitblog.service.FileService;

@Component
public class FeedService {

	private final AppProperties appProperties;

	public FeedService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public boolean filesExists() {
		Path baseDir = Paths.get(this.appProperties.getWorkDir());
		return Files.exists(baseDir.resolve("feed.rss"))
				&& Files.exists(baseDir.resolve("feed.atom"));
	}

	public void writeFeeds(List<PostMetadata> posts) {
		rss2(posts);
		atom1(posts);
	}

	private void rss2(List<PostMetadata> posts) {
		Path baseDir = Paths.get(this.appProperties.getWorkDir());
		Path feedFile = baseDir.resolve("feed.rss");
		try (Writer writer = Files.newBufferedWriter(feedFile)) {
			WireFeedOutput output = new WireFeedOutput();
			output.output(createWireFeed(posts, "rss_2.0"), writer);
		}
		catch (IllegalArgumentException | IOException | FeedException e) {
			Application.logger.error("write rss feed", e);
		}
		if (Files.exists(feedFile)) {
			FileService.gzip(feedFile);
		}
	}

	private void atom1(List<PostMetadata> posts) {
		Path baseDir = Paths.get(this.appProperties.getWorkDir());
		Path feedFile = baseDir.resolve("feed.atom");
		try (Writer writer = Files.newBufferedWriter(feedFile)) {
			WireFeedOutput output = new WireFeedOutput();
			output.output(createWireFeed(posts, "atom_1.0"), writer);
		}
		catch (IllegalArgumentException | IOException | FeedException e) {
			Application.logger.error("write rss feed", e);
		}
		if (Files.exists(feedFile)) {
			FileService.gzip(feedFile);
		}
	}

	private WireFeed createWireFeed(List<PostMetadata> posts, String feedType) {
		String baseURL = this.appProperties.getBaseUrl();

		SyndFeed feed;
		if (feedType.equals("rss_2.0")) {
			feed = new CustomFeedEntry();
		}
		else {
			feed = new SyndFeedImpl();
		}

		feed.setFeedType(feedType);

		feed.setTitle(this.appProperties.getBlogTitle());
		feed.setDescription(this.appProperties.getBlogDescription());
		feed.setLink(baseURL);
		feed.setAuthor(this.appProperties.getBlogAuthor());
		feed.setUri(baseURL);

		AtomNSModule atomNSModule = new AtomNSModuleImpl();
		String link = feedType.equals("rss_2.0") ? "feed.rss" : "feed.atom";
		atomNSModule.setLink(baseURL + link);
		feed.getModules().add(atomNSModule);

		Date latestPublishedDate = null;

		List<SyndEntry> entries = new ArrayList<>();
		for (PostMetadata post : posts) {
			SyndEntry entry;
			if (feedType.equals("rss_2.0")) {
				entry = new CustomSyndEntry();
			}
			else {
				entry = new SyndEntryImpl();
			}
			entry.setTitle(post.getTitle());
			entry.setAuthor(this.appProperties.getBlogAuthor());
			entry.setLink(baseURL + post.getUrl());
			entry.setUri(baseURL + post.getUrl());

			Date published = Date.from(post.getPublished().toInstant());
			entry.setPublishedDate(published);

			if (post.getUpdated() != null) {
				Date updated = Date.from(post.getUpdated().toInstant());
				entry.setUpdatedDate(updated);
			}
			else {
				entry.setUpdatedDate(published);
			}

			if (latestPublishedDate == null
					|| published.getTime() > latestPublishedDate.getTime()) {
				latestPublishedDate = published;
			}

			List<SyndCategory> categories = new ArrayList<>();
			if (post.getTags() != null) {
				for (String tag : post.getTags()) {
					SyndCategory category = new SyndCategoryImpl();
					category.setName(tag);
					categories.add(category);
				}
			}

			if (!categories.isEmpty()) {
				entry.setCategories(categories);
			}

			if (StringUtils.hasText(post.getSummary())) {
				SyndContent description = new SyndContentImpl();
				description.setType("text/plain");
				description.setValue(post.getSummary());
				entry.setDescription(description);
			}
			else {
				SyndContent description = new SyndContentImpl();
				description.setType("text/plain");
				description.setValue(post.getTitle());
				entry.setDescription(description);

			}

			entries.add(entry);
		}

		if (latestPublishedDate != null) {
			feed.setPublishedDate(latestPublishedDate);
		}

		feed.setEntries(entries);
		return feed.createWireFeed();
	}
}
