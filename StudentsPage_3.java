package eventms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Admin: manage student accounts. */
final class StudentsPage extends Page {
    private final DefaultTableModel m = Ui.model("ID", "Name", "Email", "Phone", "Department", "Joined");
    private final JTable t = Ui.table(m);

    StudentsPage() {
        Ui.RoundBtn add = Ui.button("+ Add student", Ui.SUCCESS), del = Ui.button("Delete", Ui.DANGER);
        add.addActionListener(e -> {
            if (addStudent(this)) refresh();
        });
        del.addActionListener(e -> {
            int id = needRow(t, "student");
            if (id >= 0 && Ui.confirm(this, "Delete this student and all of their registrations?")) {
                Dao.deleteUser(id);
                refresh();
            }
        });
        add(header("Students", "Accounts that can log in and register for events"), BorderLayout.NORTH);
        add(tableCard(t, new Ui.SearchField(18, "Search..."), add, del), BorderLayout.CENTER);
        t.removeColumn(t.getColumnModel().getColumn(0));
    }

    @Override void refresh() {
        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : Dao.students())
            rows.add(new Object[]{Ui.num(r[0]), Dao.s(r[1]), Dao.s(r[2]), Dao.s(r[5]), Dao.s(r[3]), Dao.s(r[4])});
        Ui.fill(m, rows);
    }

    /** Shared by this page and the login screen's "Create account" button. @return true if a student was created */
    static boolean addStudent(Component parent) {
        JTextField name = Ui.field(22), email = Ui.field(22), phone = Ui.field(22), dept = Ui.field(22);
        JPasswordField pw = Ui.pass(22);
        JPanel p = new JPanel(new GridBagLayout());
        Ui.row(p, 0, "Full name", name);
        Ui.row(p, 1, "Email", email);
        Ui.row(p, 2, "Phone number (optional)", phone);
        Ui.row(p, 3, "Department", dept);
        Ui.row(p, 4, "Password (min 6 characters)", pw);
        if (JOptionPane.showConfirmDialog(parent, p, "New student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
            return false;
        String pass = new String(pw.getPassword());
        if (name.getText().isBlank() || !email.getText().contains("@") || email.getText().contains(" ")) {
            Ui.error(parent, "Please enter a name and a valid email address.");
            return false;
        }
        if (!Dao.validPhone(phone.getText())) {
            Ui.error(parent, "The phone number looks wrong. Use 7-18 digits; + ( ) - and spaces are allowed.");
            return false;
        }
        if (pass.length() < 6) {
            Ui.error(parent, "The password must be at least 6 characters.");
            return false;
        }
        String err = Dao.addUser(name.getText().trim(), email.getText().trim(), pass, "STUDENT",
                dept.getText().isBlank() ? "Other" : dept.getText().trim(), phone.getText());
        if (err != null) {
            Ui.error(parent, err);
            return false;
        }
        Ui.info(parent, "Account created for " + name.getText().trim() + ".");
        return true;
    }
}
