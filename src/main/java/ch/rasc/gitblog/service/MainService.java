package ch.rasc.gitblog.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ch.rasc.gitblog.Application;
import ch.rasc.gitblog.component.FeedService;
import ch.rasc.gitblog.component.GitService;
import ch.rasc.gitblog.component.LuceneService;
import ch.rasc.gitblog.component.SitemapService;
import ch.rasc.gitblog.component.URLChecker;
import ch.rasc.gitblog.dto.GitChange;
import ch.rasc.gitblog.dto.PostContent;
import ch.rasc.gitblog.dto.PostMetadata;

@Service
public class MainService {

	private final FileService fileService;

	private final FeedService feedService;

	private final SitemapService sitemapService;

	private final LuceneService luceneSerivce;

	private final URLChecker urlChecker;

	private final ExecutorService executorService;

	private final GitService gitService;

	public MainService(FileService fileService, FeedService feedService,
			SitemapService sitemapService, LuceneService luceneSerivce,
			GitService gitService, URLChecker urlChecker) {

		this.fileService = fileService;
		this.feedService = feedService;
		this.sitemapService = sitemapService;
		this.luceneSerivce = luceneSerivce;
		this.urlChecker = urlChecker;
		this.gitService = gitService;

		this.executorService = Executors.newSingleThreadExecutor();
	}

	@PreDestroy
	public void destroy() {
		this.executorService.shutdown();
	}

	@PostConstruct
	public void setup() {
		boolean cloned = this.gitService.cloneRepositoryIfNotExists();
		Application.logger.info("Posts cloned: {}", cloned);

		if (cloned) {
			Application.logger.info("Generate All");
			generateAll();
		}
		else {
			List<GitChange> changes = this.gitService.pull();
			if (!changes.isEmpty()) {
				changes.forEach(c -> Application.logger.info("Git Change: {}", c));
				handleChanges(changes);
			}
			else if (this.luceneSerivce.hasIndex()) {
				Application.logger.info("Has Index");
				List<PostMetadata> publishedPosts = this.luceneSerivce.getAll();
				if (!this.feedService.filesExists()) {
					Application.logger.info("Generate Feeds");
					this.feedService.writeFeeds(publishedPosts);
				}
				if (!this.sitemapService.fileExists()) {
					Application.logger.info("Generate SiteMap");
					this.sitemapService.writeSitemap(publishedPosts);
					this.sitemapService.pingSearchEngines();
				}

				if (!this.urlChecker.reportExists()) {
					Application.logger.info("Generate URL Check Report");
					this.executorService
							.submit(() -> this.urlChecker.checkURLs(publishedPosts));
				}
			}
			else {
				Application.logger.info("No Index. Generate All");
				generateAll();
			}
		}
	}

	private void handleChanges(List<GitChange> changes) {
		Set<String> deletedFiles = new HashSet<>();
		Set<String> changedOrNewFiles = new HashSet<>();

		for (GitChange gitChange : changes) {
			switch (gitChange.getChangeType()) {
			case DELETE:
				if (isMdFile(gitChange.getOldPath())) {
					deletedFiles.add(gitChange.getOldPath());
				}
				break;
			case COPY:
				if (isMdFile(gitChange.getNewPath())) {
					changedOrNewFiles.add(gitChange.getNewPath());
				}
				break;
			case RENAME:
				if (isMdFile(gitChange.getOldPath())) {
					deletedFiles.add(gitChange.getOldPath());
				}
				if (isMdFile(gitChange.getNewPath())) {
					changedOrNewFiles.add(gitChange.getNewPath());
				}
				break;
			case MODIFY:
				if (isMdFile(gitChange.getNewPath())) {
					changedOrNewFiles.add(gitChange.getNewPath());
				}
				break;
			case ADD:
				if (isMdFile(gitChange.getNewPath())) {
					changedOrNewFiles.add(gitChange.getNewPath());
				}
				break;
			default: /* nothing here */
			}
		}

		this.fileService.deleteHtml(deletedFiles);
		this.luceneSerivce.delete(deletedFiles);

		List<PostContent> changedPosts = this.fileService
				.regenerateHtml(changedOrNewFiles);
		this.luceneSerivce.index(changedPosts);

		// only write feeds and sitemap if a published post changed
		if (changedPosts.stream().anyMatch(post -> !post.getMetadata().isDraft())) {
			Application.logger.info("Write Feeds and Sitemap");
			writeFeedsAndSitemap();
		}
	}

	private static boolean isMdFile(String file) {
		return file.toLowerCase().endsWith(".md");
	}

	private void generateAll() {
		List<PostContent> allPosts = this.fileService.collectAndReadPosts();
		allPosts.forEach(this.fileService::generateHtml);
		this.luceneSerivce.deleteIndex();
		this.luceneSerivce.index(allPosts);

		writeFeedsAndSitemap();
	}

	private void writeFeedsAndSitemap() {
		List<PostMetadata> publishedPostMetadata = this.luceneSerivce.getAll();
		this.feedService.writeFeeds(publishedPostMetadata);
		this.sitemapService.writeSitemap(publishedPostMetadata);
		this.sitemapService.pingSearchEngines();

		if (!this.urlChecker.reportExists()) {
			this.executorService
					.submit(() -> this.urlChecker.checkURLs(publishedPostMetadata));
		}
	}

	@Scheduled(cron = "0 0 10,22 * * *")
	public void checkGit() {
		Application.logger.info("Checking git");
		setup();
	}

	@Scheduled(cron = "0 0 2 1 * *")
	public void checkURLs() {
		Application.logger.info("Checking URLs");
		if (this.luceneSerivce.hasIndex()) {
			List<PostMetadata> publishedPosts = this.luceneSerivce.getAll();
			this.executorService.submit(() -> this.urlChecker.checkURLs(publishedPosts));
		}
	}

}
