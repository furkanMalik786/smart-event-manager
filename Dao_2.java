package eventms;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

/** All database operations used by the screens. */
final class Dao {
    private Dao() { }

    static User current;
    private static final SecureRandom RND = new SecureRandom();

    static String rand(int n) {
        String a = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(a.charAt(RND.nextInt(a.length())));
        return sb.toString();
    }

    static String s(Object o) { return o == null ? "" : o.toString(); }

    static int count(String sql, Object... p) {
        List<Object[]> r = Db.query(sql, p);
        return r.isEmpty() ? 0 : Ui.num(r.get(0)[0]);
    }

    // ---------- users ----------
    static User login(String email, String pw, String role) {
        List<Object[]> r = Db.query("SELECT id,name,email,password_hash,role,department FROM users WHERE lower(email)=lower(?) AND role=?", email, role);
        if (r.isEmpty()) return null;
        Object[] u = r.get(0);
        if (!Db.verify(pw, s(u[3]))) return null;
        return new User(Ui.num(u[0]), s(u[1]), s(u[2]), s(u[4]), s(u[5]));
    }

    /** @return null on success, otherwise an error message */
    static String addUser(String name, String email, String pw, String role, String dept) {
        return addUser(name, email, pw, role, dept, "");
    }

    /** true if the phone number is empty or looks valid (7-18 digits, optional + ( ) - and spaces) */
    static boolean validPhone(String p) { return p == null || p.isBlank() || p.trim().matches("\\+?[0-9 ()-]{7,18}"); }

    static void setPhone(int userId, String phone) { Db.update("UPDATE users SET phone=? WHERE id=?", phone, userId); }

    static String addUser(String name, String email, String pw, String role, String dept, String phone) {
        if (count("SELECT COUNT(*) FROM users WHERE lower(email)=lower(?)", email) > 0) return "That email is already registered.";
        Db.update("INSERT INTO users(name,email,password_hash,role,department,created_at,phone) VALUES(?,?,?,?,?,?,?)",
                name, email, Db.hash(pw), role, dept, LocalDate.now().toString(), phone == null ? "" : phone.trim());
        return null;
    }

    static List<Object[]> students() {
        return Db.query("SELECT id,name,email,department,created_at,phone FROM users WHERE role='STUDENT' ORDER BY name");
    }

    static void deleteUser(int id) { Db.update("DELETE FROM users WHERE id=? AND role='STUDENT'", id); }

    // ---------- events ----------
    static void saveEvent(Integer id, String title, String desc, String cat, String date, String time, String venue, String host, int cap) {
        if (id == null)
            Db.update("INSERT INTO events(title,description,category,event_date,event_time,venue,capacity,created_at,host) VALUES(?,?,?,?,?,?,?,?,?)",
                    title, desc, cat, date, time, venue, cap, LocalDate.now().toString(), host);
        else
            Db.update("UPDATE events SET title=?,description=?,category=?,event_date=?,event_time=?,venue=?,capacity=?,host=? WHERE id=?",
                    title, desc, cat, date, time, venue, cap, host, id);
    }

    static void deleteEvent(int id) { Db.update("DELETE FROM events WHERE id=?", id); }

    /** id,title,category,date,time,venue,capacity,registered,description,host */
    static List<Object[]> events() {
        return Db.query("SELECT e.id,e.title,e.category,e.event_date,e.event_time,e.venue,e.capacity,"
                + "(SELECT COUNT(*) FROM registrations r WHERE r.event_id=e.id AND r.status<>'CANCELLED'),e.description,e.host "
                + "FROM events e ORDER BY e.event_date,e.event_time");
    }

    static List<Object[]> upcoming(int limit) {
        return Db.query("SELECT e.title,e.event_date,e.event_time,e.venue,"
                + "(SELECT COUNT(*) FROM registrations r WHERE r.event_id=e.id AND r.status<>'CANCELLED')||' / '||e.capacity "
                + "FROM events e WHERE e.event_date>=? ORDER BY e.event_date,e.event_time LIMIT " + limit, LocalDate.now().toString());
    }

    // ---------- registrations ----------
    /** @return null on success, otherwise an error message */
    static String register(int eventId, int userId) {
        List<Object[]> ev = Db.query("SELECT e.capacity,(SELECT COUNT(*) FROM registrations r WHERE r.event_id=e.id AND r.status<>'CANCELLED'),e.event_date FROM events e WHERE e.id=?", eventId);
        if (ev.isEmpty()) return "Event not found.";
        int cap = Ui.num(ev.get(0)[0]), reg = Ui.num(ev.get(0)[1]);
        if (s(ev.get(0)[2]).compareTo(LocalDate.now().toString()) < 0) return "Registration is closed - this event has already happened.";
        List<Object[]> ex = Db.query("SELECT id,status FROM registrations WHERE event_id=? AND user_id=?", eventId, userId);
        if (!ex.isEmpty() && !"CANCELLED".equals(s(ex.get(0)[1]))) return "Already registered for this event.";
        if (reg >= cap) return "Sorry, this event is full.";
        if (ex.isEmpty())
            Db.update("INSERT INTO registrations(event_id,user_id,reg_date,status,pass_code) VALUES(?,?,?,'REGISTERED',?)",
                    eventId, userId, LocalDate.now().toString(), "PASS-" + eventId + "-" + rand(6));
        else
            Db.update("UPDATE registrations SET status='REGISTERED',reg_date=? WHERE id=?", LocalDate.now().toString(), ex.get(0)[0]);
        return null;
    }

    static final int R_ID = 0, R_NAME = 1, R_EMAIL = 2, R_DEPT = 3, R_TITLE = 4, R_DATE = 5, R_TIME = 6, R_VENUE = 7,
            R_STATUS = 8, R_REGDATE = 9, R_PASS = 10, R_PASSMAIL = 11, R_CERT = 12, R_CERTMAIL = 13, R_PHONE = 14, R_HOST = 15;

    /** Registration rows; columns are the R_* constants. {@code where} is appended after WHERE. */
    static List<Object[]> regRows(String where, Object... p) {
        return Db.query("SELECT r.id,u.name,u.email,u.department,e.title,e.event_date,e.event_time,e.venue,r.status,r.reg_date,"
                + "r.pass_code,r.pass_emailed,c.cert_code,c.emailed,u.phone,e.host FROM registrations r "
                + "JOIN users u ON u.id=r.user_id JOIN events e ON e.id=r.event_id "
                + "LEFT JOIN certificates c ON c.registration_id=r.id WHERE " + where + " ORDER BY e.event_date DESC,u.name", p);
    }

    static void setStatus(int regId, String status) { Db.update("UPDATE registrations SET status=? WHERE id=?", status, regId); }
    static void deleteReg(int regId) { Db.update("DELETE FROM registrations WHERE id=?", regId); }
    static void markPassEmailed(int regId) { Db.update("UPDATE registrations SET pass_emailed=1 WHERE id=?", regId); }
    static void markCertEmailed(int regId) { Db.update("UPDATE certificates SET emailed=1 WHERE registration_id=?", regId); }

    /** Creates the certificate record if needed and returns its code. */
    static String ensureCert(int regId) {
        List<Object[]> r = Db.query("SELECT cert_code FROM certificates WHERE registration_id=?", regId);
        if (!r.isEmpty()) return s(r.get(0)[0]);
        String code = "CERT-" + regId + "-" + rand(6);
        Db.update("INSERT INTO certificates(registration_id,cert_code,issued_date,emailed) VALUES(?,?,?,0)", regId, code, LocalDate.now().toString());
        return code;
    }

    // ---------- analytics ----------
    /** events, students, active registrations, attended, certificates */
    static int[] stats() {
        return new int[]{
                count("SELECT COUNT(*) FROM events"),
                count("SELECT COUNT(*) FROM users WHERE role='STUDENT'"),
                count("SELECT COUNT(*) FROM registrations WHERE status<>'CANCELLED'"),
                count("SELECT COUNT(*) FROM registrations WHERE status='ATTENDED'"),
                count("SELECT COUNT(*) FROM certificates")};
    }

    /** title,date,capacity,registered,attended,cancelled */
    static List<Object[]> reportRows() {
        return Db.query("SELECT e.title,e.event_date,e.capacity,"
                + "SUM(CASE WHEN r.status<>'CANCELLED' THEN 1 ELSE 0 END),"
                + "SUM(CASE WHEN r.status='ATTENDED' THEN 1 ELSE 0 END),"
                + "SUM(CASE WHEN r.status='CANCELLED' THEN 1 ELSE 0 END) "
                + "FROM events e LEFT JOIN registrations r ON r.event_id=e.id GROUP BY e.id ORDER BY e.event_date");
    }

    static Map<String, Number> deptCounts() {
        Map<String, Number> m = new LinkedHashMap<>();
        for (Object[] r : Db.query("SELECT COALESCE(u.department,'Other'),COUNT(*) FROM registrations r JOIN users u ON u.id=r.user_id "
                + "WHERE r.status<>'CANCELLED' GROUP BY 1 ORDER BY 2 DESC"))
            m.put(s(r[0]), Ui.num(r[1]));
        return m;
    }

    static Map<String, Number> statusCounts() {
        Map<String, Number> m = new LinkedHashMap<>();
        for (Object[] r : Db.query("SELECT status,COUNT(*) FROM registrations GROUP BY status ORDER BY 1"))
            m.put(s(r[0]), Ui.num(r[1]));
        return m;
    }

    /** Plain-text snapshot of the data, used as context for the AI module. */
    static String aiContext(boolean admin, int uid) {
        StringBuilder sb = new StringBuilder("Today is " + LocalDate.now() + ".\nEVENTS (title | category | date time | venue | registered/capacity):\n");
        for (Object[] e : events())
            sb.append("- ").append(e[1]).append(" | ").append(e[2]).append(" | ").append(e[3]).append(' ').append(e[4])
                    .append(" | ").append(e[5]).append(" | ").append(e[7]).append('/').append(e[6]).append('\n');
        if (admin) {
            int[] st = stats();
            sb.append("\nTOTALS: events=").append(st[0]).append(", students=").append(st[1]).append(", active registrations=").append(st[2])
                    .append(", attended=").append(st[3]).append(", certificates issued=").append(st[4]).append('\n');
            sb.append("ATTENDANCE PER EVENT (title: registered, attended, cancelled):\n");
            for (Object[] r : reportRows())
                sb.append("- ").append(r[0]).append(": ").append(Ui.num(r[3])).append(", ").append(Ui.num(r[4])).append(", ").append(Ui.num(r[5])).append('\n');
            sb.append("REGISTRATIONS BY DEPARTMENT: ").append(deptCounts()).append('\n');
        } else {
            sb.append("\nTHIS STUDENT'S REGISTRATIONS:\n");
            for (Object[] r : regRows("r.user_id=?", uid))
                sb.append("- ").append(r[4]).append(" on ").append(r[5]).append(" status=").append(r[8])
                        .append(r[12] != null ? " (certificate issued)" : "").append('\n');
            sb.append("\nHOW THE SYSTEM WORKS: students register from 'Browse Events'; passes are in 'My Registrations' and can be emailed; "
                    + "certificates are issued by the admin after attendance is marked and can then be viewed or emailed from 'My Registrations'.\n");
        }
        return sb.toString();
    }
}
