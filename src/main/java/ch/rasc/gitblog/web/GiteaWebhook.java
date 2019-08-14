package ch.rasc.gitblog.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.Application;
import ch.rasc.gitblog.service.MainService;

@RestController
public class GiteaWebhook {

	private final ObjectMapper objectMapper;

	private final String secret;

	private final MainService mainService;

	public GiteaWebhook(ObjectMapper objectMapper, AppProperties appProperties,
			MainService mainService) {
		this.objectMapper = objectMapper;
		this.secret = appProperties.getWebhookSecret();
		this.mainService = mainService;
	}

	@PostMapping("/webhook")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void handleWebhook(
			@SuppressWarnings("unused") @RequestHeader("X-Gitea-Delivery") String delivery,
			@RequestBody String body) {

		try {
			Map<String, Object> json = this.objectMapper.readValue(body, Map.class);
			String sentSecret = (String) json.get("secret");
			if (this.secret.equals(sentSecret)) {
				this.mainService.setup();
			}
		}
		catch (Exception e) {
			Application.logger.error("handle webhook", e);
		}

	}
}
