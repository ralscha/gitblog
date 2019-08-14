package ch.rasc.gitblog.component;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import ch.rasc.gitblog.Application;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Component
public class GitHubCodeService {
	private final String gitHubLink = "https://github.com/";
	private final String gitHubRawLink = "https://raw.githubusercontent.com/";

	private final OkHttpClient httpClient;

	private final Cache<String, String> siteCache;

	private final Pattern githubPattern;

	public GitHubCodeService() {
		this.httpClient = new OkHttpClient();

		this.siteCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
				.maximumSize(100).build();

		String prefix = Pattern.quote("[github:" + this.gitHubLink);
		String regex = prefix + "((.*?)(?:#L([0-9]+)(?:-L([0-9]+))??)??)(?::(.*?))??\\]";
		this.githubPattern = Pattern.compile(regex);
	}

	public String insertCode(String markdown) {
		Matcher matcher = this.githubPattern.matcher(markdown);

		StringBuilder sb = new StringBuilder(markdown.length());

		while (matcher.find()) {

			String completeUrl = matcher.group(1);
			String url = matcher.group(2);

			String fromStr = matcher.group(3);
			String toStr = matcher.group(4);
			Integer from = null;
			Integer to = null;
			
			if (fromStr != null) {
				from = Integer.valueOf(fromStr);
			}
			
			if (toStr != null) {				
				to = Integer.valueOf(toStr);
			}
			
			if (from != null && to == null) {
				to = from;
			}

			String language = matcher.group(5);
			if (language == null) {
				int lastDot = url.lastIndexOf(".");
				if (lastDot != -1) {
					language = url.substring(lastDot + 1);
				}
			}

			url = url.replace("/blob", "");
			String code = fetchCode(this.gitHubRawLink + url);
			if (code != null) {
				String replacementCode = code;
				if (from != null && to != null) {
					String[] lines = getLines(code, from, to);
					replacementCode = String.join("\n", lines);
				}
				replacementCode = replacementCode.replace("\t", "  ");

				StringBuilder replacement = new StringBuilder(
						replacementCode.length() + 12);
				replacement.append("\n```");
				replacement.append(language);
				replacement.append("\n");
				replacement.append(replacementCode);
				replacement.append("\n");
				replacement.append("```\n");

				replacement.append("<small class=\"gh\">");

				String fileName = url;
				int lastSlash = url.lastIndexOf("/");
				if (lastSlash != -1) {
					fileName = url.substring(lastSlash + 1);
				}
				replacement.append("[");
				replacement.append(fileName);
				replacement.append("]");

				replacement.append("(");
				replacement.append(this.gitHubLink + completeUrl);
				replacement.append(")");
				replacement.append("</small>\n");

				matcher.appendReplacement(sb,
						Matcher.quoteReplacement(replacement.toString()));

			}
		}
		matcher.appendTail(sb);

		return sb.toString();
	}

	private static String[] getLines(String code, int from, int to) {
		String lines[] = code.split("\\r?\\n");
		if (from <= lines.length && to <= lines.length) {
			return Arrays.copyOfRange(lines, from - 1, to);
		}
		return lines;
	}

	private String fetchCode(String url) {
		String code = this.siteCache.getIfPresent(url);
		if (code == null) {
			Request request = new Request.Builder().url(url).build();
			try (Response response = this.httpClient.newCall(request).execute()) {
				try (ResponseBody responseBody = response.body()) {
					if (responseBody != null) {
						String body = responseBody.string();
						this.siteCache.put(url, body);
						return body;
					}
				}
			}
			catch (IOException e) {
				Application.logger.error("fetchCode", e);
			}
		}

		return code;
	}
}
