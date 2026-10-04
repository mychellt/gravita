<#-- Variables: username, activationUrl, validityHours (optional) -->
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ative sua conta Gravita</title>
</head>
<body style="margin:0;padding:0;background-color:#F3F4F6;font-family:Arial,Helvetica,sans-serif;color:#1F2937;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#F3F4F6;padding:24px 0;">
    <tr>
        <td align="center">
            <table role="presentation" width="560" cellpadding="0" cellspacing="0"
                   style="max-width:560px;width:100%;background-color:#FFFFFF;border-radius:8px;overflow:hidden;">
                <!-- Header -->
                <tr>
                    <td style="background-color:#1A1F2E;padding:24px 32px;font-size:24px;font-weight:bold;color:#FFFFFF;">
                        Gravita
                    </td>
                </tr>
                <!-- Section title -->
                <tr>
                    <td style="background-color:#EEF1FE;padding:16px 32px;font-size:18px;font-weight:bold;color:#1A1F2E;">
                        Ative sua conta
                    </td>
                </tr>
                <!-- Content -->
                <tr>
                    <td style="padding:32px;font-size:16px;line-height:24px;">
                        <p style="margin:0 0 16px 0;font-size:20px;font-weight:bold;">Olá, ${username?html}!</p>
                        <p style="margin:0 0 24px 0;">
                            Que bom ter você com a gente na Gravita! Falta só um passo para começar a usar a plataforma:
                            confirme o seu e-mail para ativar a sua conta.
                        </p>
                        <p style="margin:0 0 24px 0;text-align:center;">
                            <a href="${activationUrl?html}"
                               style="display:inline-block;background-color:#4F6EF7;color:#FFFFFF;text-decoration:none;font-weight:bold;padding:14px 28px;border-radius:6px;">
                                Ativar minha conta
                            </a>
                        </p>
                        <p style="margin:0 0 8px 0;font-size:14px;color:#6B7280;">
                            Caso não consiga clicar no botão, copie e cole o link abaixo no seu navegador:
                        </p>
                        <p style="margin:0 0 24px 0;font-size:14px;word-break:break-all;">
                            <a href="${activationUrl?html}" style="color:#4F6EF7;">${activationUrl?html}</a>
                        </p>
                        <#if validityHours??>
                        <p style="margin:0 0 8px 0;font-weight:bold;">Este link expira em ${validityHours} horas.</p>
                        </#if>
                        <hr style="border:none;border-top:1px solid #E5E7EB;margin:24px 0;">
                        <p style="margin:0 0 16px 0;font-size:14px;color:#6B7280;">
                            Se você não criou uma conta na Gravita, desconsidere este e-mail.
                        </p>
                        <p style="margin:0;font-size:14px;">
                            Abraços,<br>
                            Time Gravita
                        </p>
                    </td>
                </tr>
                <!-- Footer -->
                <tr>
                    <td style="background-color:#EEF1FE;padding:18px 32px;font-size:13px;color:#1A1F2E;">
                        Se tiver qualquer dúvida ou precisar de ajuda, é só responder a este e-mail.
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
