package br.gravita.adapters.outbound.messaging;

import org.springframework.web.util.HtmlUtils;

/**
 * Renders the activation e-mail. Inline CSS and table layout on purpose: mail clients ignore {@code <style>}
 * blocks and flexbox. The brand constants are placeholders until the design system provides the real logo.
 */
final class ActivationEmailTemplate {

	static final String SUBJECT = "Ative sua conta Gravita";

	private static final String BRAND_NAME = "Gravita";
	private static final String BRAND_PRIMARY_COLOR = "#4F6EF7";
	private static final String BRAND_DARK_COLOR = "#1A1F2E";

	private static final String HTML = """
			<!DOCTYPE html>
			<html lang="pt-BR">
			<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>{{subject}}</title></head>
			<body style="margin:0;padding:0;background-color:#F3F4F6;font-family:Arial,Helvetica,sans-serif;color:#1F2937;">
			<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#F3F4F6;padding:24px 0;">
			<tr><td align="center">
			<table role="presentation" width="560" cellpadding="0" cellspacing="0"
				style="max-width:560px;width:100%;background-color:#FFFFFF;border-radius:8px;overflow:hidden;">
			<tr><td style="background-color:{{dark}};padding:24px 32px;font-size:24px;font-weight:bold;color:#FFFFFF;">
			<!-- Branding placeholder: swap the name for the logo image once the asset is hosted. -->
			{{brand}}
			</td></tr>
			<tr><td style="padding:32px;font-size:16px;line-height:24px;">
			<p style="margin:0 0 16px 0;font-size:20px;font-weight:bold;">Olá, {{name}}!</p>
			<p style="margin:0 0 24px 0;">Falta só um passo para começar a usar a {{brand}}: confirme o seu e-mail para ativar a sua conta.</p>
			<p style="margin:0 0 24px 0;"><a href="{{link}}" style="display:inline-block;background-color:{{primary}};color:#FFFFFF;text-decoration:none;font-weight:bold;padding:14px 28px;border-radius:6px;">Ativar minha conta</a></p>
			<p style="margin:0 0 8px 0;font-weight:bold;">Este link expira em {{hours}} horas.</p>
			<p style="margin:0;font-size:14px;color:#6B7280;">Se você não criou uma conta na {{brand}}, ignore esta mensagem.</p>
			</td></tr>
			</table>
			</td></tr>
			</table>
			</body>
			</html>
			""";

	private static final String TEXT = """
			Olá, {{name}}!

			Falta só um passo para começar a usar a {{brand}}: confirme o seu e-mail para ativar a sua conta.

			Ativar minha conta: {{link}}

			Este link expira em {{hours}} horas.

			Se você não criou uma conta na {{brand}}, ignore esta mensagem.
			""";

	record Rendered(String subject, String html, String plainText) {
	}

	private ActivationEmailTemplate() {
	}

	static Rendered render(final String recipientName, final String activationLink, final long validityHours) {
		final String hours = String.valueOf(validityHours);
		final String html = HTML
				.replace("{{subject}}", SUBJECT)
				.replace("{{dark}}", BRAND_DARK_COLOR)
				.replace("{{primary}}", BRAND_PRIMARY_COLOR)
				.replace("{{brand}}", BRAND_NAME)
				.replace("{{hours}}", hours)
				.replace("{{name}}", HtmlUtils.htmlEscape(recipientName))
				.replace("{{link}}", HtmlUtils.htmlEscape(activationLink));
		final String text = TEXT
				.replace("{{brand}}", BRAND_NAME)
				.replace("{{hours}}", hours)
				.replace("{{name}}", recipientName)
				.replace("{{link}}", activationLink);
		return new Rendered(SUBJECT, html, text);
	}
}
