package eventms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One table, three roles:
 * PARTICIPANTS - admin manages who registered (name, phone, email) and marks attendance;
 * DOCUMENTS    - admin issues certificates, designs them, and emails passes / certificates;
 * STUDENT      - a student sees their own passes and certificates.
 */
final class RegistrationsPage extends Page {
    enum Mode { PARTICIPANTS, DOCUMENTS, STUDENT }

    private final Mode mode;
    private final DefaultTableModel m = Ui.model("ID", "Student", "Phone", "Email", "Event", "Date", "Venue", "Status", "Pass",
            "Certificate", "Pass email", "Cert email");
    private final JTable t = Ui.table(m);
    private final Map<Integer, Object[]> rows = new HashMap<>();

    RegistrationsPage(Mode mode) {
        this.mode = mode;
        JTextField search = new Ui.SearchField(18, "Search...");
        JComponent[] btns;
        switch (mode) {
            case PARTICIPANTS -> {
                add(header("Participants", "Everyone registered for your events, with phone, email and attendance"), BorderLayout.NORTH);
                btns = new JComponent[]{
                        btn("+ Add participant", Ui.SUCCESS, this::addParticipant),
                        btn("Mark attended", Ui.PRIMARY, () -> setStatus("ATTENDED")),
                        btn("Cancel", Ui.WARN, () -> setStatus("CANCELLED")),
                        btn("Remove", Ui.DANGER, this::remove)};
            }
            case DOCUMENTS -> {
                add(header("Passes & Certificates", "Issue certificates, design them, and email documents to students"), BorderLayout.NORTH);
                btns = new JComponent[]{
                        btn("View pass", Ui.GRAY, () -> view(false)),
                        btn("View certificate", Ui.GRAY, () -> view(true)),
                        btn("Issue to all attended", Ui.SUCCESS, this::issueAll),
                        btn("Email pass", Ui.PRIMARY, () -> mail(false)),
                        btn("Email certificate", Ui.PRIMARY, () -> mail(true)),
                        btn("Certificate design", Ui.PINK, () -> CertDesigner.open(this))};
            }
            default -> {
                add(header("My Registrations", "Your passes and certificates"), BorderLayout.NORTH);
                btns = new JComponent[]{
                        btn("View pass", Ui.GRAY, () -> view(false)),
                        btn("View certificate", Ui.GRAY, () -> view(true)),
                        btn("Email pass to me", Ui.PRIMARY, () -> mail(false)),
                        btn("Email certificate to me", Ui.PRIMARY, () -> mail(true)),
                        btn("Cancel registration", Ui.DANGER, () -> setStatus("CANCELLED"))};
            }
        }
        add(tableCard(t, search, btns), BorderLayout.CENTER);
        TableColumnModel cm = t.getColumnModel();
        String[] hidden = switch (mode) {
            case PARTICIPANTS -> new String[]{"ID", "Pass", "Certificate", "Pass email", "Cert email"};
            case DOCUMENTS -> new String[]{"ID", "Phone", "Venue", "Pass", "Date"};
            case STUDENT -> new String[]{"ID", "Student", "Phone", "Email"};
        };
        for (String name : hidden) cm.removeColumn(cm.getColumn(cm.getColumnIndex(name)));
    }

    private Ui.RoundBtn btn(String text, Color c, Runnable r) {
        Ui.RoundBtn b = Ui.button(text, c);
        b.addActionListener(e -> r.run());
        return b;
    }

    @Override void refresh() {
        rows.clear();
        List<Object[]> out = new ArrayList<>();
        List<Object[]> data = mode == Mode.STUDENT ? Dao.regRows("r.user_id=?", Dao.current.id()) : Dao.regRows("1=1");
        for (Object[] r : data) {
            int id = Ui.num(r[Dao.R_ID]);
            rows.put(id, r);
            boolean cancelled = "CANCELLED".equals(Dao.s(r[Dao.R_STATUS]));
            String phone = Dao.s(r[Dao.R_PHONE]);
            out.add(new Object[]{id, Dao.s(r[Dao.R_NAME]), phone.isEmpty() ? "-" : phone, Dao.s(r[Dao.R_EMAIL]),
                    Dao.s(r[Dao.R_TITLE]), Dao.s(r[Dao.R_DATE]), Dao.s(r[Dao.R_VENUE]),
                    Dao.s(r[Dao.R_STATUS]), cancelled ? "-" : Dao.s(r[Dao.R_PASS]),
                    r[Dao.R_CERT] == null ? "Not issued" : Dao.s(r[Dao.R_CERT]),
                    cancelled ? "-" : Ui.num(r[Dao.R_PASSMAIL]) == 1 ? "Sent" : "Pending",
                    r[Dao.R_CERT] == null ? "-" : Ui.num(r[Dao.R_CERTMAIL]) == 1 ? "Sent" : "Pending"});
        }
        Ui.fill(m, out);
    }

    // ---------- participants ----------
    private void addParticipant() {
        JComboBox<Ui.Item> students = Ui.combo(), evs = Ui.combo();
        students.addItem(new Ui.Item(-1, "+ New person (fill in the fields below)"));
        for (Object[] s : Dao.students()) students.addItem(new Ui.Item(Ui.num(s[0]), Dao.s(s[1]) + " (" + Dao.s(s[2]) + ")"));
        for (Object[] e : Dao.events()) evs.addItem(new Ui.Item(Ui.num(e[0]), Dao.s(e[1]) + " - " + Dao.s(e[3])));
        if (evs.getItemCount() == 0) {
            Ui.info(this, "Please create an event first.");
            return;
        }
        JTextField name = Ui.field(22), email = Ui.field(22), phone = Ui.field(22), dept = Ui.field(22);
        JPanel p = new JPanel(new GridBagLayout());
        Ui.row(p, 0, "Event", evs);
        Ui.row(p, 1, "Participant", students);
        Ui.row(p, 2, "New person: full name", name);
        Ui.row(p, 3, "New person: email (the pass is sent here)", email);
        Ui.row(p, 4, "Phone number", phone);
        Ui.row(p, 5, "New person: department (optional)", dept);
        Runnable toggle = () -> {
            boolean isNew = ((Ui.Item) students.getSelectedItem()).id() == -1;
            name.setEnabled(isNew);
            email.setEnabled(isNew);
            dept.setEnabled(isNew);
        };
        students.addActionListener(e -> toggle.run());
        toggle.run();
        if (JOptionPane.showConfirmDialog(this, p, "Add participant", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        if (!Dao.validPhone(phone.getText())) {
            Ui.error(this, "The phone number looks wrong. Use 7-18 digits; + ( ) - and spaces are allowed.");
            return;
        }
        String ph = phone.getText().trim();
        int userId = ((Ui.Item) students.getSelectedItem()).id();
        String note = "";
        if (userId == -1) {
            String n = name.getText().trim(), em = email.getText().trim();
            if (n.isEmpty() || !em.contains("@") || em.contains(" ")) {
                Ui.error(this, "Please enter the person's name and a valid email address.");
                return;
            }
            List<Object[]> ex = Db.query("SELECT id,role FROM users WHERE lower(email)=lower(?)", em);
            if (!ex.isEmpty()) {
                if (!"STUDENT".equals(Dao.s(ex.get(0)[1]))) { Ui.error(this, "That email belongs to an admin account."); return; }
                userId = Ui.num(ex.get(0)[0]);
            } else {
                Dao.addUser(n, em, "student123", "STUDENT", dept.getText().isBlank() ? "Other" : dept.getText().trim(), ph);
                userId = Ui.num(Db.query("SELECT id FROM users WHERE lower(email)=lower(?)", em).get(0)[0]);
                note = "\nA student account was created: " + em + " / student123";
                ph = "";   // already saved
            }
        }
        if (!ph.isEmpty()) Dao.setPhone(userId, ph);
        String err = Dao.register(((Ui.Item) evs.getSelectedItem()).id(), userId);
        if (err != null) Ui.error(this, err);
        else Ui.info(this, "Participant added." + note);
        refresh();
    }

    private void setStatus(String status) {
        List<Integer> ids = Ui.selIds(t, 0);
        if (ids.isEmpty()) { Ui.info(this, "Please select a registration first."); return; }
        if (mode == Mode.STUDENT && !Ui.confirm(this, "Cancel the selected registration(s)?")) return;
        int changed = 0;
        for (int id : ids) {
            String cur = Dao.s(rows.get(id)[Dao.R_STATUS]);
            if ("CANCELLED".equals(cur) && status.equals(cur)) continue;
            if (mode == Mode.STUDENT && !"REGISTERED".equals(cur)) continue; // students can only cancel upcoming ones
            if ("ATTENDED".equals(status) && "CANCELLED".equals(cur)) continue;
            Dao.setStatus(id, status);
            changed++;
        }
        if (changed == 0) Ui.info(this, "Nothing to change for the selected row(s).");
        refresh();
    }

    private void remove() {
        List<Integer> ids = Ui.selIds(t, 0);
        if (ids.isEmpty()) { Ui.info(this, "Please select a registration first."); return; }
        if (!Ui.confirm(this, "Permanently remove " + ids.size() + " registration(s)?")) return;
        for (int id : ids) Dao.deleteReg(id);
        refresh();
    }

    // ---------- certificates ----------
    private void issueAll() {
        int n = 0;
        for (Map.Entry<Integer, Object[]> e : rows.entrySet())
            if ("ATTENDED".equals(Dao.s(e.getValue()[Dao.R_STATUS])) && e.getValue()[Dao.R_CERT] == null) {
                Dao.ensureCert(e.getKey());
                n++;
            }
        Ui.info(this, n == 0 ? "Everyone who attended already has a certificate." : n + " certificate(s) issued.");
        refresh();
    }

    // ---------- passes / certificates: view + email ----------
    private BufferedImage pass(Object[] r, String code) {
        return Art.pass(Dao.s(r[Dao.R_NAME]), Dao.s(r[Dao.R_TITLE]), Dao.s(r[Dao.R_DATE]), Dao.s(r[Dao.R_TIME]), Dao.s(r[Dao.R_VENUE]), code);
    }

    private BufferedImage cert(Object[] r, String code) {
        return Art.certificate(Dao.s(r[Dao.R_NAME]), Dao.s(r[Dao.R_TITLE]), Dao.s(r[Dao.R_DATE]), code, Dao.s(r[Dao.R_HOST]));
    }

    /** Certificate code for a row; admins may create it on the spot for attended students. */
    private String certCode(int id, Object[] r) {
        if (r[Dao.R_CERT] != null) return Dao.s(r[Dao.R_CERT]);
        if (mode != Mode.STUDENT && "ATTENDED".equals(Dao.s(r[Dao.R_STATUS]))) return Dao.ensureCert(id);
        return null;
    }

    private void view(boolean certificate) {
        int id = needRow(t, "registration");
        if (id < 0) return;
        Object[] r = rows.get(id);
        String status = Dao.s(r[Dao.R_STATUS]);
        if (!certificate) {
            if ("CANCELLED".equals(status)) { Ui.info(this, "This registration was cancelled, so there is no pass."); return; }
            Art.preview(this, pass(r, Dao.s(r[Dao.R_PASS])), "Entry pass", "pass-" + id + ".png");
        } else {
            String code = certCode(id, r);
            if (code == null) { Ui.info(this, "No certificate yet. It is issued after attendance is marked."); return; }
            Art.preview(this, cert(r, code), "Certificate", "certificate-" + id + ".png");
            refresh();
        }
    }

    private void mail(boolean certificate) {
        List<Integer> ids = Ui.selIds(t, 0);
        if (ids.isEmpty()) { Ui.info(this, "Please select one or more registrations first."); return; }
        Ui.run(this, () -> {
            int ok = 0, skipped = 0;
            String firstError = null;
            for (int id : ids) {
                Object[] r = rows.get(id);
                String status = Dao.s(r[Dao.R_STATUS]);
                String to = Dao.s(r[Dao.R_EMAIL]), name = Dao.s(r[Dao.R_NAME]), event = Dao.s(r[Dao.R_TITLE]);
                try {
                    if (certificate) {
                        String code = certCode(id, r);
                        if (code == null) { skipped++; continue; }
                        Mailer.send(to, "Your certificate - " + event,
                                Mailer.html("Your Certificate", name, "Thank you for attending <b>" + Mailer.esc(event)
                                        + "</b>. Your certificate is attached to this email.<br>Certificate ID: <b>" + code + "</b>"),
                                "certificate-" + id + ".png", Art.png(cert(r, code)));
                        Dao.markCertEmailed(id);
                    } else {
                        if ("CANCELLED".equals(status)) { skipped++; continue; }
                        Mailer.send(to, "Your entry pass - " + event,
                                Mailer.html("Your Entry Pass", name, "You are registered for <b>" + Mailer.esc(event) + "</b> on <b>"
                                        + Dao.s(r[Dao.R_DATE]) + "</b> at <b>" + Dao.s(r[Dao.R_TIME]) + "</b>, " + Mailer.esc(Dao.s(r[Dao.R_VENUE]))
                                        + ".<br>Your pass is attached. Please show it at the entrance.<br>Pass code: <b>" + Dao.s(r[Dao.R_PASS]) + "</b>"),
                                "pass-" + id + ".png", Art.png(pass(r, Dao.s(r[Dao.R_PASS]))));
                        Dao.markPassEmailed(id);
                    }
                    ok++;
                } catch (Exception ex) {
                    if (firstError == null) firstError = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    if (ex instanceof IllegalStateException) break; // email not configured: no point trying the rest
                }
            }
            String what = certificate ? "certificate(s)" : "pass(es)";
            if (firstError != null && ok == 0) throw new Exception(firstError);
            return ok + " " + what + " emailed." + (skipped > 0 ? "\n" + skipped + " skipped (" + (certificate ? "no certificate yet" : "cancelled") + ")." : "")
                    + (firstError != null ? "\nSome failed:\n" + firstError : "");
        }, this::refresh);
    }
}
