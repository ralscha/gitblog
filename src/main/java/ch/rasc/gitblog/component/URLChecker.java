package ch.rasc.gitblog.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import ch.rasc.gitblog.dto.PostMetadata;
import ch.rasc.gitblog.dto.URLCheck;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class URLChecker {
	private final OkHttpClient httpClient;

	private final AppProperties appProperties;

	private final Template checkurlTemplate;

	public URLChecker(Mustache.Compiler mustacheCompiler, AppProperties appProperties)
			throws IOException {

		this.httpClient = new OkHttpClient.Builder()
				.hostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				}).followRedirects(false).followSslRedirects(false).build();

		this.appProperties = appProperties;

		ClassPathResource cpr = new ClassPathResource("/templates/urlcheck.mustache");
		try (InputStream is = cpr.getInputStream();
				InputStreamReader isr = new InputStreamReader(is,
						StandardCharsets.UTF_8);) {
			this.checkurlTemplate = mustacheCompiler.compile(isr);
		}
	}

	public boolean reportExists() {
		return Files.exists(Paths.get(this.appProperties.getWorkDir())
				.resolve("report/urlcheck.html"));
	}

	public void checkURLs(List<PostMetadata> posts) {
		Set<String> ignoreUrls = new HashSet<>();
		try {
			Path ignoreUrlsFile = Paths.get(this.appProperties.getIgnoreUrlList());
			if (Files.exists(ignoreUrlsFile)) {
				ignoreUrls.addAll(Files.readAllLines(ignoreUrlsFile));
			}
		}
		catch (IOException e) {
			Application.logger.error("checkURLs read ignore file", e);
		}

		LinkExtractor linkExtractor = LinkExtractor.builder()
				.linkTypes(EnumSet.of(LinkType.URL, LinkType.WWW)).build();

		List<URLCheck> results = new ArrayList<>();

		for (PostMetadata post : posts) {
			try {
				Path htmlFile = PostMetadata.siblingPath(post.getMdFile(), "html");
				String htmlContent = new String(Files.readAllBytes(htmlFile),
						StandardCharsets.UTF_8);

				Set<String> urls = new HashSet<>();
				for (LinkSpan link : linkExtractor.extractLinks(htmlContent)) {
					urls.add(htmlContent.substring(link.getBeginIndex(),
							link.getEndIndex()));
				}
				if (!urls.isEmpty()) {
					List<URLCheck> urlChecks = urls.parallelStream()
							.map(url -> checkUrl(post, url, ignoreUrls))
							.filter(Objects::nonNull).collect(Collectors.toList());
					results.addAll(urlChecks);
				}
			}
			catch (IOException e) {
				Application.logger.error("check url", e);
			}
		}

		String checkHtml = this.checkurlTemplate.execute(new Object() {
			@SuppressWarnings({ "unused" })
			List<URLCheck> checks = results;
		});

		try {
			Path checkPath = Paths.get(this.appProperties.getWorkDir())
					.resolve("report/urlcheck.html");
			Files.createDirectories(checkPath.getParent());

			Files.write(checkPath, checkHtml.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e) {
			Application.logger.error("url check", e);
		}

	}

	private URLCheck checkUrl(PostMetadata post, String url, Set<String> ignoreUrls) {

		Optional<String> ignoredUrl = ignoreUrls.stream()
				.filter(iu -> url.toLowerCase().startsWith(iu.toLowerCase())).findAny();
		if (ignoredUrl.isPresent()) {
			return null;
		}

		try {
			Request request = new Request.Builder().url(url).header("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36")
					.build();

			try (Response response = this.httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					if (response.isRedirect()) {
						String location = response.header("Location");
						return new URLCheck(url, post.getUrl(), response.code(),
								location);
					}
					return new URLCheck(url, post.getUrl(), response.code(), null);
				}
				return null;
			}
			catch (Exception e) {
				Application.logger.info("check url: " + url, e);
			}
		}
		catch (Exception e) {
			Application.logger.info("check url: " + url, e);
		}
		return new URLCheck(url, post.getUrl(), -1, null);

	}
}
