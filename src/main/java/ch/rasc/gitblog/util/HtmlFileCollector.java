package ch.rasc.gitblog.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class HtmlFileCollector extends SimpleFileVisitor<Path> {

	private final List<Path> htmlFiles;

	public HtmlFileCollector() {
		this.htmlFiles = new ArrayList<>();
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			throws IOException {
		if (file.getFileName().toString().toLowerCase().endsWith(".html")) {
			this.htmlFiles.add(file);
		}
		return FileVisitResult.CONTINUE;
	}

	public List<Path> getHtmlFiles() {
		return this.htmlFiles;
	}

}
