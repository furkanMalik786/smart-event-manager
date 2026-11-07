package eventms;

import javax.swing.*;

/** Entry point: run this class. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.setup();
            try {
                Db.init();
            } catch (Exception e) {
                Ui.error(null, "Could not open the database:\n" + e.getMessage());
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
