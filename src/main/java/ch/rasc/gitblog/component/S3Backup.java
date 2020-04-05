package ch.rasc.gitblog.component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3Backup {

	private final AppProperties appProperties;

	public S3Backup(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	@Scheduled(cron = "0 0 12 * * *")
	public void backup() {
		try {
			Path tmpDir = Files.createTempDirectory("gitblog");
			Path zipFile = Files.createTempFile("gitblogzip", ".zip");

			doBackup(tmpDir, zipFile);

			try {
				Files.deleteIfExists(zipFile);
			}
			catch (IOException e) {
				Application.logger.info("backup", e);
			}

			try {
				FileSystemUtils.deleteRecursively(tmpDir);
			}
			catch (IOException e) {
				Application.logger.info("delete", e);
			}
		}
		catch (IOException e) {
			Application.logger.error("backup", e);
		}
	}

	private void doBackup(Path tmpDir, Path zipFile) {

		try {
			bareClone(tmpDir);
			zip(tmpDir, zipFile);
		}
		catch (IllegalStateException | IOException | GitAPIException e) {
			Application.logger.error("backup", e);
		}

		if (this.appProperties.getAwsAccessKey() != null
				&& this.appProperties.getAwsSecretKey() != null
				&& this.appProperties.getAwsBucket() != null) {
			try (S3Client s3Client = S3Client.builder().region(Region.US_EAST_1)
					.credentialsProvider(
							StaticCredentialsProvider.create(AwsBasicCredentials.create(
									this.appProperties.getAwsAccessKey(),
									this.appProperties.getAwsSecretKey())))
					.build()) {

				PutObjectRequest request = PutObjectRequest.builder()
						.bucket(this.appProperties.getAwsBucket())
						.key(LocalDateTime.now()
								.format(DateTimeFormatter.ofPattern(
										"yyyy-MM-dd'T'HH_mm_ss", Locale.ENGLISH))
								+ ".zip")
						.build();
				s3Client.putObject(request, zipFile);
			}
		}
	}

	private static void zip(Path tmpDir, Path zipFile) throws IOException {
		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFile))) {
			Files.walk(tmpDir).filter(path -> !Files.isDirectory(path)).forEach(path -> {
				ZipEntry zipEntry = new ZipEntry(tmpDir.relativize(path).toString());
				try {
					zs.putNextEntry(zipEntry);
					Files.copy(path, zs);
					zs.closeEntry();
				}
				catch (IOException e) {
					Application.logger.error("zip", e);
				}
			});
		}
	}

	private void bareClone(Path tmpDir) throws IOException, InvalidRemoteException,
			TransportException, IllegalStateException, GitAPIException {
		Files.createDirectories(tmpDir);

		try (Git git = Git.cloneRepository().setBare(true)
				.setURI(this.appProperties.getGitRepository())
				.setDirectory(tmpDir.toFile())
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
						this.appProperties.getGitRepositoryUser(),
						this.appProperties.getGitRepositoryPassword()))
				.call()) {
			git.getRepository().close();
		}
	}

}
