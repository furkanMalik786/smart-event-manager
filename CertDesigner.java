package eventms;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** "Certificate design" dialog: heading, college name, logo and up to two signers (name, title, signature image). */
final class CertDesigner {
    private CertDesigner() { }

    private static final String[] KEYS = {"cert.title", "cert.subtitle", "cert.org", "cert.logo",
            "cert.s1.name", "cert.s1.title", "cert.s1.sig", "cert.s2.name", "cert.s2.title", "cert.s2.sig"};

    static void open(Component parent) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String k : KEYS) snapshot.put(k, Config.get(k));

        JLabel preview = new JLabel("", SwingConstants.CENTER);
        preview.setBorder(new LineBorder(Ui.LINE));
        preview.setPreferredSize(new Dimension(540, 430));

        Map<String, JTextField> texts = new LinkedHashMap<>();
        Runnable[] render = new Runnable[1];
        Timer debounce = new Timer(300, e -> render[0].run());
        debounce.setRepeats(false);
        DocumentListener dl = new DocumentListener() {
            void changed() { apply(texts); debounce.restart(); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        };
        render[0] = () -> {
            BufferedImage img = Art.certificate("Aarav Sharma", "Code Sprint Hackathon", LocalDate.now().toString(),
                    "CERT-0000-SAMPLE", "Prof. A. Kumar");
            double sc = 530.0 / img.getWidth();
            preview.setIcon(new ImageIcon(img.getScaledInstance(530, (int) (img.getHeight() * sc), Image.SCALE_SMOOTH)));
        };

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(4, 4, 4, 12));
        int y = 0;
        y = text(form, y, "Heading", "cert.title", "CERTIFICATE", texts, dl);
        y = text(form, y, "Sub-heading", "cert.subtitle", "OF PARTICIPATION", texts, dl);
        y = text(form, y, "College / organisation name", "cert.org", "", texts, dl);
        y = file(form, y, "College logo (optional)", "cert.logo", "logo", preview, render);
        y = section(form, y, "Signer 1  (left)");
        y = text(form, y, "Name", "cert.s1.name", "", texts, dl);
        y = text(form, y, "Title / designation", "cert.s1.title", "Event Coordinator", texts, dl);
        y = file(form, y, "Signature image (PNG with transparent background is best)", "cert.s1.sig", "signature1", preview, render);
        y = section(form, y, "Signer 2  (right)");
        y = text(form, y, "Name", "cert.s2.name", "", texts, dl);
        y = text(form, y, "Title / designation", "cert.s2.title", "Head of Department", texts, dl);
        y = file(form, y, "Signature image", "cert.s2.sig", "signature2", preview, render);
        JLabel tip = Ui.label("<html><body style='width:300px'>No signature image? The name is shown in a handwriting font instead. "
                + "If signer 1's name is empty, the event's Host is used. Clear everything for signer 2 to centre a single signer.</body></html>",
                Font.PLAIN, 12, Ui.MUTED);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = y * 2; g.insets = new Insets(14, 0, 0, 0); g.anchor = GridBagConstraints.WEST;
        form.add(tip, g);

        JScrollPane sp = new JScrollPane(form);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(370, 560));
        sp.getVerticalScrollBar().setUnitIncrement(18);

        JPanel root = new JPanel(new BorderLayout(14, 0));
        root.add(sp, BorderLayout.WEST);
        root.add(preview, BorderLayout.CENTER);
        render[0].run();

        int r = JOptionPane.showConfirmDialog(parent, root, "Certificate design", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            snapshot.forEach(Config::set);   // undo the live preview changes
            return;
        }
        apply(texts);
        try {
            Config.save();
            Ui.info(parent, "Certificate design saved. New certificates and emails will use it.");
        } catch (Exception e) {
            Ui.error(parent, "Could not save config.properties: " + e.getMessage());
        }
    }

    private static void apply(Map<String, JTextField> texts) {
        texts.forEach((k, f) -> Config.set(k, f.getText()));
    }

    private static int section(JPanel p, int y, String title) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = y * 2; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(y == 0 ? 0 : 18, 0, 0, 0);
        p.add(Ui.label(title, Font.BOLD, 14, Ui.PRIMARY), g);
        return y + 1;
    }

    private static int text(JPanel p, int y, String label, String key, String hint, Map<String, JTextField> texts, DocumentListener dl) {
        JTextField f = Ui.field(22);
        // first time: show the built-in default so it is clear what will be printed
        f.setText(Config.has(key) ? Config.get(key) : hint);
        f.getDocument().addDocumentListener(dl);
        texts.put(key, f);
        Ui.row(p, y, label, f);
        return y + 1;
    }

    /** A "current file + Choose / Remove" row for an image setting. */
    private static int file(JPanel p, int y, String label, String key, String slot, JLabel preview, Runnable[] render) {
        JLabel current = Ui.label(shortName(Config.get(key)), Font.PLAIN, 12, Ui.MUTED);
        Ui.RoundBtn choose = Ui.button("Choose image...", Ui.GRAY), clear = Ui.button("Remove", Ui.DANGER);
        choose.addActionListener(e -> {
            String path = pick(preview, slot);
            if (path != null) {
                Config.set(key, path);
                current.setText(shortName(path));
                render[0].run();
            }
        });
        clear.addActionListener(e -> {
            Config.set(key, "");
            current.setText(shortName(""));
            render[0].run();
        });
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(Ui.bar(6, choose, clear), BorderLayout.WEST);
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setOpaque(false);
        box.add(row, BorderLayout.NORTH);
        box.add(current, BorderLayout.SOUTH);
        Ui.row(p, y, label, box);
        return y + 1;
    }

    private static String shortName(String path) { return path == null || path.isEmpty() ? "No image selected" : new File(path).getName(); }

    /** Lets the user pick an image, copies it into ./signatures so it keeps working, returns the stored path. */
    private static String pick(Component parent, String slot) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Images (png, jpg, gif)", "png", "jpg", "jpeg", "gif"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        try {
            if (ImageIO.read(f) == null) throw new IllegalArgumentException("That file is not a readable image.");
            String name = f.getName(), ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".png";
            Files.createDirectories(Path.of("signatures"));
            Path dest = Path.of("signatures", slot + ext.toLowerCase());
            Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toString().replace('\\', '/');
        } catch (Exception e) {
            Ui.error(parent, e.getMessage() == null ? e.toString() : e.getMessage());
            return null;
        }
    }
}
