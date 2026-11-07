package eventms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Base class for every screen shown in the main window. */
abstract class Page extends JPanel {
    private float intro = 1f;
    private Timer introTimer;

    Page() {
        super(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(24, 28, 24, 28));
    }

    /** Dark gradient with slowly drifting coloured glows. */
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, Ui.DEEP0, w, h, Ui.DEEP1));
        g2.fillRect(0, 0, w, h);
        double t = Ui.clock;
        Color[] oc = {Ui.VIOLET, Ui.CYAN, Ui.PINK};
        for (int i = 0; i < oc.length; i++) {
            float cx = (float) (w * (0.15 + 0.35 * i) + Math.sin(t * 0.35 + i * 2.1) * w * 0.06);
            float cy = (float) (h * (0.2 + 0.28 * i) + Math.cos(t * 0.28 + i * 1.3) * h * 0.07);
            float r = 300 + i * 50;
            g2.setPaint(new RadialGradientPaint(cx, cy, r, new float[]{0f, 1f}, new Color[]{Ui.alpha(oc[i], 62), Ui.alpha(oc[i], 0)}));
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
    }

    /** Children fade and slide up while the page "plays in". */
    @Override protected void paintChildren(Graphics g) {
        if (intro >= 1f) { super.paintChildren(g); return; }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, intro))));
        g2.translate(0, (int) ((1 - intro) * 34));
        super.paintChildren(g2);
        g2.dispose();
    }

    /** Transition shown each time the page is opened. */
    void playIn() {
        if (!Ui.FX) return;
        if (introTimer != null) introTimer.stop();
        intro = 0f;
        final long t0 = System.nanoTime();
        introTimer = new Timer(15, e -> {
            float p = Math.min(1f, (System.nanoTime() - t0) / 4.5e8f);
            intro = 1 - (float) Math.pow(1 - p, 3);
            repaint();
            if (p >= 1f) { intro = 1f; ((Timer) e.getSource()).stop(); }
        });
        introTimer.start();
    }

    /** Reload data from the database. Called each time the page is opened. */
    abstract void refresh();

    JPanel header(String title, String sub, JComponent... right) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JPanel l = new JPanel();
        l.setOpaque(false);
        l.setLayout(new BoxLayout(l, BoxLayout.Y_AXIS));
        JLabel a = new Ui.GradLabel(title, 28), b = Ui.label(sub, Font.PLAIN, 13, Ui.INK2);
        a.setAlignmentX(LEFT_ALIGNMENT);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setBorder(new EmptyBorder(0, 3, 0, 0));
        l.add(a);
        l.add(Box.createVerticalStrut(2));
        l.add(b);
        h.add(l, BorderLayout.WEST);
        if (right.length > 0) h.add(Ui.bar(8, right), BorderLayout.EAST);
        return h;
    }

    /** Glass card with a search box and buttons above a table (buttons wrap to a second row when there are many). */
    JPanel tableCard(JTable t, JTextField search, JComponent... buttons) {
        Ui.Card c = new Ui.Card(new BorderLayout(0, 12));
        JPanel left = Ui.bar(0, Ui.label("Search  ", Font.BOLD, 12, Ui.INK2), search);
        JPanel top = new JPanel();
        top.setOpaque(false);
        if (buttons.length <= 4) {
            top.setLayout(new BorderLayout(12, 0));
            top.add(left, BorderLayout.WEST);
            top.add(Ui.bar(8, buttons), BorderLayout.EAST);
        } else {
            top.setLayout(new BorderLayout(0, 10));
            top.add(left, BorderLayout.NORTH);
            top.add(Ui.bar(8, buttons), BorderLayout.CENTER);
        }
        c.add(top, BorderLayout.NORTH);
        c.add(Ui.scroll(t), BorderLayout.CENTER);
        Ui.attachSearch(t, search);
        return c;
    }

    int needRow(JTable t, String what) {
        int id = Ui.selId(t, 0);
        if (id < 0) Ui.info(this, "Please select a " + what + " first.");
        return id;
    }
}
