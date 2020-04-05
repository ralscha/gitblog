package ch.rasc.gitblog.component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import ch.rasc.gitblog.dto.GitChange;

@Component
public class GitService {

	private final AppProperties appProperties;

	public GitService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public boolean cloneRepositoryIfNotExists() {
		if (!Files.exists(Paths.get(this.appProperties.getWorkDir()))) {
			try {
				Files.createDirectories(Paths.get(this.appProperties.getWorkDir()));
			}
			catch (IOException e) {
				Application.logger.error("create workdir", e);
			}

			CloneCommand gitCommand = Git.cloneRepository()
					.setURI(this.appProperties.getGitRepository())
					.setDirectory(Paths.get(this.appProperties.getWorkDir()).toFile());

			if (StringUtils.hasText(this.appProperties.getGitRepositoryUser())) {
				UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(
						this.appProperties.getGitRepositoryUser(),
						this.appProperties.getGitRepositoryPassword());

				gitCommand.setCredentialsProvider(credentialsProvider);
			}

			try (Git result = gitCommand.call()) {
				return true;
			}
			catch (GitAPIException e) {
				Application.logger.error("clone repository", e);
			}
		}

		return false;
	}

	public List<GitChange> pull() {
		List<GitChange> changes = new ArrayList<>();
		try (Git git = Git.open(Paths.get(this.appProperties.getWorkDir()).toFile());
				Repository repository = git.getRepository();
				ObjectReader reader = repository.newObjectReader()) {

			ObjectId oldHead = repository.resolve("HEAD^{tree}");

			if (StringUtils.hasText(this.appProperties.getGitRepositoryUser())) {
				git.pull()
						.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
								this.appProperties.getGitRepositoryUser(),
								this.appProperties.getGitRepositoryPassword()))
						.call();
			}
			else {
				git.pull().call();
			}

			ObjectId head = repository.resolve("HEAD^{tree}");

			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, oldHead);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, head);
			List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter)
					.setOldTree(oldTreeIter).call();

			for (DiffEntry entry : diffs) {
				changes.add(new GitChange(entry.getChangeType(), entry.getNewPath(),
						entry.getOldPath()));
			}

		}
		catch (IOException | GitAPIException e) {
			Application.logger.error("pull", e);
		}
		return changes;
	}

}
