package eventms;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Chat with an AI assistant that can see the event data (through the API configured in Settings). */
final class AiPage extends Page {
    private final boolean admin;
    private final JTextPane chat = new JTextPane();
    private final JTextField input = new Ui.SearchField(40, "Ask something...");
    private final Ui.RoundBtn send = Ui.button("Send", Ui.PRIMARY);
    private final List<String[]> history = new ArrayList<>();

    AiPage(boolean admin) {
        this.admin = admin;
        add(header("AI Assistant", admin ? "Ask about attendance, trends and ideas for your events"
                : "Ask about events, registrations and your certificates"), BorderLayout.NORTH);

        chat.setEditable(false);
        chat.setOpaque(false);
        chat.setFont(Ui.font(Font.PLAIN, 14));
        chat.setForeground(Ui.INK);
        chat.setCaretColor(Ui.INK);
        chat.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        post("Assistant", "Hi " + Dao.current.name().split(" ")[0] + "! Pick a suggestion below or type your own question.", Ui.CYAN);

        String[] ideas = admin
                ? new String[]{"Summarize how our events performed", "Which event had the best attendance?", "How can we get more registrations?"}
                : new String[]{"Which events are coming up?", "How do I get my certificate?", "Which events still have seats?"};
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        for (String s : ideas) {
            Ui.RoundBtn b = Ui.button(s, new Color(0x6D28D9));
            b.addActionListener(e -> ask(s));
            chips.add(b);
        }

        input.addActionListener(e -> ask(input.getText()));
        send.addActionListener(e -> ask(input.getText()));
        JPanel in = new JPanel(new BorderLayout(8, 0));
        in.setOpaque(false);
        in.add(input, BorderLayout.CENTER);
        in.add(send, BorderLayout.EAST);

        Ui.Card card = new Ui.Card(new BorderLayout(0, 12)).glow(Ui.VIOLET);
        card.add(chips, BorderLayout.NORTH);
        JScrollPane sp = Ui.thin(new JScrollPane(chat));
        card.add(sp, BorderLayout.CENTER);
        card.add(in, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override void refresh() { }

    /** Appends a coloured speaker name and the message text to the chat. */
    private void post(String who, String text, Color c) {
        StyledDocument d = chat.getStyledDocument();
        SimpleAttributeSet a = new SimpleAttributeSet(), b = new SimpleAttributeSet();
        StyleConstants.setForeground(a, c);
        StyleConstants.setBold(a, true);
        StyleConstants.setFontSize(a, 13);
        StyleConstants.setForeground(b, Ui.INK);
        StyleConstants.setFontSize(b, 14);
        try {
            d.insertString(d.getLength(), who + "\n", a);
            d.insertString(d.getLength(), text + "\n\n", b);
        } catch (BadLocationException ignored) { }
        chat.setCaretPosition(d.getLength());
    }

    private void ask(String q) {
        q = q.trim();
        if (q.isEmpty() || !send.isEnabled()) return;
        input.setText("");
        post("You", q, new Color(0xF9A8D4));
        history.add(new String[]{"user", q});
        send.setEnabled(false);
        List<String[]> snapshot = new ArrayList<>(history);
        final int uid = Dao.current.id();
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                String system = "You are the assistant inside Smart Event Manager, a college event management app. "
                        + "Answer briefly and clearly using the data below. If the data does not contain the answer, say so instead of guessing. "
                        + (admin ? "You are talking to an administrator.\n\n" : "You are talking to a student.\n\n") + Dao.aiContext(admin, uid);
                return Ai.ask(system, snapshot);
            }

            @Override protected void done() {
                try {
                    String a = get();
                    history.add(new String[]{"assistant", a});
                    post("Assistant", a, Ui.CYAN);
                } catch (Exception e) {
                    history.remove(history.size() - 1); // drop the unanswered question so the next try is clean
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    post("Assistant", "(" + (c.getMessage() == null ? c.toString() : c.getMessage()) + ")", new Color(0xFCA5A5));
                }
                send.setEnabled(true);
                input.requestFocusInWindow();
            }
        }.execute();
    }
}
