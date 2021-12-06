package ch.rasc.gitblog.web;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.rasc.gitblog.AppProperties;
import ch.rasc.gitblog.service.MainService;

@RestController
public class GiteaWebhook {
	private final Mac mac;

	private final MainService mainService;

	public GiteaWebhook(AppProperties appProperties, MainService mainService)
			throws InvalidKeyException, NoSuchAlgorithmException {
		SecretKeySpec keySpec = new SecretKeySpec(
				appProperties.getWebhookSecret().getBytes(), "HmacSHA1");

		this.mac = Mac.getInstance("HmacSHA1");
		this.mac.init(keySpec);
		this.mainService = mainService;
	}

	@PostMapping("/webhook")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void handleWebhook(@RequestHeader("X-Hub-Signature") String signature,
			@RequestBody String body) {

		byte[] result = this.mac.doFinal(body.getBytes());

		StringBuilder sb = new StringBuilder();
		for (byte b : result) {
			sb.append(String.format("%02x", b));
		}
		String computedSignature = "sha1=" + sb.toString();

		if (signature.equals(computedSignature)) {
			this.mainService.setup();
		}

	}
}
