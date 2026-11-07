package eventms;

import javax.swing.*;
import java.awt.*;

/** Admin: email (SMTP), AI API and certificate settings, stored in config.properties. */
final class SettingsPage extends Page {
    private final JTextField host = Ui.field(24), port = Ui.field(24), user = Ui.field(24), from = Ui.field(24),
            model = Ui.field(24), testTo = Ui.field(24);
    private final JPasswordField pass = Ui.pass(24), key = Ui.pass(24);

    SettingsPage() {
        Ui.RoundBtn save = Ui.button("Save settings", Ui.SUCCESS);
        save.addActionListener(e -> {
            if (saveQuietly()) Ui.info(this, "Settings saved.");
        });
        add(header("Settings", "Email, AI and certificate connections", save), BorderLayout.NORTH);

        Ui.Card mail = new Ui.Card(new BorderLayout(0, 10)).glow(Ui.CYAN);
        mail.add(Ui.label("Email (SMTP)", Font.BOLD, 16, Ui.INK), BorderLayout.NORTH);
        JPanel f1 = new JPanel(new GridBagLayout());
        f1.setOpaque(false);
        Ui.row(f1, 0, "SMTP host (e.g. smtp.gmail.com)", host, Ui.INK2);
        Ui.row(f1, 1, "Port (587 = STARTTLS, 465 = SSL)", port, Ui.INK2);
        Ui.row(f1, 2, "Username (your email address)", user, Ui.INK2);
        Ui.row(f1, 3, "Password / app password", pass, Ui.INK2);
        Ui.row(f1, 4, "Send from (optional)", from, Ui.INK2);
        Ui.row(f1, 5, "Send a test email to", testTo, Ui.INK2);
        mail.add(f1, BorderLayout.CENTER);
        Ui.RoundBtn test = Ui.button("Send test email", Ui.PRIMARY);
        test.addActionListener(e -> {
            String to = testTo.getText().trim();
            if (!to.contains("@") || to.contains(" ")) { Ui.error(this, "Type a real email address you can open in 'Send a test email to'."); return; }
            if (!saveQuietly()) return;
            Ui.run(this, () -> {
                Mailer.send(to, "Smart Event Manager test", Mailer.html("It works!", Dao.current.name(),
                        "Your email settings are correct. Passes and certificates can now be sent."), null, null);
                return "Test email sent to " + to + ".\nCheck the inbox (and the Spam folder).";
            }, null);
        });
        mail.add(Ui.bar(0, test), BorderLayout.SOUTH);

        Ui.Card ai = new Ui.Card(new BorderLayout(0, 10)).glow(Ui.PINK);
        ai.add(Ui.label("AI module (Anthropic API)", Font.BOLD, 16, Ui.INK), BorderLayout.NORTH);
        JPanel f2 = new JPanel(new GridBagLayout());
        f2.setOpaque(false);
        Ui.row(f2, 0, "API key", key, Ui.INK2);
        Ui.row(f2, 1, "Model", model, Ui.INK2);
        JPanel f2top = new JPanel(new BorderLayout());
        f2top.setOpaque(false);
        f2top.add(f2, BorderLayout.NORTH);
        ai.add(f2top, BorderLayout.CENTER);
        ai.add(Ui.label("<html><body style='width:330px'>Get a key at console.anthropic.com. It is stored only in config.properties on this computer.</body></html>", Font.PLAIN, 12, Ui.INK2), BorderLayout.SOUTH);

        Ui.Card cert = new Ui.Card(new BorderLayout(0, 8)).glow(Ui.VIOLET);
        cert.add(Ui.label("Certificates", Font.BOLD, 16, Ui.INK), BorderLayout.NORTH);
        cert.add(Ui.label("<html><body style='width:330px'>Set the heading, college name, logo and the signers' names, titles and signatures that are printed on every certificate.</body></html>",
                Font.PLAIN, 12, Ui.INK2), BorderLayout.CENTER);
        Ui.RoundBtn design = Ui.button("Open certificate design", Ui.PINK);
        design.addActionListener(e -> CertDesigner.open(this));
        cert.add(Ui.bar(0, design), BorderLayout.SOUTH);

        Ui.Card db = new Ui.Card(new BorderLayout(0, 6)).glow(Ui.SUCCESS);
        db.add(Ui.label("Database", Font.BOLD, 16, Ui.INK), BorderLayout.NORTH);
        db.add(Ui.label("<html><body style='width:330px'>Connected to: " + Config.get("db.url", "jdbc:sqlite:eventms.db")
                + "<br>Change db.url / db.user / db.password in config.properties to use MySQL or another JDBC database.</body></html>",
                Font.PLAIN, 12, Ui.INK2), BorderLayout.CENTER);

        JPanel top = new JPanel(new GridLayout(1, 2, 6, 0));
        top.setOpaque(false);
        top.add(mail);
        top.add(ai);
        JPanel bottom = new JPanel(new GridLayout(1, 2, 6, 0));
        bottom.setOpaque(false);
        bottom.add(cert);
        bottom.add(db);
        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setOpaque(false);
        body.add(top, BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);
        wrap.add(body, BorderLayout.CENTER);
        JScrollPane sp = Ui.thin(new JScrollPane(wrap));
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);
    }

    @Override void refresh() {
        host.setText(Config.get("smtp.host"));
        port.setText(Config.get("smtp.port", "587"));
        user.setText(Config.get("smtp.user"));
        pass.setText(Config.get("smtp.password"));
        from.setText(Config.get("smtp.from"));
        key.setText(Config.get("ai.key"));
        model.setText(Config.get("ai.model", "claude-sonnet-5-5"));
        if (testTo.getText().isBlank()) testTo.setText(Config.get("smtp.user"));
    }

    private boolean saveQuietly() {
        Config.set("smtp.host", host.getText());
        Config.set("smtp.port", port.getText());
        Config.set("smtp.user", user.getText());
        Config.set("smtp.password", new String(pass.getPassword()));
        Config.set("smtp.from", from.getText());
        Config.set("ai.key", new String(key.getPassword()));
        Config.set("ai.model", model.getText());
        try {
            Config.save();
            return true;
        } catch (Exception e) {
            Ui.error(this, "Could not save config.properties: " + e.getMessage());
            return false;
        }
    }
}
