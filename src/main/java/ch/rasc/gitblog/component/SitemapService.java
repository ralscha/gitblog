package ch.rasc.gitblog.component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.redfin.sitemapgenerator.WebSitemapGenerator;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import ch.rasc.gitblog.dto.PostMetadata;
import ch.rasc.gitblog.service.FileService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class SitemapService {

	private final AppProperties appProperties;

	public SitemapService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public boolean fileExists() {
		Path workDir = Paths.get(this.appProperties.getWorkDir());
		return Files.exists(workDir.resolve("sitemap.xml"));
	}

	public void writeSitemap(List<PostMetadata> posts) {
		String baseURL = this.appProperties.getBaseUrl();
		Path workDir = Paths.get(this.appProperties.getWorkDir());
		try {
			WebSitemapGenerator wsg = new WebSitemapGenerator(baseURL);
			wsg.addUrl(baseURL + "index.html");

			for (PostMetadata post : posts) {
				wsg.addUrl(baseURL + post.getUrl());
			}

			String result = wsg.writeAsStrings().stream()
					.collect(Collectors.joining("\n"));
			Path sitemapPath = workDir.resolve("sitemap.xml");
			Files.write(sitemapPath, result.getBytes(StandardCharsets.UTF_8));

			FileService.gzip(sitemapPath);
			FileService.brotli(this.appProperties.getBrotliCmd(), sitemapPath);
		}
		catch (IOException e) {
			Application.logger.error("writeSitemap", e);
		}
	}

	public void pingSearchEngines() {
		String baseURL = this.appProperties.getBaseUrl();
		String sitemapUrl = baseURL + "sitemap.xml";
		OkHttpClient httpClient = new OkHttpClient();

		HttpUrl googlePingUrl = new HttpUrl.Builder().scheme("https").host("google.com")
				.addPathSegment("ping").addQueryParameter("sitemap", sitemapUrl).build();

		HttpUrl bingPingUrl = new HttpUrl.Builder().scheme("https").host("www.bing.com")
				.addPathSegment("webmaster").addPathSegment("ping.aspx")
				.addQueryParameter("siteMap", sitemapUrl).build();

		ping(httpClient, googlePingUrl);
		ping(httpClient, bingPingUrl);
	}

	private static void ping(OkHttpClient httpClient, HttpUrl pingUrl) {
		Request request = new Request.Builder().url(pingUrl).build();
		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				Application.logger.error("sitemap controller", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (!response.isSuccessful()) {
					Application.logger.error("Unexpected code " + response);
				}
			}
		});
	}

}
