package cl.trato.notifications.application;

/** Plantillas en español de los correos del módulo de identidad (RF-NOT-01). */
public class IdentityEmailTemplates {

  private static final String DEMO_NOTICE =
      "Proyecto de demostración: no se realizan cobros reales.";
  private static final String SIGNATURE = "\n\n— El equipo de Trato\n" + DEMO_NOTICE;

  private final String frontendBaseUrl;

  public IdentityEmailTemplates(String frontendBaseUrl) {
    this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
  }

  public EmailMessage verification(String to, String name, String token) {
    String link = frontendBaseUrl + "/verificar-correo?token=" + token;
    String text =
        "Hola, "
            + name
            + ".\n\nPara activar tu cuenta en Trato, abre este enlace (vale por 24 horas y"
            + " solo se puede usar una vez):\n\n"
            + link
            + "\n\nSi no creaste una cuenta, ignora este mensaje."
            + SIGNATURE;
    String html =
        layout(
            name,
            "Para activar tu cuenta en Trato, abre este enlace. Vale por 24 horas y solo se puede"
                + " usar una vez.",
            link,
            "Verificar mi correo",
            "Si no creaste una cuenta, ignora este mensaje.");
    return new EmailMessage(to, "Verifica tu correo en Trato", text, html);
  }

  public EmailMessage passwordReset(String to, String name, String token) {
    String link = frontendBaseUrl + "/restablecer-contrasena?token=" + token;
    String text =
        "Hola, "
            + name
            + ".\n\nRecibimos una solicitud para restablecer tu contraseña. Usa este enlace"
            + " (vale por 1 hora y solo se puede usar una vez):\n\n"
            + link
            + "\n\nSi no lo pediste, ignora este mensaje: tu contraseña no cambiará."
            + SIGNATURE;
    String html =
        layout(
            name,
            "Recibimos una solicitud para restablecer tu contraseña. El enlace vale por 1 hora y"
                + " solo se puede usar una vez.",
            link,
            "Restablecer contraseña",
            "Si no lo pediste, ignora este mensaje: tu contraseña no cambiará.");
    return new EmailMessage(to, "Restablece tu contraseña de Trato", text, html);
  }

  public EmailMessage registrationAttempt(String to, String name) {
    String link = frontendBaseUrl + "/recuperar-contrasena";
    String text =
        "Hola, "
            + name
            + ".\n\nAlguien intentó crear una cuenta en Trato con este correo, que ya tiene una"
            + " cuenta. Si fuiste tú y no recuerdas tu contraseña, puedes recuperarla aquí:\n\n"
            + link
            + "\n\nSi no fuiste tú, no necesitas hacer nada."
            + SIGNATURE;
    String html =
        layout(
            name,
            "Alguien intentó crear una cuenta en Trato con este correo, que ya tiene una cuenta."
                + " Si fuiste tú y no recuerdas tu contraseña, puedes recuperarla.",
            link,
            "Recuperar contraseña",
            "Si no fuiste tú, no necesitas hacer nada.");
    return new EmailMessage(to, "Intento de registro con tu correo", text, html);
  }

  private static String layout(
      String name, String body, String link, String action, String footer) {
    return "<!doctype html><html lang=\"es-CL\"><body style=\"font-family:Arial,sans-serif;"
        + "color:#0b1020\"><p>Hola, "
        + escape(name)
        + ".</p><p>"
        + escape(body)
        + "</p><p><a href=\""
        + escape(link)
        + "\" style=\"background:#3b5bdb;color:#ffffff;padding:10px 16px;border-radius:6px;"
        + "text-decoration:none\">"
        + escape(action)
        + "</a></p><p style=\"font-size:12px;color:#555\">Si el botón no funciona, copia este"
        + " enlace: "
        + escape(link)
        + "</p><p style=\"font-size:12px;color:#555\">"
        + escape(footer)
        + "<br>"
        + DEMO_NOTICE
        + "</p></body></html>";
  }

  static String escape(String value) {
    StringBuilder out = new StringBuilder(value.length());
    for (char c : value.toCharArray()) {
      switch (c) {
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '&' -> out.append("&amp;");
        case '"' -> out.append("&quot;");
        case '\'' -> out.append("&#39;");
        default -> out.append(c);
      }
    }
    return out.toString();
  }
}
