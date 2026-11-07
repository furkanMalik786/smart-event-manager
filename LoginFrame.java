package eventms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Login screen with Admin / Student role switch. */
final class LoginFrame extends JFrame {
    private String role = "ADMIN";
    private final Ui.RoundBtn adminBtn = Ui.button("Admin", Ui.PRIMARY), studentBtn = Ui.button("Student", new Color(0x9CA3AF));
    private final JTextField email = Ui.field(22);
    private final JPasswordField pass = Ui.pass(22);
    private final JLabel hint = Ui.label(" ", Font.PLAIN, 12, Ui.MUTED);

    LoginFrame() {
        super("Smart Event Manager - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(920, 580);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel brand = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Ui.PRIMARY, getWidth(), getHeight(), new Color(0x7C3AED)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillOval(-80, getHeight() - 220, 300, 300);
                g2.fillOval(getWidth() - 140, -80, 260, 260);
                g2.dispose();
            }
        };
        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.add(Ui.label("Smart Event", Font.BOLD, 38, Color.WHITE));
        txt.add(Ui.label("Manager", Font.BOLD, 38, Color.WHITE));
        txt.add(Box.createVerticalStrut(14));
        for (String s : new String[]{"Events, venues and schedules", "Registrations and entry passes", "Certificates sent by email", "Reports and an AI assistant"})
            txt.add(Ui.label("•  " + s, Font.PLAIN, 14, new Color(0xE0E7FF)));
        brand.add(txt);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(0, 56, 0, 56));
        JPanel box = new JPanel(new GridBagLayout());
        box.setOpaque(false);
        box.add(Ui.label("Welcome back", Font.BOLD, 26, Ui.TEXT), gc(0, 0, 0));
        box.add(Ui.label("Sign in to continue", Font.PLAIN, 13, Ui.MUTED), gc(0, 1, 18));
        adminBtn.addActionListener(e -> setRole("ADMIN"));
        studentBtn.addActionListener(e -> setRole("STUDENT"));
        box.add(Ui.pair(adminBtn, studentBtn), gc(0, 2, 4));
        box.add(Ui.label("EMAIL", Font.BOLD, 11, Ui.MUTED), gc(0, 3, 14));
        box.add(email, gc(0, 4, 4));
        box.add(Ui.label("PASSWORD", Font.BOLD, 11, Ui.MUTED), gc(0, 5, 12));
        box.add(pass, gc(0, 6, 4));
        Ui.RoundBtn login = Ui.button("Sign in", Ui.PRIMARY);
        login.addActionListener(e -> login());
        box.add(login, gc(0, 7, 20));
        Ui.RoundBtn signup = Ui.button("New student? Create an account", Ui.GRAY);
        signup.addActionListener(e -> StudentsPage.addStudent(this));
        box.add(signup, gc(0, 8, 8));
        box.add(hint, gc(0, 9, 14));
        form.add(box);
        getRootPane().setDefaultButton(login);

        setLayout(new GridLayout(1, 2));
        add(brand);
        add(form);
        setRole("ADMIN");
    }

    private static GridBagConstraints gc(int x, int y, int top) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(top, 0, 0, 0);
        return g;
    }

    private void setRole(String r) {
        role = r;
        adminBtn.setBase("ADMIN".equals(r) ? Ui.PRIMARY : new Color(0x9CA3AF));
        studentBtn.setBase("STUDENT".equals(r) ? Ui.PRIMARY : new Color(0x9CA3AF));
        hint.setText("ADMIN".equals(r) ? "Demo: admin@college.edu / admin123" : "Demo: student@college.edu / student123");
    }

    private void login() {
        if (email.getText().isBlank() || pass.getPassword().length == 0) {
            Ui.error(this, "Please enter your email and password.");
            return;
        }
        try {
            User u = Dao.login(email.getText().trim(), new String(pass.getPassword()), role);
            if (u == null) {
                Ui.error(this, "Wrong email or password for the " + role.toLowerCase() + " account.");
                return;
            }
            Dao.current = u;
            dispose();
            new AppFrame().setVisible(true);
        } catch (RuntimeException ex) {
            Ui.error(this, "Database problem: " + ex.getMessage());
        }
    }
}
