package eventms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Home screen: headline numbers, the next events and a chart. */
final class DashboardPage extends Page {
    private final boolean admin;
    private final Ui.StatCard[] cards;
    private final DefaultTableModel model = Ui.model("Event", "Date", "Time", "Venue", "Registered");
    private final Charts.Bar bar = new Charts.Bar();
    private final Charts.Pie pie = new Charts.Pie();

    DashboardPage(boolean admin) {
        this.admin = admin;
        String[] titles = admin
                ? new String[]{"Events", "Students", "Registrations", "Attended", "Certificates"}
                : new String[]{"My registrations", "Attended", "Certificates", "Upcoming events"};
        cards = new Ui.StatCard[titles.length];
        JPanel grid = new JPanel(new GridLayout(1, titles.length, 6, 0));
        grid.setOpaque(false);
        for (int i = 0; i < titles.length; i++) {
            cards[i] = new Ui.StatCard(titles[i], Ui.PALETTE[i % Ui.PALETTE.length]);
            grid.add(cards[i]);
        }
        String first = Dao.current.name().split(" ")[0];
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(header("Welcome back, " + first, admin ? "Overview of all events and registrations" : "Your events at a glance"), BorderLayout.NORTH);
        top.add(grid, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        Ui.Card events = new Ui.Card(new BorderLayout(0, 10)).glow(Ui.CYAN);
        events.add(Ui.label("Upcoming events", Font.BOLD, 16, Ui.INK), BorderLayout.NORTH);
        events.add(Ui.scroll(Ui.table(model)), BorderLayout.CENTER);

        Ui.Card chart = new Ui.Card(new BorderLayout(0, 10)).glow(Ui.PINK);
        chart.add(Ui.label(admin ? "Registrations per event" : "My registration status", Font.BOLD, 16, Ui.INK), BorderLayout.NORTH);
        chart.add(admin ? bar : pie, BorderLayout.CENTER);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.gridy = 0;
        g.gridx = 0;
        g.weightx = 0.6;
        row.add(events, g);
        g.gridx = 1;
        g.weightx = 0.4;
        row.add(chart, g);
        add(row, BorderLayout.CENTER);
    }

    @Override void refresh() {
        if (admin) {
            int[] s = Dao.stats();
            for (int i = 0; i < 5; i++) cards[i].setValue(String.valueOf(s[i]));
            Map<String, Number> perEvent = new LinkedHashMap<>();
            for (Object[] r : Dao.reportRows()) perEvent.put(Dao.s(r[0]), Ui.num(r[3]));
            bar.setData(perEvent);
        } else {
            List<Object[]> all = Dao.regRows("r.user_id=?", Dao.current.id());
            int active = 0, attended = 0, certs = 0;
            Map<String, Number> status = new LinkedHashMap<>();
            for (Object[] r : all) {
                String st = Dao.s(r[Dao.R_STATUS]);
                status.merge(st, 1, (a, b) -> a.intValue() + b.intValue());
                if (!"CANCELLED".equals(st)) active++;
                if ("ATTENDED".equals(st)) attended++;
                if (r[Dao.R_CERT] != null) certs++;
            }
            cards[0].setValue(String.valueOf(active));
            cards[1].setValue(String.valueOf(attended));
            cards[2].setValue(String.valueOf(certs));
            cards[3].setValue(String.valueOf(Dao.upcoming(1000).size()));
            pie.setData(status);
        }
        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : Dao.upcoming(10))
            rows.add(new Object[]{Dao.s(r[0]), Dao.s(r[1]), Dao.s(r[2]), Dao.s(r[3]), Dao.s(r[4])});
        Ui.fill(model, rows);
    }
}
