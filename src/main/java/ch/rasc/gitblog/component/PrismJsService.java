package ch.rasc.gitblog.component;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;

@Component
public class PrismJsService {
	private final Path prismJsDir;

	private final Path prismComponentsDir;
	private final Path prismCoreJs;

	private final Set<String> builtin = new HashSet<>();
	private final Map<String, String> aliases = new HashMap<>();

	private ScriptEngine engine;

	public PrismJsService(AppProperties appProperties) {
		this.prismJsDir = Paths.get(appProperties.getPrismJsWorkdir(),
				appProperties.getPrismJsVersion());

		this.prismComponentsDir = this.prismJsDir.resolve("components");

		this.prismCoreJs = this.prismJsDir.resolve("prism.js");

		this.builtin.add("markup");
		this.builtin.add("xml");
		this.builtin.add("html");
		this.builtin.add("mathml");
		this.builtin.add("svg");
		this.builtin.add("css");
		this.builtin.add("clike");
		this.builtin.add("javascript");

		this.aliases.put("js", "javascript");
		this.aliases.put("py", "python");
		this.aliases.put("rb", "ruby");
		this.aliases.put("ps1", "powershell");
		this.aliases.put("psm1", "powershell");
		this.aliases.put("sh", "bash");
		this.aliases.put("bat", "batch");
		this.aliases.put("h", "c");
		this.aliases.put("tex", "latex");
		this.aliases.put("ts", "typescript");
		this.aliases.put("kt", "kotlin");
		this.aliases.put("proto", "protobuf");

		downloadIfNotExists(appProperties.getPrismJsDownloadUrl());

		try {
			this.engine = new ScriptEngineManager().getEngineByName("graal.js");
			try (FileReader fr = new FileReader(this.prismCoreJs.toFile())) {
				this.engine.eval(fr);
			}
		}
		catch (ScriptException | IOException e) {
			Application.logger.error("set up prismjs", e);
		}
	}

	public String prism(String html) {
		Document doc = Jsoup.parse(html);
		Elements codeElements = doc.select("code[class*=\"language-\"]");
		for (Element codeElement : codeElements) {
			String lang = "markup";
			for (String cl : codeElement.classNames()) {
				if (cl.startsWith("language-")) {
					lang = cl.substring("language-".length());
				}
			}
			codeElement.html(prism(codeElement.wholeText(), lang));
		}
		return doc.body().html();
	}

	private String prism(String code, String language) {
		try {
			String lang = this.aliases.get(language);
			if (lang == null) {
				lang = language;
			}

			if (!this.builtin.contains(lang)) {
				Path componentFile = this.prismComponentsDir
						.resolve("prism-" + lang + ".js");
				if (Files.exists(componentFile)) {
					try (FileReader fr = new FileReader(componentFile.toFile())) {
						this.engine.eval(fr);
					}
					this.engine.eval("var lang = Prism.languages." + lang);
				}
				else {
					this.engine.eval("var lang = Prism.languages.markup");
				}
			}
			else {
				this.engine.eval("var lang = Prism.languages." + lang);
			}

			Invocable invocable = (Invocable) this.engine;

			Object result = invocable.invokeMethod(this.engine.get("Prism"), "highlight",
					code, this.engine.get("lang"));
			return (String) result;
		}
		catch (NoSuchMethodException | ScriptException | IOException e) {
			Application.logger.error("prism", e);
			return null;
		}
	}

	private void downloadIfNotExists(String downloadURL) {
		Path parent = this.prismJsDir.getParent();
		if (!Files.exists(parent)) {
			try {
				Files.createDirectories(parent);
			}
			catch (IOException e) {
				Application.logger.error("set up prism js", e);
			}

			// download and unzip
			OkHttpClient httpClient = new OkHttpClient();
			Request request = new Request.Builder().url(downloadURL).build();
			Path downloadedFile = parent.resolve("tmp.zip");
			try (Response response = httpClient.newCall(request).execute()) {
				try (Sink downloadSink = Okio.sink(downloadedFile);
						BufferedSink sink = Okio.buffer(downloadSink)) {
					try (ResponseBody body = response.body()) {
						if (body != null) {
							try (BufferedSource source = body.source()) {
								sink.writeAll(source);
							}
						}
					}
				}
			}
			catch (IOException e) {
				Application.logger.error("fetchCode", e);
			}

			try (InputStream is = Files.newInputStream(downloadedFile);
					ZipInputStream zis = new ZipInputStream(is)) {

				ZipEntry zipEntry = zis.getNextEntry();
				while (zipEntry != null) {
					String fileName = zipEntry.getName();
					Path newFile = parent.resolve(fileName);
					if (zipEntry.isDirectory()) {
						Files.createDirectories(newFile);
					}
					else {
						Files.copy(zis, newFile);
					}
					zipEntry = zis.getNextEntry();
				}
				zis.closeEntry();
				zis.close();

				Files.deleteIfExists(downloadedFile);
			}
			catch (IOException e) {
				Application.logger.error("fetchCode", e);
			}

		}
	}
}
