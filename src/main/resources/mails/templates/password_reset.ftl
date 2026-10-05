<#-- Variables: username, resetUrl, validityMinutes (optional) -->
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Redefina sua senha Gravita</title>
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
                        Redefina sua senha
                    </td>
                </tr>
                <!-- Content -->
                <tr>
                    <td style="padding:32px;font-size:16px;line-height:24px;">
                        <p style="margin:0 0 16px 0;font-size:20px;font-weight:bold;">Olá, ${username?html}!</p>
                        <p style="margin:0 0 24px 0;">
                            Recebemos um pedido para redefinir a senha da sua conta na Gravita.
                            Clique no botão abaixo para escolher uma nova senha.
                        </p>
                        <p style="margin:0 0 24px 0;text-align:center;">
                            <a href="${resetUrl?html}"
                               style="display:inline-block;background-color:#4F6EF7;color:#FFFFFF;text-decoration:none;font-weight:bold;padding:14px 28px;border-radius:6px;">
                                Redefinir minha senha
                            </a>
                        </p>
                        <p style="margin:0 0 8px 0;font-size:14px;color:#6B7280;">
                            Caso não consiga clicar no botão, copie e cole o link abaixo no seu navegador:
                        </p>
                        <p style="margin:0 0 24px 0;font-size:14px;word-break:break-all;">
                            <a href="${resetUrl?html}" style="color:#4F6EF7;">${resetUrl?html}</a>
                        </p>
                        <#if validityMinutes??>
                        <p style="margin:0 0 8px 0;font-weight:bold;">Este link expira em ${validityMinutes} minutos e só pode ser usado uma vez.</p>
                        </#if>
                        <hr style="border:none;border-top:1px solid #E5E7EB;margin:24px 0;">
                        <p style="margin:0 0 16px 0;font-size:14px;color:#6B7280;">
                            Se você não pediu a redefinição, desconsidere este e-mail: a sua senha continua a mesma.
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
