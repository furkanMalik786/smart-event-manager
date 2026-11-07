package eventms;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/** Draws entry passes and certificates as images (shown on screen and attached to emails). */
final class Art {
    private Art() { }

    private static Graphics2D canvas(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g;
    }

    private static String fit(Graphics2D g, String s, int maxW) {
        if (g.getFontMetrics().stringWidth(s) <= maxW) return s;
        while (s.length() > 1 && g.getFontMetrics().stringWidth(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private static void center(Graphics2D g, String s, int cx, int y) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }

    private static void text(Graphics2D g, String label, String value, int x, int y, int maxW) {
        g.setColor(Ui.MUTED);
        g.setFont(Ui.font(Font.BOLD, 12));
        g.drawString(label.toUpperCase(), x, y);
        g.setColor(Ui.TEXT);
        g.setFont(Ui.font(Font.BOLD, 20));
        g.drawString(fit(g, value, maxW), x, y + 26);
    }

    private static void barcode(Graphics2D g, String code, int x, int y, int h) {
        g.setColor(Ui.TEXT);
        for (byte b : code.getBytes()) {
            for (int bit = 7; bit >= 0; bit--) {
                if ((b >> bit & 1) == 1) g.fillRect(x, y, 2, h);
                x += 3;
            }
        }
    }

    static BufferedImage pass(String name, String event, String date, String time, String venue, String code) {
        int w = 900, h = 340;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(img);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setPaint(new GradientPaint(0, 0, Ui.PRIMARY, 0, h, new Color(0x7C3AED)));
        g.fillRect(0, 0, 230, h);
        g.setColor(Color.WHITE);
        g.setFont(Ui.font(Font.BOLD, 34));
        center(g, "ENTRY", 115, 140);
        center(g, "PASS", 115, 182);
        g.setFont(Ui.font(Font.PLAIN, 12));
        center(g, "Smart Event Manager", 115, 300);
        // perforation
        g.setColor(new Color(0xD1D5DB));
        for (int y = 10; y < h; y += 16) g.fillOval(228, y, 5, 5);

        int x = 270, maxW = 590;
        g.setColor(Ui.PRIMARY);
        g.setFont(Ui.font(Font.BOLD, 13));
        g.drawString("ADMIT ONE", x, 48);
        g.setColor(Ui.TEXT);
        g.setFont(Ui.font(Font.BOLD, 30));
        g.drawString(fit(g, event, maxW), x, 88);
        text(g, "Participant", name, x, 130, maxW);
        text(g, "Date", date, x, 190, 190);
        text(g, "Time", time, x + 220, 190, 150);
        text(g, "Venue", venue, x, 250, maxW);
        barcode(g, code, x, 285, 34);
        g.setColor(Ui.MUTED);
        g.setFont(Ui.font(Font.BOLD, 12));
        g.drawString(code, x + 340, 308);
        g.dispose();
        return img;
    }

    // ---------------------------------------------------------------- certificate

    /** Config value, or the default when empty. */
    private static String cfg(String key, String def) {
        String v = Config.get(key);
        return v.isEmpty() ? def : v;
    }

    private static BufferedImage load(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            File f = new File(path);
            return f.isFile() ? ImageIO.read(f) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** "OF PARTICIPATION" -> "O F   P A R T I C I P A T I O N" */
    private static String spaced(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(c == ' ' ? "  " : c + " ");
        return sb.toString().trim();
    }

    /** First installed handwriting-style font, so typed signatures look signed. */
    private static Font script(int size) {
        java.util.Set<String> have = new java.util.HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe Script", "Lucida Handwriting", "Brush Script MT", "Monotype Corsiva", "Z003"})
            if (have.contains(f)) return new Font(f, Font.PLAIN, size);
        return new Font("Serif", Font.BOLD | Font.ITALIC, size + 4);
    }

    private static BufferedImage scaleTo(BufferedImage src, int maxW, int maxH) {
        double sc = Math.min(1.0, Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight()));
        int w = Math.max(1, (int) (src.getWidth() * sc)), h = Math.max(1, (int) (src.getHeight() * sc));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** One signature block: signature (image or typed), line, name, title. */
    private static void signer(Graphics2D g, int cx, int lineY, String name, String title, String sigPath) {
        BufferedImage sig = load(sigPath);
        if (sig != null) {
            BufferedImage s = scaleTo(sig, 230, 78);
            g.drawImage(s, cx - s.getWidth() / 2, lineY - s.getHeight() - 4, null);
        } else if (!name.isBlank()) {
            g.setColor(new Color(0x1E3A8A));
            int size = 36;
            g.setFont(script(size));
            while (size > 16 && g.getFontMetrics().stringWidth(name) > 250) g.setFont(script(--size));   // shrink to fit, never cut
            center(g, name, cx, lineY - 12);
        }
        g.setColor(Ui.TEXT);
        g.fillRect(cx - 125, lineY, 250, 2);
        int y = lineY + 26;
        if (!name.isBlank()) {
            g.setFont(Ui.font(Font.BOLD, 19));
            center(g, fit(g, name, 260), cx, y);
            y += 22;
        }
        g.setColor(Ui.MUTED);
        g.setFont(Ui.font(Font.PLAIN, 15));
        if (!title.isBlank()) center(g, fit(g, title, 260), cx, y);
    }

    /**
     * Certificate with the look configured in Certificate design (heading, college name, logo, up to two signers
     * with signature images). {@code host} is the event's host/organiser; it is printed on the certificate and
     * is used as signer 1's name when that has been left blank.
     */
    static BufferedImage certificate(String name, String event, String date, String code, String host) {
        host = host == null ? "" : host.trim();
        int w = 1100, h = 880;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(img);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        Color gold = new Color(0xB8860B);
        g.setColor(Ui.PRIMARY);
        g.setStroke(new BasicStroke(10));
        g.drawRect(24, 24, w - 48, h - 48);
        g.setColor(gold);
        g.setStroke(new BasicStroke(2));
        g.drawRect(44, 44, w - 88, h - 88);
        int cx = w / 2;

        BufferedImage logo = load(Config.get("cert.logo"));
        if (logo != null) {
            BufferedImage l = scaleTo(logo, 200, 72);
            g.drawImage(l, cx - l.getWidth() / 2, 62, null);
        }
        String org = Config.get("cert.org");
        if (!org.isEmpty()) {
            g.setColor(Ui.TEXT);
            g.setFont(Ui.font(Font.BOLD, 20));
            center(g, fit(g, org.toUpperCase(), w - 220), cx, 158);
        }

        g.setColor(Ui.PRIMARY);
        g.setFont(new Font("Serif", Font.BOLD, 62));
        center(g, fit(g, cfg("cert.title", "CERTIFICATE"), w - 200), cx, 232);
        String sub = Config.get("cert.subtitle").isEmpty() && !Config.get("cert.title").isEmpty() ? "" : cfg("cert.subtitle", "OF PARTICIPATION");
        if (!sub.isEmpty()) {
            g.setColor(gold);
            g.setFont(Ui.font(Font.BOLD, 19));
            center(g, spaced(sub), cx, 272);
        }
        g.setColor(Ui.MUTED);
        g.setFont(Ui.font(Font.PLAIN, 20));
        center(g, "This is to certify that", cx, 345);
        g.setColor(Ui.TEXT);
        g.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 56));
        center(g, fit(g, name, w - 220), cx, 420);
        g.setColor(Ui.LINE);
        g.fillRect(cx - 280, 440, 560, 2);
        g.setColor(Ui.MUTED);
        g.setFont(Ui.font(Font.PLAIN, 20));
        center(g, "has successfully attended and participated in", cx, 495);
        g.setColor(Ui.PRIMARY);
        g.setFont(Ui.font(Font.BOLD, 34));
        center(g, fit(g, event, w - 220), cx, 555);
        g.setColor(Ui.MUTED);
        g.setFont(Ui.font(Font.PLAIN, 20));
        center(g, "held on " + date, cx, 600);
        if (!host.isEmpty()) {
            g.setFont(Ui.font(Font.ITALIC, 18));
            center(g, fit(g, "Hosted by " + host, w - 220), cx, 632);
        }

        // signers: both -> left/right, one -> centred
        String n1 = Config.get("cert.s1.name"), n2 = Config.get("cert.s2.name");
        if (n1.isEmpty()) n1 = host;
        String t1 = cfg("cert.s1.title", "Event Coordinator"), t2 = cfg("cert.s2.title", "Head of Department");
        String s1 = Config.get("cert.s1.sig"), s2 = Config.get("cert.s2.sig");
        boolean anyS1 = !Config.get("cert.s1.name").isEmpty() || !s1.isEmpty() || !Config.get("cert.s1.title").isEmpty();
        boolean anyS2 = !n2.isEmpty() || !s2.isEmpty() || !Config.get("cert.s2.title").isEmpty();
        boolean second = anyS2 || !anyS1;   // nothing set: show both default signers as before
        int lineY = 738;
        if (second) {
            signer(g, 280, lineY, n1, t1, s1);
            signer(g, w - 280, lineY, n2, t2, s2);
        } else {
            signer(g, cx, lineY, n1, t1, s1);
        }
        g.setColor(Ui.MUTED);
        g.setFont(Ui.font(Font.PLAIN, 13));
        center(g, "Certificate ID: " + code, cx, 822);
        g.dispose();
        return img;
    }

    static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ImageIO.write(img, "png", o);
        return o.toByteArray();
    }

    /** Shows the image in a dialog with a "Save as PNG" option. */
    static void preview(Component parent, BufferedImage img, String title, String fileName) {
        double sc = Math.min(1.0, 760.0 / img.getWidth());
        Image small = img.getScaledInstance((int) (img.getWidth() * sc), (int) (img.getHeight() * sc), Image.SCALE_SMOOTH);
        Object[] opts = {"Save as PNG...", "Close"};
        int r = JOptionPane.showOptionDialog(parent, new JLabel(new ImageIcon(small)), title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[1]);
        if (r != 0) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(fileName));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try {
            ImageIO.write(img, "png", fc.getSelectedFile());
            Ui.info(parent, "Saved to " + fc.getSelectedFile().getAbsolutePath());
        } catch (IOException e) {
            Ui.error(parent, e.getMessage());
        }
    }
}
