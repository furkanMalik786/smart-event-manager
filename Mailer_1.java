package eventms;

import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;

import java.util.Properties;

/** Sends passes and certificates by SMTP (Gmail, Outlook, college server...). Settings come from config.properties. */
final class Mailer {
    private Mailer() { }

    static boolean configured() {
        return !Config.get("smtp.host").isEmpty() && !Config.get("smtp.user").isEmpty();
    }

    /** Sends one email. Any failure is rethrown with a plain-language explanation of what to fix. */
    static void send(String to, String subject, String html, String fileName, byte[] png) throws Exception {
        if (!configured())
            throw new IllegalStateException("Email is not set up yet. Admin: open Settings and fill in the SMTP details.");
        if (to == null || !to.contains("@") || to.contains(" "))
            throw new Exception("This person's email address looks invalid: '" + to + "'. Fix it in the Students page.");
        try {
            sendRaw(to, subject, html, fileName, png);
        } catch (Exception e) {
            throw new Exception(explain(e), e);
        } catch (LinkageError e) {
            throw new Exception(explain(e), e);
        }
    }

    private static void sendRaw(String to, String subject, String html, String fileName, byte[] png) throws Exception {
        String user = Config.get("smtp.user"), rawPass = Config.get("smtp.password");
        // Google shows app passwords in 4 groups of 4 letters; the spaces are not part of the password
        final String pass = Config.get("smtp.host").toLowerCase().contains("gmail") ? rawPass.replace(" ", "") : rawPass;
        int port = Config.getInt("smtp.port", 587);
        Properties p = new Properties();
        p.put("mail.smtp.host", Config.get("smtp.host"));
        p.put("mail.smtp.port", String.valueOf(port));
        p.put("mail.smtp.auth", "true");
        if (port == 465) p.put("mail.smtp.ssl.enable", "true");
        else { p.put("mail.smtp.starttls.enable", "true"); p.put("mail.smtp.starttls.required", "true"); }
        p.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        p.put("mail.smtp.connectiontimeout", "15000");
        p.put("mail.smtp.timeout", "20000");
        p.put("mail.smtp.writetimeout", "20000");
        Session s = Session.getInstance(p, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(user, pass); }
        });
        String from = Config.get("smtp.from").isEmpty() ? user : Config.get("smtp.from");
        MimeMessage m = new MimeMessage(s);
        m.setFrom(new InternetAddress(from, "Smart Event Manager"));
        m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        m.setSubject(subject, "UTF-8");
        MimeMultipart mp = new MimeMultipart();
        MimeBodyPart body = new MimeBodyPart();
        body.setContent(html, "text/html; charset=UTF-8");
        mp.addBodyPart(body);
        if (png != null) {
            MimeBodyPart att = new MimeBodyPart();
            att.setDataHandler(new DataHandler(new ByteArrayDataSource(png, "image/png")));
            att.setFileName(fileName);
            mp.addBodyPart(att);
        }
        m.setContent(mp);
        Transport.send(m);
    }

    /** Turns a mail exception into something a non-programmer can act on, plus the technical detail. */
    static String explain(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String msg = String.valueOf(e.getMessage());
        String low = (msg + " " + root).toLowerCase(java.util.Locale.ROOT);
        String host = Config.get("smtp.host"), port = Config.get("smtp.port", "587");
        String hint;
        if (root instanceof LinkageError || root instanceof ClassNotFoundException)
            hint = "A mail library is missing. Make sure the lib folder contains jakarta.mail and jakarta.activation, and start the app with: java -cp \"out;lib/*\" eventms.Main";
        else if (e instanceof jakarta.mail.AuthenticationFailedException || low.contains("535") || low.contains("username and password not accepted") || low.contains("authentication failed"))
            hint = "The mail server refused the login. For Gmail you must turn on 2-Step Verification and use a 16-character App Password (not your normal password). Check the username and password in Settings.";
        else if (root instanceof java.net.UnknownHostException)
            hint = "Cannot find the mail server '" + host + "'. Check the SMTP host spelling and your internet connection.";
        else if (root instanceof java.net.ConnectException || root instanceof java.net.SocketTimeoutException || low.contains("couldn't connect"))
            hint = "Cannot reach " + host + " on port " + port + ". Check your internet, firewall or college network, or try port 465 instead of 587.";
        else if (low.contains("pkix") || low.contains("sslhandshake") || root instanceof javax.net.ssl.SSLException)
            hint = "The secure (TLS) connection failed. An antivirus with 'web shield' or a network proxy can cause this: switch it off for a moment, or try the other port (465 / 587).";
        else if (e instanceof jakarta.mail.SendFailedException || low.contains("invalid addresses") || low.contains("recipient"))
            hint = "The mail server rejected the recipient address. Check the participant's email.";
        else
            hint = "Check the SMTP details in Settings (host, port, username, password).";
        return hint + "\n\nTechnical detail: " + root.getClass().getSimpleName() + (root.getMessage() == null ? "" : " - " + root.getMessage());
    }

    static String html(String heading, String name, String text) {
        return "<div style='font-family:Segoe UI,Arial,sans-serif;max-width:520px;margin:auto;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden'>"
                + "<div style='background:#4F46E5;color:#fff;padding:18px 24px;font-size:20px;font-weight:bold'>" + heading + "</div>"
                + "<div style='padding:24px;color:#1f2937;font-size:15px;line-height:1.6'>Hi " + esc(name) + ",<br><br>" + text
                + "<br><br>Regards,<br>Smart Event Manager</div></div>";
    }

    static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
