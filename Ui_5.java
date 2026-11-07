package eventms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/** Shared look & feel (dark glass theme with glow) plus small Swing helpers. */
final class Ui {
    private Ui() { }

    // light-surface colours (dialogs, certificates, passes)
    static final Color PRIMARY = new Color(0x4F46E5), BG = new Color(0xF3F4F8), TEXT = new Color(0x1F2937),
            MUTED = new Color(0x6B7280), SUCCESS = new Color(0x10B981), DANGER = new Color(0xEF4444),
            WARN = new Color(0xF59E0B), SIDEBAR = new Color(0x111827), LINE = new Color(0xE5E7EB), GRAY = new Color(0x4B5563);
    // dark-theme colours (main window)
    static final Color INK = new Color(0xF1F5F9), INK2 = new Color(0xA5B0D0), DEEP0 = new Color(0x070B1A), DEEP1 = new Color(0x1B1245),
            CYAN = new Color(0x22D3EE), VIOLET = new Color(0x8B5CF6), PINK = new Color(0xEC4899);
    static final Color[] PALETTE = {new Color(0x4F46E5), new Color(0x10B981), new Color(0xF59E0B), new Color(0xEF4444),
            new Color(0x06B6D4), new Color(0x8B5CF6), new Color(0xEC4899), new Color(0x84CC16)};

    /** Set ui.animations=false in config.properties to switch the moving effects off (older PCs). */
    static final boolean FX = !"false".equalsIgnoreCase(Config.get("ui.animations", "true"));
    /** Seconds-ish clock advanced by the main window; drives drifting glow and pulsing. */
    static volatile double clock;

    record Item(int id, String label) {
        @Override public String toString() { return label; }
    }

    interface Job { String run() throws Exception; }

    static Font font(int style, int size) { return new Font("Segoe UI", style, size); }

    static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a))); }

    static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    static void setup() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        for (String k : new String[]{"Label.font", "TextField.font", "TextArea.font", "ComboBox.font", "Button.font",
                "Table.font", "TabbedPane.font", "OptionPane.messageFont", "OptionPane.buttonFont", "PasswordField.font"})
            UIManager.put(k, font(Font.PLAIN, 13));
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("Panel.background", Color.WHITE);
    }

    static int num(Object o) { return o instanceof Number n ? n.intValue() : 0; }

    // ---------- components ----------

    /** Glossy 3D button: shadow, gradient face, gloss, glow ring on hover (eased), sinks when pressed. */
    static class RoundBtn extends JButton {
        private Color base;
        private boolean hover;
        private float hv;
        private Timer fade;

        RoundBtn(String t, Color c) {
            super(t);
            base = c;
            setForeground(Color.WHITE);
            setFont(font(Font.BOLD, 13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(new EmptyBorder(9, 18, 13, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; ease(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; ease(); }
            });
        }

        private void ease() {
            if (!FX) { hv = hover ? 1 : 0; repaint(); return; }
            if (fade == null) fade = new Timer(16, e -> {
                float tg = hover ? 1f : 0f;
                hv += (tg - hv) * 0.3f;
                if (Math.abs(tg - hv) < 0.02f) { hv = tg; fade.stop(); }
                repaint();
            });
            fade.start();
        }

        void setBase(Color c) { base = c; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            boolean down = getModel().isPressed() && isEnabled();
            int w = getWidth(), h = getHeight() - 4, dy = down ? 2 : 0;
            Color c = !isEnabled() ? new Color(0x8B90A0) : mix(base, base.brighter(), hv);
            g2.setColor(new Color(0, 0, 0, down ? 25 : 55));
            g2.fillRoundRect(2, 4 + dy, w - 4, h, 14, 14);
            g2.setColor(c.darker().darker());
            g2.fillRoundRect(0, 3 + dy, w, h, 14, 14);
            g2.setPaint(new GradientPaint(0, dy, c.brighter(), 0, h + dy, c));
            g2.fillRoundRect(0, dy, w, h, 14, 14);
            Shape old = g2.getClip();
            g2.clip(new RoundRectangle2D.Float(0, dy, w, h, 14, 14));
            g2.setPaint(new GradientPaint(0, dy, new Color(255, 255, 255, 95), 0, dy + h / 2f, new Color(255, 255, 255, 5)));
            g2.fillRect(0, dy, w, h / 2);
            g2.setClip(old);
            g2.setStroke(new BasicStroke(1f + hv * 1.5f));
            g2.setColor(new Color(255, 255, 255, (int) (70 + 130 * hv)));
            g2.drawRoundRect(0, dy, w - 1, h - 1, 14, 14);
            g2.dispose();
            Graphics2D t = (Graphics2D) g.create();
            t.translate(0, dy - 2);
            super.paintComponent(t);
            t.dispose();
        }
    }

    /** Frosted-glass panel with a soft coloured glow around it. */
    static class Card extends JPanel {
        private Color glow = VIOLET;

        Card(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(20, 22, 22, 22));
        }

        Card glow(Color c) { glow = c; repaint(); return this; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight(), m = 7;
            float pulse = FX ? (float) (1 + 0.25 * Math.sin(Ui.clock * 1.6)) : 1f;
            for (int k = 0; k < m; k++) {
                g2.setColor(alpha(glow, (int) ((3 + k * 1.4) * pulse)));
                g2.fillRoundRect(k, k + 1, w - 2 * k, h - 2 * k - 1, 28 - k, 28 - k);
            }
            RoundRectangle2D body = new RoundRectangle2D.Float(m, m - 1, w - 2 * m, h - 2 * m, 20, 20);
            g2.setColor(new Color(10, 14, 40, 120));
            g2.fill(body);
            g2.setPaint(new GradientPaint(0, m, new Color(255, 255, 255, 34), 0, h - m, new Color(255, 255, 255, 9)));
            g2.fill(body);
            g2.setStroke(new BasicStroke(1.2f));
            g2.setPaint(new GradientPaint(m, m, new Color(255, 255, 255, 120), w - m, h - m, alpha(glow, 110)));
            g2.draw(body);
            g2.dispose();
        }
    }

    /** Gradient stat tile; the number counts up when set. */
    static class StatCard extends Card {
        private final JLabel value = label("0", Font.BOLD, 34, Color.WHITE);
        private final Color accent;
        private Timer anim;

        StatCard(String title, Color accent) {
            super(new BorderLayout(0, 6));
            this.accent = accent;
            add(label(title.toUpperCase(), Font.BOLD, 11, new Color(255, 255, 255, 220)), BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
            setBorder(new EmptyBorder(20, 24, 24, 18));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            float pulse = FX ? (float) (1 + 0.3 * Math.sin(Ui.clock * 1.8 + accent.getRed())) : 1f;
            for (int i = 0; i < 7; i++) {
                g2.setColor(alpha(accent, (int) ((10 + i * 2) * pulse)));
                g2.fillRoundRect(i, i + 3, w - 2 * i, h - 2 * i - 3, 28 - i, 28 - i);
            }
            Shape face = new RoundRectangle2D.Float(7, 4, w - 14, h - 14, 22, 22);
            g2.setPaint(new GradientPaint(0, 0, accent.brighter(), w, h, accent.darker().darker()));
            g2.fill(face);
            Shape old = g2.getClip();
            g2.clip(face);
            g2.setColor(new Color(255, 255, 255, 36));
            g2.fillOval(w - 95, -45, 150, 150);
            g2.fillOval(w - 45, h - 55, 95, 95);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 75), 0, h / 2f, new Color(255, 255, 255, 0)));
            g2.fillRect(0, 0, w, h / 2);
            g2.setClip(old);
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(255, 255, 255, 110));
            g2.draw(face);
            g2.dispose();
        }

        void setValue(String v) {
            if (anim != null) anim.stop();
            final int target;
            try { target = Integer.parseInt(v.trim()); } catch (NumberFormatException e) { value.setText(v); return; }
            if (!FX) { value.setText(v); return; }
            final long t0 = System.nanoTime();
            anim = new Timer(16, e -> {
                double p = Math.min(1, (System.nanoTime() - t0) / 7e8);
                p = 1 - Math.pow(1 - p, 3);
                value.setText(String.valueOf((int) Math.round(target * p)));
                if (p >= 1) ((Timer) e.getSource()).stop();
            });
            anim.start();
        }
    }

    /** Page title: white-to-cyan gradient text with a soft violet glow. */
    static class GradLabel extends JLabel {
        GradLabel(String t, int size) { super(t); setFont(font(Font.BOLD, size)); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = 2, y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(alpha(VIOLET, 22));
            for (int dx = -3; dx <= 3; dx += 3) for (int dy = -3; dy <= 3; dy += 3) g2.drawString(getText(), x + dx, y + dy);
            g2.setPaint(new GradientPaint(0, 0, Color.WHITE, Math.max(40, fm.stringWidth(getText())), 0, new Color(0x7DD3FC)));
            g2.drawString(getText(), x, y);
            g2.dispose();
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 8, d.height + 4);
        }
    }

    static JLabel label(String t, int style, int size, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(font(style, size));
        l.setForeground(c);
        return l;
    }

    static RoundBtn button(String t, Color c) { return new RoundBtn(t, c); }

    static void styleInput(JComponent f) {
        f.setFont(font(Font.PLAIN, 13));
        Border idle = new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(7, 10, 7, 10));
        Border focus = new CompoundBorder(new LineBorder(VIOLET, 2, true), new EmptyBorder(6, 9, 6, 9));
        f.setBorder(idle);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.setBorder(focus); }
            @Override public void focusLost(FocusEvent e) { f.setBorder(idle); }
        });
    }

    /** Rounded glass text box for the dark theme (search boxes, chat input). */
    static class SearchField extends JTextField {
        private final String hint;

        SearchField(int cols, String hint) {
            super(cols);
            this.hint = hint;
            setOpaque(false);
            setForeground(INK);
            setCaretColor(CYAN);
            setSelectionColor(alpha(VIOLET, 170));
            setFont(font(Font.PLAIN, 13));
            setBorder(new EmptyBorder(8, 18, 8, 16));
            setPreferredSize(new Dimension(280, 38));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            boolean f = hasFocus();
            g2.setColor(new Color(255, 255, 255, f ? 40 : 24));
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setStroke(new BasicStroke(f ? 2f : 1f));
            g2.setColor(f ? alpha(CYAN, 210) : new Color(255, 255, 255, 70));
            g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && !f) {
                Graphics2D t = aa(g);
                t.setFont(getFont());
                t.setColor(INK2);
                FontMetrics fm = t.getFontMetrics();
                t.drawString(hint, getInsets().left, (h - fm.getHeight()) / 2 + fm.getAscent());
                t.dispose();
            }
        }
    }

    static JTextField field(int cols) { JTextField f = new JTextField(cols); styleInput(f); return f; }
    static JPasswordField pass(int cols) { JPasswordField f = new JPasswordField(cols); styleInput(f); return f; }

    static JScrollPane textArea(JTextArea a, int rows) {
        a.setRows(rows);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(font(Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(new LineBorder(LINE, 1, true));
        return sp;
    }

    static <T> JComboBox<T> combo() {
        JComboBox<T> c = new JComboBox<>();
        c.setFont(font(Font.PLAIN, 13));
        c.setPreferredSize(new Dimension(220, 36));
        return c;
    }

    static JSpinner dateSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        s.setPreferredSize(new Dimension(120, 36));
        return s;
    }

    static JSpinner timeSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm"));
        s.setPreferredSize(new Dimension(90, 36));
        return s;
    }

    static String dateOf(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    static String timeOf(JSpinner s) {
        return DateTimeFormatter.ofPattern("HH:mm").format(((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()));
    }

    static void setDate(JSpinner s, String d) {
        s.setValue(Date.from(LocalDate.parse(d).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    static void setTime(JSpinner s, String t) {
        s.setValue(Date.from(LocalDate.now().atTime(LocalTime.parse(t)).atZone(ZoneId.systemDefault()).toInstant()));
    }

    /** Stacked "label above input" row for GridBagLayout forms (light background). */
    static void row(JPanel p, int y, String label, JComponent c) { row(p, y, label, c, MUTED); }

    /** Same, with a custom label colour (use INK2 on the dark glass cards). */
    static void row(JPanel p, int y, String label, JComponent c, Color labelColor) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = y * 2; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(y == 0 ? 0 : 10, 0, 3, 0);
        p.add(label(label, Font.BOLD, 12, labelColor), g);
        g = new GridBagConstraints();
        g.gridx = 0; g.gridy = y * 2 + 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        p.add(c, g);
    }

    static JPanel pair(JComponent a, JComponent b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 0));
        p.setOpaque(false);
        p.add(a);
        p.add(b);
        return p;
    }

    static JPanel bar(int gap, JComponent... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        for (JComponent c : cs) p.add(c);
        return p;
    }

    // ---------- tables ----------
    static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return getRowCount() > 0 && getValueAt(0, c) != null ? getValueAt(0, c).getClass() : Object.class;
            }
        };
    }

    static void fill(DefaultTableModel m, List<Object[]> rows) {
        m.setRowCount(0);
        for (Object[] r : rows) m.addRow(r);
    }

    private static Color pillColor(String s) {
        return switch (s) {
            case "ATTENDED", "Sent", "Open" -> new Color(0x34D399);
            case "REGISTERED" -> new Color(0x38BDF8);
            case "CANCELLED", "Full" -> new Color(0xF87171);
            case "Pending" -> new Color(0xFBBF24);
            case "Closed", "Not issued" -> new Color(0x94A3B8);
            default -> null;
        };
    }

    /** Row-card cell: rounded translucent row, hover glow, glowing selection, status shown as a pill. */
    private static final class Cell extends DefaultTableCellRenderer {
        private boolean sel, hov;
        private int row, col, cols;
        private Color pill;

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            sel = s;
            row = r;
            col = c;
            cols = t.getColumnCount();
            hov = t.getClientProperty("hoverRow") instanceof Integer i && i == r;
            String txt = v == null ? "" : v.toString();
            pill = pillColor(txt);
            setOpaque(false);
            setFont(font(pill != null ? Font.BOLD : Font.PLAIN, pill != null ? 11 : 13));
            setForeground(sel ? Color.WHITE : pill != null ? pill : INK);
            setHorizontalAlignment(v instanceof Number ? CENTER : LEFT);
            setBorder(new EmptyBorder(0, pill != null ? 25 : 14, 0, 10));
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            int x0 = col == 0 ? 2 : -20, x1 = col == cols - 1 ? w - 2 : w + 20;
            Shape rowShape = new RoundRectangle2D.Float(x0, 3, x1 - x0, h - 6, 16, 16);
            if (sel) {
                g2.setPaint(new GradientPaint(0, 0, new Color(99, 102, 241, 200), w, 0, new Color(139, 92, 246, 160)));
                g2.fill(rowShape);
                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(new Color(255, 255, 255, 90));
                g2.draw(rowShape);
                if (col == 0) { g2.setColor(CYAN); g2.fillRoundRect(7, 12, 3, h - 24, 3, 3); }
            } else if (hov) {
                g2.setColor(alpha(CYAN, 34));
                g2.fill(rowShape);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(alpha(CYAN, 95));
                g2.draw(rowShape);
            } else {
                g2.setColor(new Color(255, 255, 255, row % 2 == 0 ? 15 : 8));
                g2.fill(rowShape);
            }
            if (pill != null) {
                g2.setFont(getFont());
                int tw = g2.getFontMetrics().stringWidth(getText()), ph = 22, py = (h - ph) / 2;
                g2.setColor(alpha(pill, 42));
                g2.fillRoundRect(10, py, tw + 30, ph, ph, ph);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(alpha(pill, 170));
                g2.drawRoundRect(10, py, tw + 30, ph, ph, ph);
                g2.setColor(pill);
                g2.fillOval(19, h / 2 - 3, 6, 6);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Header cell: glass bar with a neon underline that shifts violet -> cyan across the columns. */
    private static final class Head extends DefaultTableCellRenderer {
        private int col, cols;

        @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(tb, v, false, false, r, c);
            col = c;
            cols = tb.getColumnCount();
            String t = v == null ? "" : v.toString().toUpperCase();
            RowSorter<?> rs = tb.getRowSorter();
            if (rs != null) for (RowSorter.SortKey k : rs.getSortKeys())
                if (k.getColumn() == tb.convertColumnIndexToModel(c))
                    t += k.getSortOrder() == SortOrder.ASCENDING ? "  ▲" : k.getSortOrder() == SortOrder.DESCENDING ? "  ▼" : "";
            setText(t);
            setOpaque(false);
            setFont(font(Font.BOLD, 11));
            setForeground(INK2);
            setHorizontalAlignment(LEFT);
            setBorder(new EmptyBorder(0, 14, 2, 10));
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            int x0 = col == 0 ? 2 : -20, x1 = col == cols - 1 ? w - 2 : w + 20;
            Shape bar = new RoundRectangle2D.Float(x0, 2, x1 - x0, h - 5, 14, 14);
            g2.setPaint(new GradientPaint(0, 0, alpha(VIOLET, 80), 0, h, alpha(PRIMARY, 30)));
            g2.fill(bar);
            Shape old = g2.getClip();
            g2.clip(bar);
            g2.setColor(alpha(mix(VIOLET, CYAN, col / (float) Math.max(1, cols - 1)), 230));
            g2.fillRect(0, h - 5, w, 2);
            g2.setClip(old);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m) {
            @Override public void paint(Graphics g) {
                super.paint(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = aa(g);
                    g2.setFont(font(Font.PLAIN, 14));
                    g2.setColor(INK2);
                    String s = "Nothing here yet";
                    g2.drawString(s, (getWidth() - g2.getFontMetrics().stringWidth(s)) / 2, 50);
                    g2.dispose();
                }
            }
        };
        Cell cell = new Cell();
        t.setDefaultRenderer(Object.class, cell);
        t.setDefaultRenderer(Number.class, cell);
        t.setOpaque(false);
        t.setRowHeight(44);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFont(font(Font.PLAIN, 13));
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setFocusable(true);
        JTableHeader h = t.getTableHeader();
        h.setOpaque(false);
        h.setPreferredSize(new Dimension(0, 40));
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new Head());
        Timer refit = new Timer(40, e -> fit(t));   // after the rows change, size the columns once
        refit.setRepeats(false);
        m.addTableModelListener(e -> refit.restart());
        t.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int r = t.rowAtPoint(e.getPoint());
                if (!Integer.valueOf(r).equals(t.getClientProperty("hoverRow"))) { t.putClientProperty("hoverRow", r); t.repaint(); }
            }
        });
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { t.putClientProperty("hoverRow", -1); t.repaint(); }
        });
        return t;
    }

    /** Sizes each column to its header and content so labels and status pills are not cut off. */
    static void fit(JTable t) {
        TableColumnModel cm = t.getColumnModel();
        TableCellRenderer hr = t.getTableHeader().getDefaultRenderer();
        for (int c = 0; c < cm.getColumnCount(); c++) {
            TableColumn col = cm.getColumn(c);
            int w = hr.getTableCellRendererComponent(t, col.getHeaderValue(), false, false, -1, c).getPreferredSize().width + 28;
            for (int r = 0; r < Math.min(t.getRowCount(), 60); r++)
                w = Math.max(w, t.prepareRenderer(t.getCellRenderer(r, c), r, c).getPreferredSize().width + 28);
            col.setPreferredWidth(Math.max(64, Math.min(320, w)));
        }
    }

    private static final class Thin extends BasicScrollBarUI {
        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }

        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = aa(g);
            g2.setColor(new Color(255, 255, 255, isThumbRollover() || isDragging ? 120 : 62));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    /** Transparent scroll pane with slim rounded scrollbars (for use on the dark glass). */
    static JScrollPane thin(JScrollPane sp) {
        // an EmptyBorder (not null) so JTable does not put its own white scroll-pane border back
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(null);
        for (String corner : new String[]{JScrollPane.UPPER_TRAILING_CORNER, JScrollPane.LOWER_TRAILING_CORNER}) {
            JPanel c = new JPanel();
            c.setOpaque(false);
            sp.setCorner(corner, c);
        }
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        if (sp.getColumnHeader() != null) sp.getColumnHeader().setOpaque(false);
        for (JScrollBar b : new JScrollBar[]{sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) {
            b.setUI(new Thin());
            b.setOpaque(false);
            b.setPreferredSize(new Dimension(10, 10));
            b.setUnitIncrement(18);
        }
        return sp;
    }

    static JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t) {
            // the header viewport is created later by JTable, so make it see-through here
            @Override public void setColumnHeaderView(Component v) {
                super.setColumnHeaderView(v);
                if (getColumnHeader() != null) getColumnHeader().setOpaque(false);
            }
        };
        return thin(sp);
    }

    /** Model-row index of the selected row's value in column {@code col}, or -1. */
    static int selId(JTable t, int col) {
        int r = t.getSelectedRow();
        return r < 0 ? -1 : num(t.getModel().getValueAt(t.convertRowIndexToModel(r), col));
    }

    static java.util.List<Integer> selIds(JTable t, int col) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (int r : t.getSelectedRows()) out.add(num(t.getModel().getValueAt(t.convertRowIndexToModel(r), col)));
        return out;
    }

    static void attachSearch(JTable t, JTextField f) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            void go() {
                @SuppressWarnings("unchecked")
                TableRowSorter<TableModel> s = (TableRowSorter<TableModel>) t.getRowSorter();
                String q = f.getText().trim();
                s.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
            }
            public void insertUpdate(DocumentEvent e) { go(); }
            public void removeUpdate(DocumentEvent e) { go(); }
            public void changedUpdate(DocumentEvent e) { go(); }
        });
    }

    // ---------- dialogs ----------
    static void info(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Smart Event Manager", JOptionPane.INFORMATION_MESSAGE); }
    static void error(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Something went wrong", JOptionPane.ERROR_MESSAGE); }
    static boolean confirm(Component p, String m) {
        return JOptionPane.showConfirmDialog(p, m, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Runs a slow job (email, network) off the UI thread, then shows its result message. */
    static void run(Component parent, Job job, Runnable after) {
        Window w = SwingUtilities.getWindowAncestor(parent);
        if (w != null) w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return job.run(); }
            @Override protected void done() {
                if (w != null) w.setCursor(Cursor.getDefaultCursor());
                try {
                    String msg = get();
                    if (msg != null && !msg.isEmpty()) info(parent, msg);
                } catch (Exception e) {
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    if (c instanceof LinkageError)
                        error(parent, "A required library is missing (" + c.getMessage() + ").\nStart the app with:  java -cp \"out;lib/*\" eventms.Main\nand make sure the lib folder has the jakarta.mail and jakarta.activation jars.");
                    else
                        error(parent, c.getMessage() == null ? c.toString() : c.getMessage());
                }
                if (after != null) after.run();
            }
        }.execute();
    }
}
