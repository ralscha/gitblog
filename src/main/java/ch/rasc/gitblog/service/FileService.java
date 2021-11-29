package ch.rasc.gitblog.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.Deflater;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import ch.rasc.gitblog.component.GitHubCodeService;
import ch.rasc.gitblog.component.MarkdownService;
import ch.rasc.gitblog.component.PrismJsService;
import ch.rasc.gitblog.dto.PostContent;
import ch.rasc.gitblog.dto.PostHeader;
import ch.rasc.gitblog.dto.PostMetadata;
import ch.rasc.gitblog.util.MarkdownFileCollector;
import ch.rasc.gitblog.util.MyGZIPOutputStream;

@Service
public class FileService {

	final Path workDir;

	private final Pattern dirPattern = Pattern.compile("\\d{4}");

	private final GitHubCodeService gitHubCodeService;

	private final Yaml yaml;

	private final MarkdownService markdownService;

	private final PrismJsService prismJsService;

	private final String brotliCmd;

	private Template postTemplate;

	private final Pattern headerPattern = Pattern.compile("---(.*?)---(.*)",
			Pattern.DOTALL);

	public FileService(AppProperties appProperties, MarkdownService markdownService,
			GitHubCodeService gitHubCodeService, PrismJsService prismJsService,
			Mustache.Compiler mustacheCompiler) {
		this.workDir = Paths.get(appProperties.getWorkDir());
		this.brotliCmd = appProperties.getBrotliCmd();
		this.gitHubCodeService = gitHubCodeService;
		this.markdownService = markdownService;
		this.yaml = new Yaml();
		this.prismJsService = prismJsService;

		ClassPathResource cpr = new ClassPathResource("/templates/post.mustache");
		try (InputStream is = cpr.getInputStream();
				InputStreamReader isr = new InputStreamReader(is,
						StandardCharsets.UTF_8);) {
			this.postTemplate = mustacheCompiler.withFormatter(new Mustache.Formatter() {
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
		catch (IOException e) {
			Application.logger.error("init post template", e);
		}
	}

	public void deleteHtml(Set<String> deletedFiles) {
		for (String url : deletedFiles) {
			Path mdFile = this.workDir.resolve(url);
			try {
				Files.deleteIfExists(PostMetadata.siblingPath(mdFile, "html.br"));
				Files.deleteIfExists(PostMetadata.siblingPath(mdFile, "html.gz"));
				Files.deleteIfExists(PostMetadata.siblingPath(mdFile, "html"));
			}
			catch (IOException e) {
				Application.logger.error("delete html file", e);
			}
		}

	}

	public List<PostContent> regenerateHtml(Set<String> changedUrls) {
		List<PostContent> posts = new ArrayList<>();
		for (String url : changedUrls) {
			Path mdFile = this.workDir.resolve(url);

			Application.logger.info("creating html for {}", mdFile);
			PostContent content = readPost(mdFile);
			generateHtml(content);

			posts.add(readPost(mdFile));
		}
		return posts;
	}

	public void generateHtml(PostContent post) {
		try {
			String postHtml = this.postTemplate.execute(post);
			Path htmlFile = PostMetadata.siblingPath(post.getMetadata().getMdFile(),
					"html");
			Files.write(htmlFile, postHtml.getBytes(StandardCharsets.UTF_8));
			gzip(htmlFile);
			brotli(this.brotliCmd, htmlFile);
		}
		catch (IOException e) {
			Application.logger.error("generate", e);
		}
	}

	public List<Path> collectMdFiles() {
		MarkdownFileCollector fileCollector = new MarkdownFileCollector();
		walkFiles(fileCollector);
		return fileCollector.getMdFiles();
	}

	public List<PostContent> collectAndReadPosts() {
		return collectMdFiles().stream().map(this::readPost).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public PostContent readPost(Path mdFile) {
		try {
			// read markdown
			String content = new String(Files.readAllBytes(mdFile),
					StandardCharsets.UTF_8);

			// extract header
			Matcher matcher = this.headerPattern.matcher(content);
			if (!matcher.matches()) {
				// not a valid post, delete an existing html file
				Files.deleteIfExists(PostMetadata.siblingPath(mdFile, "html"));
				return null;
			}

			String headerString = matcher.group(1);
			PostHeader header = this.yaml.loadAs(headerString, PostHeader.class);
			PostMetadata metadata = new PostMetadata(header, this.workDir, mdFile);

			// insert github code
			String markdown = matcher.group(2);
			markdown = this.gitHubCodeService.insertCode(markdown);

			// convert md to html
			String html = this.markdownService.renderHtml(markdown);
			html = this.prismJsService.prism(html);

			return new PostContent(metadata, markdown, html);
		}
		catch (IOException e) {
			Application.logger.error("readPost", e);
		}

		return null;
	}

	public void walkFiles(SimpleFileVisitor<Path> visitor) {

		try {
			Files.list(this.workDir).forEach(rootPath -> {
				if (Files.isDirectory(rootPath)) {
					String dirName = rootPath.getFileName().toString();
					Matcher matcher = this.dirPattern.matcher(dirName);
					if (matcher.matches()) {
						try {
							Files.walkFileTree(rootPath, visitor);
						}
						catch (IOException e) {
							Application.logger.error("collect posts", e);
						}
					}
				}
			});
		}
		catch (IOException e) {
			Application.logger.error("collect posts", e);
		}
	}

	public static void gzip(Path file) {
		Path outfile = file.resolveSibling(file.getFileName().toString() + ".gz");

		try (OutputStream out = Files.newOutputStream(outfile);
				MyGZIPOutputStream gzout = new MyGZIPOutputStream(out)) {
			gzout.setLevel(Deflater.BEST_COMPRESSION);
			Files.copy(file, gzout);
		}
		catch (IOException e) {
			Application.logger.error("gzip", e);
		}
	}

	public static void brotli(String brotliCmd, Path file) {
		if (brotliCmd != null && !brotliCmd.isBlank()) {
			List<String> cmds = new ArrayList<>(List.of(brotliCmd.split(" ")));
			cmds.add(file.toString());
			ProcessBuilder builder = new ProcessBuilder(cmds);

			try {
				Process process = builder.start();
				process.waitFor();
			}
			catch (IOException | InterruptedException e) {
				Application.logger.error("brotli", e);
			}
		}
	}

}
