package eventms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin: create / edit / duplicate / delete your own events. Student: browse events and register. */
final class EventsPage extends Page {
    private final boolean admin;
    private final DefaultTableModel m = Ui.model("ID", "Event", "Category", "Date", "Time", "Venue", "Host", "Capacity", "Registered", "Status");
    private final JTable t = Ui.table(m);
    private final Map<Integer, Object[]> events = new HashMap<>();

    EventsPage(boolean admin) {
        this.admin = admin;
        JPanel card;
        JTextField search = new Ui.SearchField(18, "Search...");
        if (admin) {
            Ui.RoundBtn add = Ui.button("+ Create event", Ui.SUCCESS), edit = Ui.button("Edit", Ui.PRIMARY),
                    dup = Ui.button("Duplicate", Ui.GRAY), del = Ui.button("Delete", Ui.DANGER);
            add.addActionListener(e -> edit(null));
            edit.addActionListener(e -> { int id = needRow(t, "event"); if (id >= 0) edit(id); });
            dup.addActionListener(e -> {
                int id = needRow(t, "event");
                if (id < 0) return;
                Object[] o = events.get(id);
                Dao.saveEvent(null, Dao.s(o[1]) + " (copy)", Dao.s(o[8]), Dao.s(o[2]), Dao.s(o[3]), Dao.s(o[4]), Dao.s(o[5]),
                        Dao.s(o[9]), Ui.num(o[6]));
                refresh();
            });
            del.addActionListener(e -> {
                int id = needRow(t, "event");
                if (id >= 0 && Ui.confirm(this, "Delete this event and all of its registrations?")) {
                    Dao.deleteEvent(id);
                    refresh();
                }
            });
            card = tableCard(t, search, add, edit, dup, del);
            add(header("Events", "Create your own events: title, category, date, time, venue, host and capacity"), BorderLayout.NORTH);
        } else {
            Ui.RoundBtn reg = Ui.button("Register for selected event", Ui.SUCCESS);
            reg.addActionListener(e -> {
                int id = needRow(t, "event");
                if (id < 0) return;
                String err = Dao.register(id, Dao.current.id());
                if (err != null) Ui.error(this, err);
                else Ui.info(this, "You're registered! Find your pass under 'My Registrations'.");
                refresh();
            });
            card = tableCard(t, search, reg);
            add(header("Browse Events", "Pick an event and register in one click"), BorderLayout.NORTH);
        }
        t.removeColumn(t.getColumnModel().getColumn(0));
        add(card, BorderLayout.CENTER);
    }

    @Override void refresh() {
        events.clear();
        List<Object[]> rows = new ArrayList<>();
        String today = LocalDate.now().toString();
        for (Object[] e : Dao.events()) {
            int id = Ui.num(e[0]), cap = Ui.num(e[6]), reg = Ui.num(e[7]);
            events.put(id, e);
            String status = Dao.s(e[3]).compareTo(today) < 0 ? "Closed" : reg >= cap ? "Full" : "Open";
            rows.add(new Object[]{id, Dao.s(e[1]), Dao.s(e[2]), Dao.s(e[3]), Dao.s(e[4]), Dao.s(e[5]), Dao.s(e[9]), cap, reg, status});
        }
        Ui.fill(m, rows);
    }

    private void edit(Integer id) {
        Object[] e = id == null ? null : events.get(id);
        JTextField title = Ui.field(24), venue = Ui.field(24), host = Ui.field(24);
        host.putClientProperty("JTextField.placeholderText", "e.g. Dr. A. Kumar / CS Department");
        JComboBox<String> cat = new JComboBox<>(new String[]{"Seminar", "Workshop", "Hackathon", "Cultural", "Technical", "Sports", "Other"});
        cat.setEditable(true);   // type your own category
        cat.setPreferredSize(new Dimension(220, 36));
        JSpinner date = Ui.dateSpinner(), time = Ui.timeSpinner();
        JSpinner cap = new JSpinner(new SpinnerNumberModel(100, 1, 100000, 10));
        cap.setPreferredSize(new Dimension(120, 36));
        JTextArea desc = new JTextArea();
        if (e != null) {
            title.setText(Dao.s(e[1]));
            cat.setSelectedItem(Dao.s(e[2]));
            Ui.setDate(date, Dao.s(e[3]));
            Ui.setTime(time, Dao.s(e[4]));
            venue.setText(Dao.s(e[5]));
            host.setText(Dao.s(e[9]));
            cap.setValue(Ui.num(e[6]));
            desc.setText(Dao.s(e[8]));
        }
        JPanel p = new JPanel(new GridBagLayout());
        Ui.row(p, 0, "Title", title);
        Ui.row(p, 1, "Category (pick one or type your own)", cat);
        Ui.row(p, 2, "Date and time", Ui.pair(date, time));
        Ui.row(p, 3, "Venue", venue);
        Ui.row(p, 4, "Host / organiser (printed on certificates)", host);
        Ui.row(p, 5, "Capacity", cap);
        Ui.row(p, 6, "Description", Ui.textArea(desc, 3));
        if (JOptionPane.showConfirmDialog(this, p, id == null ? "Create event" : "Edit event",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (title.getText().isBlank() || venue.getText().isBlank()) {
            Ui.error(this, "Title and venue are required.");
            return;
        }
        String category = String.valueOf(cat.getSelectedItem()).trim();
        Dao.saveEvent(id, title.getText().trim(), desc.getText().trim(), category.isEmpty() ? "Other" : category,
                Ui.dateOf(date), Ui.timeOf(time), venue.getText().trim(), host.getText().trim(), ((Number) cap.getValue()).intValue());
        refresh();
    }
}
