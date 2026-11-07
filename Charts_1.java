package eventms;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.Map;

/** Animated 3D bar and pie charts for the report page. */
final class Charts {
    private Charts() { }

    private static Graphics2D g2(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    private static boolean noData(Graphics2D g, Map<String, Number> d, int w, int h) {
        if (d != null && d.values().stream().anyMatch(v -> v.doubleValue() > 0)) return false;
        g.setColor(Ui.MUTED);
        g.setFont(Ui.font(Font.PLAIN, 13));
        String s = "No data yet";
        g.drawString(s, (w - g.getFontMetrics().stringWidth(s)) / 2, h / 2);
        return true;
    }

    static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "…"; }

    private static Color shade(Color c, double f) {
        return new Color(Math.max(0, Math.min(255, (int) (c.getRed() * f))), Math.max(0, Math.min(255, (int) (c.getGreen() * f))),
                Math.max(0, Math.min(255, (int) (c.getBlue() * f))));
    }

    /** Chart base: holds the data and a 0..1 "grow" animation value. */
    abstract static class Animated extends JComponent {
        Map<String, Number> data;
        float prog = 1f;
        private Timer anim;

        Animated() { setPreferredSize(new Dimension(320, 230)); }

        void setData(Map<String, Number> d) {
            data = d;
            if (anim != null) anim.stop();
            prog = 0;
            final long t0 = System.nanoTime();
            anim = new Timer(16, e -> {
                float p = Math.min(1f, (System.nanoTime() - t0) / 8e8f);
                prog = 1 - (float) Math.pow(1 - p, 3);
                repaint();
                if (p >= 1f) ((Timer) e.getSource()).stop();
            });
            anim.start();
        }
    }

    static class Bar extends Animated {
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = g2(g0);
            int w = getWidth(), h = getHeight();
            if (noData(g, data, w, h)) { g.dispose(); return; }
            int left = 34, bottom = 34, top = 24, right = 20;
            double max = data.values().stream().mapToDouble(Number::doubleValue).max().orElse(1);
            max = Math.max(1, Math.ceil(max));
            int ph = h - top - bottom, pw = w - left - right;
            g.setFont(Ui.font(Font.PLAIN, 11));
            for (int i = 0; i <= 4; i++) {
                int y = top + ph - ph * i / 4;
                g.setColor(Ui.LINE);
                g.drawLine(left, y, w - right + 10, y);
                g.setColor(Ui.MUTED);
                g.drawString(String.valueOf((int) Math.round(max * i / 4)), 4, y + 4);
            }
            int n = data.size(), slot = pw / n, bw = Math.max(10, Math.min(44, slot - 18)), d = Math.max(5, Math.min(11, bw / 3)), i = 0;
            for (Map.Entry<String, Number> e : data.entrySet()) {
                int bh = (int) (ph * e.getValue().doubleValue() / max * prog);
                int x = left + i * slot + (slot - bw) / 2, base = top + ph, y = base - bh;
                Color c = Ui.PALETTE[i % Ui.PALETTE.length];
                // floor shadow
                g.setColor(new Color(0, 0, 0, 40));
                g.fill(new Ellipse2D.Double(x - 2, base - 3, bw + d + 6, 9));
                // side face
                Path2D side = new Path2D.Double();
                side.moveTo(x + bw, y);
                side.lineTo(x + bw + d, y - d);
                side.lineTo(x + bw + d, base - d);
                side.lineTo(x + bw, base);
                side.closePath();
                g.setColor(shade(c, 0.62));
                g.fill(side);
                // front face
                g.setPaint(new GradientPaint(x, 0, shade(c, 1.12), x + bw, 0, shade(c, 0.85)));
                g.fillRect(x, y, bw, bh);
                // top face
                Path2D topFace = new Path2D.Double();
                topFace.moveTo(x, y);
                topFace.lineTo(x + d, y - d);
                topFace.lineTo(x + bw + d, y - d);
                topFace.lineTo(x + bw, y);
                topFace.closePath();
                g.setColor(shade(c, 1.3));
                g.fill(topFace);
                g.setColor(new Color(255, 255, 255, 60));
                g.fillRect(x + 2, y, Math.max(2, bw / 5), bh);   // gloss strip
                g.setColor(Ui.TEXT);
                g.setFont(Ui.font(Font.BOLD, 11));
                String v = String.valueOf((int) Math.round(e.getValue().doubleValue() * prog));
                g.drawString(v, x + (bw - g.getFontMetrics().stringWidth(v)) / 2 + d / 2, y - d - 4);
                g.setColor(Ui.MUTED);
                g.setFont(Ui.font(Font.PLAIN, 11));
                String lbl = cut(e.getKey(), Math.max(5, slot / 6));
                g.drawString(lbl, left + i * slot + (slot - g.getFontMetrics().stringWidth(lbl)) / 2, h - 12);
                i++;
            }
            g.dispose();
        }
    }

    static class Pie extends Animated {
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = g2(g0);
            int w = getWidth(), h = getHeight();
            if (noData(g, data, w, h)) { g.dispose(); return; }
            double total = data.values().stream().mapToDouble(Number::doubleValue).sum();
            int thick = 16;
            int dw = Math.min((int) (w * 0.52), (int) ((h - thick - 24) / 0.62));
            int dh = (int) (dw * 0.62);
            int x = 10, y = (h - dh - thick) / 2;
            g.setColor(new Color(0, 0, 0, 38));
            g.fill(new Ellipse2D.Double(x + 4, y + thick + 6, dw, dh));      // ground shadow
            // extruded side (draw darker copies stepping down), then the lit top
            for (int pass = 0; pass < 2; pass++) {
                for (int dy = pass == 0 ? thick : 0; dy >= (pass == 0 ? 1 : 0); dy -= (pass == 0 ? 2 : 1)) {
                    double start = 90;
                    int i = 0;
                    for (Map.Entry<String, Number> e : data.entrySet()) {
                        double ext = -360.0 * e.getValue().doubleValue() / total * prog;
                        Color c = Ui.PALETTE[i % Ui.PALETTE.length];
                        if (pass == 0) g.setColor(shade(c, 0.6));
                        else g.setPaint(new GradientPaint(x, y, shade(c, 1.18), x + dw, y + dh, shade(c, 0.92)));
                        g.fill(new Arc2D.Double(x, y + dy, dw, dh, start, ext, Arc2D.PIE));
                        start += ext;
                        i++;
                    }
                    if (pass == 1) break;
                }
            }
            // white slice separators + glossy highlight
            double start = 90;
            g.setStroke(new BasicStroke(1.6f));
            g.setColor(Color.WHITE);
            for (Map.Entry<String, Number> e : data.entrySet()) {
                double ext = -360.0 * e.getValue().doubleValue() / total * prog;
                g.draw(new Arc2D.Double(x, y, dw, dh, start, ext, Arc2D.PIE));
                start += ext;
            }
            g.setPaint(new GradientPaint(0, y, new Color(255, 255, 255, 70), 0, y + dh / 2f, new Color(255, 255, 255, 0)));
            g.fill(new Ellipse2D.Double(x + dw * 0.1, y + 2, dw * 0.8, dh * 0.45));
            // legend
            g.setFont(Ui.font(Font.PLAIN, 12));
            int ly = Math.max(16, (h - data.size() * 22) / 2 + 12);
            int i = 0;
            for (Map.Entry<String, Number> e : data.entrySet()) {
                int lx = x + dw + 22;
                Color c = Ui.PALETTE[i % Ui.PALETTE.length];
                g.setPaint(new GradientPaint(lx, ly - 10, shade(c, 1.2), lx, ly + 2, shade(c, 0.85)));
                g.fillRoundRect(lx, ly - 10, 12, 12, 4, 4);
                g.setColor(Ui.TEXT);
                g.drawString(cut(e.getKey(), 16) + "  " + e.getValue().intValue(), lx + 18, ly);
                ly += 22;
                i++;
            }
            g.dispose();
        }
    }
}
