package ch.rasc.gitblog.component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import ch.rasc.gitblog.dto.PostContent;
import ch.rasc.gitblog.dto.PostMetadata;
import jakarta.annotation.PreDestroy;

@Component
public class LuceneService {
	private final MarkdownService markdownService;

	private final Directory directory;

	private final Analyzer analyzer;

	private final AppProperties appProperties;

	private final Set<Integer> publishedYears = new TreeSet<>();

	public LuceneService(AppProperties appProperties, MarkdownService markdownService)
			throws IOException {
		this.markdownService = markdownService;
		this.analyzer = new StandardAnalyzer();
		this.appProperties = appProperties;

		Path luceneDir = Paths.get(appProperties.getLuceneDir());
		Files.createDirectories(luceneDir);
		this.directory = new NIOFSDirectory(luceneDir);

		this.publishedYears
				.addAll(this.getAll().stream().map(post -> post.getPublished().getYear())
						.distinct().collect(Collectors.toList()));
	}

	@PreDestroy
	public void destroy() {
		if (this.directory != null) {
			try {
				this.directory.close();
			}
			catch (IOException e) {
				Application.logger.error("close lucene directory", e);
			}
		}
	}

	public boolean hasIndex() {
		try {
			Path luceneDir = Paths.get(this.appProperties.getLuceneDir());
			return Files.list(luceneDir).count() > 1;
		}
		catch (IOException e) {
			Application.logger.error("hasIndex", e);
		}
		return false;
	}

	public void deleteIndex() {
		this.publishedYears.clear();

		IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
		try (IndexWriter indexWriter = new IndexWriter(this.directory, config)) {
			indexWriter.deleteAll();
			indexWriter.commit();
		}
		catch (IOException e) {
			Application.logger.error("indexAll", e);
		}
	}

	public void delete(Set<String> urls) {
		if (urls.isEmpty()) {
			return;
		}

		IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
		try (IndexWriter indexWriter = new IndexWriter(this.directory, config)) {
			for (String url : urls) {
				Term keyTerm = new Term("url", url.replace(".md", ".html"));
				indexWriter.deleteDocuments(keyTerm);
			}
			indexWriter.commit();
		}
		catch (IOException e) {
			Application.logger.error("indexAll", e);
		}
	}

	public void index(List<PostContent> posts) {
		if (posts.isEmpty()) {
			return;
		}

		IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
		try (IndexWriter indexWriter = new IndexWriter(this.directory, config)) {

			for (PostContent post : posts) {
				if (!post.getMetadata().isDraft()) {

					this.publishedYears.add(post.getMetadata().getPublished().getYear());

					PostMetadata metadata = post.getMetadata();
					String text = this.markdownService.renderText(post.getMarkdown());

					Document doc = new Document();
					doc.add(new TextField("body", text, Field.Store.NO));
					doc.add(new TextField("body", metadata.getTitle(), Field.Store.NO));

					if (StringUtils.hasText(metadata.getSummary())) {
						doc.add(new TextField("summary", metadata.getSummary(),
								Field.Store.YES));
						doc.add(new TextField("body", metadata.getSummary(),
								Field.Store.NO));
					}
					doc.add(new TextField("title", metadata.getTitle(), Field.Store.YES));
					doc.add(new StringField("url", metadata.getUrl(), Field.Store.YES));
					doc.add(new StringField("path", metadata.getMdFile().toString(),
							Field.Store.YES));

					long publishedEpochSeconds = metadata.getPublished().toEpochSecond();
					doc.add(new NumericDocValuesField("publishedts",
							publishedEpochSeconds));
					doc.add(new StoredField("published", publishedEpochSeconds));

					doc.add(new NumericDocValuesField("publishedyear",
							metadata.getPublished().getYear()));

					if (metadata.getUpdated() != null) {
						doc.add(new StoredField("updated",
								metadata.getUpdated().toEpochSecond()));
					}

					for (String tag : metadata.getTags()) {
						doc.add(new StringField("tags", tag, Field.Store.YES));
					}

					try {
						Term keyTerm = new Term("url", metadata.getUrl());
						indexWriter.updateDocument(keyTerm, doc);
					}
					catch (IOException e) {
						Application.logger.error("indexAll", e);
					}
				}
			}

			indexWriter.commit();

		}
		catch (IOException e) {
			Application.logger.error("indexAll", e);
		}

	}

	public List<PostMetadata> getAll() {
		MatchAllDocsQuery query = new MatchAllDocsQuery();
		return search(query);
	}

	public List<PostMetadata> getPostsOfYear(int year) {
		return search(NumericDocValuesField.newSlowExactQuery("publishedyear", year));
	}

	public List<PostMetadata> searchWithTag(String tag) {
		TermQuery query = new TermQuery(new Term("tags", tag));
		return search(query);
	}

	public List<PostMetadata> searchWithQuery(String query) {
		try {
			Query q = new QueryParser("body", this.analyzer).parse(query);
			return search(q);
		}
		catch (ParseException e) {
			Application.logger.error("search", e);
			return Collections.emptyList();
		}
	}

	private List<PostMetadata> search(Query query) {
		List<PostMetadata> posts = new ArrayList<>();
		try (IndexReader reader = DirectoryReader.open(this.directory)) {
			IndexSearcher indexSearcher = new IndexSearcher(reader);

			TopFieldDocs topDocs = indexSearcher.search(query, 1000,
					new Sort(new SortField("publishedts", SortField.Type.LONG, true)));

			StoredFields storedFields = indexSearcher.storedFields();
			for (ScoreDoc sd : topDocs.scoreDocs) {
				int docId = sd.doc;
				Document doc = storedFields.document(docId);
				posts.add(new PostMetadata(doc));
			}
		}
		catch (IOException e) {
			Application.logger.error("search: " + e.getMessage());
		}
		return posts;
	}

	public Set<Integer> getPublishedYears() {
		return Collections.unmodifiableSet(this.publishedYears);
	}

}
