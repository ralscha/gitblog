package ch.rasc.gitblog.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class MarkdownFileCollector extends SimpleFileVisitor<Path> {

	private final List<Path> mdFiles;

	public MarkdownFileCollector() {
		this.mdFiles = new ArrayList<>();
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			throws IOException {
		if (file.getFileName().toString().toLowerCase().endsWith(".md")) {
			this.mdFiles.add(file);
		}
		return FileVisitResult.CONTINUE;
	}

	public List<Path> getMdFiles() {
		return this.mdFiles;
	}

}
