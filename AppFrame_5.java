package eventms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main window: glowing sidebar + a page area. Admin and student get different menus. */
final class AppFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, Nav> navs = new LinkedHashMap<>();
    private Page current;
    private final Timer fx;

    // ---------------------------------------------------------------- small vector icons
    private static void icon(Graphics2D g, String k, int x, int y, int s, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (k) {
            case "dash" -> {
                int a = s / 2 - 2;
                g.fillRoundRect(x, y, a, a, 4, 4);
                g.fillRoundRect(x + s - a, y, a, a, 4, 4);
                g.fillRoundRect(x, y + s - a, a, a, 4, 4);
                g.fillRoundRect(x + s - a, y + s - a, a, a, 4, 4);
            }
            case "events" -> {
                g.drawRoundRect(x, y + 3, s, s - 3, 5, 5);
                g.drawLine(x, y + 8, x + s, y + 8);
                g.drawLine(x + 5, y, x + 5, y + 5);
                g.drawLine(x + s - 5, y, x + s - 5, y + 5);
                g.fillOval(x + 4, y + 11, 3, 3);
                g.fillOval(x + 10, y + 11, 3, 3);
            }
            case "people" -> {
                g.drawOval(x + s / 2 - 4, y, 8, 8);
                g.draw(new Arc2D.Double(x + 1, y + 9, s - 2, s, 0, 180, Arc2D.OPEN));
            }
            case "docs", "mine" -> {
                g.drawRoundRect(x, y + 2, s, s - 4, 5, 5);
                g.fillOval(x - 2, y + s / 2 - 3, 6, 6);
                g.fillOval(x + s - 4, y + s / 2 - 3, 6, 6);
                g.drawLine(x + 6, y + 7, x + s - 6, y + 7);
                g.drawLine(x + 6, y + s - 7, x + s - 9, y + s - 7);
            }
            case "students" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(x + s / 2.0, y + 1);
                p.lineTo(x + s, y + 6);
                p.lineTo(x + s / 2.0, y + 11);
                p.lineTo(x, y + 6);
                p.closePath();
                g.draw(p);
                g.drawLine(x + 4, y + 10, x + 4, y + 15);
                g.drawLine(x + s - 4, y + 10, x + s - 4, y + 15);
                g.drawLine(x + 4, y + 15, x + s - 4, y + 15);
            }
            case "reports" -> {
                g.fillRoundRect(x + 1, y + 9, 4, s - 9, 2, 2);
                g.fillRoundRect(x + 7, y + 3, 4, s - 3, 2, 2);
                g.fillRoundRect(x + 13, y + 6, 4, s - 6, 2, 2);
            }
            case "ai" -> {
                Path2D p = new Path2D.Double();
                double cx = x + s / 2.0, cy = y + s / 2.0, o = s / 2.0, in = s / 7.0;
                p.moveTo(cx, cy - o);
                p.quadTo(cx + in, cy - in, cx + o, cy);
                p.quadTo(cx + in, cy + in, cx, cy + o);
                p.quadTo(cx - in, cy + in, cx - o, cy);
                p.quadTo(cx - in, cy - in, cx, cy - o);
                g.fill(p);
            }
            default -> {   // settings
                g.drawOval(x + 4, y + 4, s - 8, s - 8);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i, cx = x + s / 2.0, cy = y + s / 2.0;
                    g.draw(new Line2D.Double(cx + Math.cos(a) * (s / 2.0 - 3), cy + Math.sin(a) * (s / 2.0 - 3),
                            cx + Math.cos(a) * (s / 2.0 + 1), cy + Math.sin(a) * (s / 2.0 + 1)));
                }
            }
        }
    }

    // ---------------------------------------------------------------- sidebar button
    private static final class Nav extends JButton {
        private final String ic;
        private boolean active, hover;
        private float act, hv;
        private Timer t;

        Nav(String text, String icon) {
            super(text);
            this.ic = icon;
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(Ui.font(Font.BOLD, 13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(new EmptyBorder(11, 48, 11, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; kick(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; kick(); }
            });
        }

        void setActive(boolean a) { active = a; kick(); }

        private void kick() {
            if (!Ui.FX) { act = active ? 1 : 0; hv = hover ? 1 : 0; repaint(); return; }
            if (t == null) t = new Timer(16, e -> {
                float ta = active ? 1f : 0f, th = hover ? 1f : 0f;
                act += (ta - act) * 0.22f;
                hv += (th - hv) * 0.3f;
                if (Math.abs(ta - act) < 0.02f && Math.abs(th - hv) < 0.02f) { act = ta; hv = th; t.stop(); }
                repaint();
            });
            t.start();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight() - 3;
            if (act > 0.01f) {
                g2.setColor(Ui.alpha(Ui.VIOLET, (int) (40 * act)));
                g2.fillRoundRect(-2, 1, w + 4, h + 4, 18, 18);
                g2.setPaint(new GradientPaint(0, 0, Ui.alpha(new Color(0x6366F1), (int) (255 * act)), w, h, Ui.alpha(new Color(0x7C3AED), (int) (255 * act))));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, (int) (80 * act)), 0, h / 2f, new Color(255, 255, 255, 0)));
                g2.fillRoundRect(0, 0, w, h / 2, 14, 14);
                g2.setColor(Ui.alpha(Ui.CYAN, (int) (255 * act)));
                g2.fillRoundRect(0, h / 2 - 9, 4, 18, 4, 4);
            }
            if (hv > 0.01f && act < 0.99f) {
                g2.setColor(new Color(255, 255, 255, (int) (24 * hv * (1 - act))));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }
            Color c = Ui.mix(new Color(0xA5B0D0), Color.WHITE, Math.max(act, hv * 0.8f));
            icon(g2, ic, 18, (h - 18) / 2, 18, c);
            g2.setFont(getFont());
            g2.setColor(c);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 48, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Round gradient avatar showing the user's initial. */
    private static final class Avatar extends JComponent {
        private final String initial;

        Avatar(String name) {
            initial = name.isBlank() ? "?" : name.substring(0, 1).toUpperCase();
            setPreferredSize(new Dimension(40, 40));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Ui.alpha(Ui.CYAN, 50));
            g2.fillOval(0, 0, 40, 40);
            g2.setPaint(new GradientPaint(0, 0, Ui.CYAN, 36, 36, Ui.VIOLET));
            g2.fillOval(3, 3, 34, 34);
            g2.setColor(Color.WHITE);
            g2.setFont(Ui.font(Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial, 20 - fm.stringWidth(initial) / 2, 20 + (fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    AppFrame() {
        super("Smart Event Manager");
        boolean admin = "ADMIN".equals(Dao.current.role());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);

        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0B1022), 0, getHeight(), new Color(0x1A1148)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                float sway = (float) (Math.sin(Ui.clock * 0.5) * 30);
                g2.setPaint(new RadialGradientPaint(getWidth() / 2f + sway, 0, 280f, new float[]{0f, 1f},
                        new Color[]{new Color(99, 102, 241, 100), new Color(99, 102, 241, 0)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new RadialGradientPaint(getWidth() / 2f - sway, getHeight(), 260f, new float[]{0f, 1f},
                        new Color[]{new Color(236, 72, 153, 55), new Color(236, 72, 153, 0)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 0), 0, getHeight() / 2f, new Color(255, 255, 255, 40)));
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.setBorder(new EmptyBorder(22, 14, 18, 14));
        side.setPreferredSize(new Dimension(250, 0));
        JLabel logo = new Ui.GradLabel("Smart Events", 22);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel role = Ui.label(admin ? "ADMIN PANEL" : "STUDENT PORTAL", Font.BOLD, 11, new Color(0x818CF8));
        role.setBorder(new EmptyBorder(0, 4, 22, 0));
        role.setAlignmentX(LEFT_ALIGNMENT);
        side.add(logo);
        side.add(role);

        add("dash", "Dashboard", "dash", new DashboardPage(admin), side);
        if (admin) {
            add("events", "Events", "events", new EventsPage(true), side);
            add("people", "Participants", "people", new RegistrationsPage(RegistrationsPage.Mode.PARTICIPANTS), side);
            add("docs", "Passes & Certificates", "docs", new RegistrationsPage(RegistrationsPage.Mode.DOCUMENTS), side);
            add("students", "Students", "students", new StudentsPage(), side);
            add("reports", "Analysis Report", "reports", new ReportsPage(), side);
            add("ai", "AI Assistant", "ai", new AiPage(true), side);
            add("settings", "Settings", "settings", new SettingsPage(), side);
        } else {
            add("events", "Browse Events", "events", new EventsPage(false), side);
            add("mine", "My Registrations", "mine", new RegistrationsPage(RegistrationsPage.Mode.STUDENT), side);
            add("ai", "AI Assistant", "ai", new AiPage(false), side);
        }

        side.add(Box.createVerticalGlue());
        JPanel me = new JPanel(new BorderLayout(10, 0));
        me.setOpaque(false);
        me.setAlignmentX(LEFT_ALIGNMENT);
        me.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(Ui.label(Dao.current.name(), Font.BOLD, 13, Color.WHITE));
        names.add(Ui.label(Dao.current.email(), Font.PLAIN, 11, new Color(0x9CA3AF)));
        me.add(new Avatar(Dao.current.name()), BorderLayout.WEST);
        me.add(names, BorderLayout.CENTER);
        Ui.RoundBtn out = Ui.button("Log out", new Color(0x374151));
        out.setAlignmentX(LEFT_ALIGNMENT);
        out.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        out.addActionListener(e -> {
            Dao.current = null;
            dispose();
            new LoginFrame().setVisible(true);
        });
        side.add(me);
        side.add(Box.createVerticalStrut(12));
        side.add(out);

        setLayout(new BorderLayout());
        add(side, BorderLayout.WEST);
        content.setBackground(Ui.DEEP0);
        add(content, BorderLayout.CENTER);
        fx = new Timer(45, e -> {
            Ui.clock += 0.045;
            if (current != null) current.repaint();
            side.repaint();
        });
        if (Ui.FX) fx.start();
        show("dash");
    }

    @Override public void dispose() {
        fx.stop();
        super.dispose();
    }

    private void add(String key, String title, String icon, Page page, JPanel side) {
        pages.put(key, page);
        content.add(page, key);
        Nav n = new Nav(title, icon);
        n.setAlignmentX(LEFT_ALIGNMENT);
        n.addActionListener(e -> show(key));
        navs.put(key, n);
        side.add(n);
        side.add(Box.createVerticalStrut(4));
    }

    private void show(String key) {
        navs.forEach((k, n) -> n.setActive(k.equals(key)));
        Page p = pages.get(key);
        p.refresh();
        current = p;
        cards.show(content, key);
        p.playIn();
    }
}
