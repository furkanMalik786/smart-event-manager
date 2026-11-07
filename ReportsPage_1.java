package eventms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/** Admin analysis report: charts + per-event table, exportable to CSV. */
final class ReportsPage extends Page {
    private final DefaultTableModel m = Ui.model("Event", "Date", "Capacity", "Registered", "Attended", "Cancelled", "Fill %", "Attendance %");
    private final Charts.Bar perEvent = new Charts.Bar();
    private final Charts.Pie byDept = new Charts.Pie(), byStatus = new Charts.Pie();
    private final List<Object[]> current = new ArrayList<>();

    ReportsPage() {
        Ui.RoundBtn export = Ui.button("Export CSV", Ui.PRIMARY);
        export.addActionListener(e -> export());
        add(header("Analysis Report", "Registrations, attendance and department breakdown", export), BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 3, 14, 0));
        charts.setOpaque(false);
        charts.add(chart("Registrations per event", perEvent));
        charts.add(chart("Registrations by department", byDept));
        charts.add(chart("Registration status", byStatus));
        charts.setPreferredSize(new Dimension(0, 300));

        Ui.Card tbl = new Ui.Card(new BorderLayout(0, 10));
        tbl.add(Ui.label("Event performance", Font.BOLD, 15, Ui.INK), BorderLayout.NORTH);
        tbl.add(Ui.scroll(Ui.table(m)), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(charts, BorderLayout.NORTH);
        body.add(tbl, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private Ui.Card chart(String title, JComponent c) {
        Ui.Card card = new Ui.Card(new BorderLayout(0, 8));
        card.add(Ui.label(title, Font.BOLD, 14, Ui.INK), BorderLayout.NORTH);
        card.add(c, BorderLayout.CENTER);
        return card;
    }

    @Override void refresh() {
        current.clear();
        Map<String, Number> perEv = new LinkedHashMap<>();
        for (Object[] r : Dao.reportRows()) {
            int cap = Ui.num(r[2]), reg = Ui.num(r[3]), att = Ui.num(r[4]), can = Ui.num(r[5]);
            int fill = cap == 0 ? 0 : Math.round(100f * reg / cap), attPct = reg == 0 ? 0 : Math.round(100f * att / reg);
            current.add(new Object[]{Dao.s(r[0]), Dao.s(r[1]), cap, reg, att, can, fill + "%", attPct + "%"});
            perEv.put(Dao.s(r[0]), reg);
        }
        Ui.fill(m, current);
        perEvent.setData(perEv);
        byDept.setData(Dao.deptCounts());
        byStatus.setData(Dao.statusCounts());
    }

    private void export() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("event-report.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < m.getColumnCount(); c++) sb.append(c > 0 ? "," : "").append(csv(m.getColumnName(c)));
        sb.append('\n');
        for (Object[] r : current) {
            for (int c = 0; c < r.length; c++) sb.append(c > 0 ? "," : "").append(csv(String.valueOf(r[c])));
            sb.append('\n');
        }
        try {
            Files.writeString(fc.getSelectedFile().toPath(), sb.toString());
            Ui.info(this, "Report saved to " + fc.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private static String csv(String s) { return '"' + s.replace("\"", "\"\"") + '"'; }
}
