package ch.rasc.gitblog.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.hashids.Hashids;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import ch.rasc.gitblog.AppProperties;

@Controller
public class FeedbackController {

	private final Template feedbackTemplate;

	private final Template feedbackOkTemplate;

	private final JavaMailSender mailSender;

	private final AppProperties appProperties;

	private final ExecutorService executorService;

	private final Hashids hashids;

	public FeedbackController(Mustache.Compiler mustacheCompiler,
			JavaMailSender mailSender, AppProperties appProperties) throws IOException {

		ClassPathResource cpr = new ClassPathResource("/templates/feedback.mustache");
		try (InputStream is = cpr.getInputStream();
				InputStreamReader isr = new InputStreamReader(is,
						StandardCharsets.UTF_8);) {
			this.feedbackTemplate = mustacheCompiler.compile(isr);
		}

		cpr = new ClassPathResource("/templates/feedback_ok.mustache");
		try (InputStream is = cpr.getInputStream();
				InputStreamReader isr = new InputStreamReader(is,
						StandardCharsets.UTF_8);) {
			this.feedbackOkTemplate = mustacheCompiler.compile(isr);
		}

		this.mailSender = mailSender;
		this.appProperties = appProperties;
		this.executorService = Executors.newSingleThreadExecutor();

		this.hashids = new Hashids("golb feedback");
	}

	@PreDestroy
	public void destroy() {
		this.executorService.shutdown();
	}

	@PostMapping("/submitFeedback")
	public ResponseEntity<?> submitFeedback(
			@RequestParam(name = "url", required = false) String url,
			@RequestParam(name = "token", required = false) String token,
			@RequestParam(name = "feedback", required = false) String feedbackStr,
			@RequestParam(name = "email", required = false) String email,
			@RequestParam(name = "name", required = false) String nameHoney) {

		if (StringUtils.hasText(feedbackStr) && StringUtils.hasText(url)
				&& StringUtils.hasText(token) && !StringUtils.hasText(nameHoney)) {
			long[] numbers = this.hashids.decode(token);
			long twoSecondsAgo = System.currentTimeMillis() - 2_000;
			if (numbers.length == 1 && numbers[0] < twoSecondsAgo) {
				this.executorService.submit(() -> {
					SimpleMailMessage mailMessage = new SimpleMailMessage();
					mailMessage.setFrom(this.appProperties.getFeedbackFromEmail());
					mailMessage.setTo(this.appProperties.getFeedbackToEmail());

					if (StringUtils.hasText(email)) {
						mailMessage.setReplyTo(email);
					}
					mailMessage.setSubject("Feedback: " + url);
					mailMessage.setText(feedbackStr);

					this.mailSender.send(mailMessage);
				});
			}
		}

		String feedbackOkHtml = this.feedbackOkTemplate.execute(null);

		return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
				.cacheControl(CacheControl.noCache()).body(feedbackOkHtml);
	}

	@GetMapping("/feedback/{url}")
	public ResponseEntity<?> feedback(@PathVariable("url") String url) {

		String feedbackHtml = this.feedbackTemplate.execute(new Object() {
			@SuppressWarnings("unused")
			String token = FeedbackController.this.hashids
					.encode(System.currentTimeMillis());
		});

		return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
				.cacheControl(CacheControl.noCache()).body(feedbackHtml);

	}
}
