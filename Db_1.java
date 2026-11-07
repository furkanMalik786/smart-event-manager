package eventms;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** JDBC helper: connection, schema creation, seed data, tiny query helpers. */
final class Db {
    private Db() { }

    static Connection conn() throws SQLException {
        String url = Config.get("db.url", "jdbc:sqlite:eventms.db");
        String u = Config.get("db.user"), p = Config.get("db.password");
        Connection c = u.isEmpty() ? DriverManager.getConnection(url) : DriverManager.getConnection(url, u, p);
        if (url.startsWith("jdbc:sqlite")) {
            try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys=ON"); }
        }
        return c;
    }

    static void init() throws SQLException {
        try (Connection c = conn(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, "
                    + "email TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, role TEXT NOT NULL, "
                    + "department TEXT, created_at TEXT, phone TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS events(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, "
                    + "description TEXT, category TEXT, event_date TEXT NOT NULL, event_time TEXT NOT NULL, "
                    + "venue TEXT NOT NULL, capacity INTEGER NOT NULL, created_at TEXT, host TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS registrations(id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "event_id INTEGER NOT NULL REFERENCES events(id) ON DELETE CASCADE, "
                    + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, reg_date TEXT, "
                    + "status TEXT NOT NULL DEFAULT 'REGISTERED', pass_code TEXT UNIQUE, "
                    + "pass_emailed INTEGER DEFAULT 0, UNIQUE(event_id,user_id))");
            s.execute("CREATE TABLE IF NOT EXISTS certificates(id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "registration_id INTEGER NOT NULL UNIQUE REFERENCES registrations(id) ON DELETE CASCADE, "
                    + "cert_code TEXT UNIQUE, issued_date TEXT, emailed INTEGER DEFAULT 0)");
        }
        // upgrade older databases created before phone / host existed (errors = column already there)
        for (String alter : new String[]{"ALTER TABLE users ADD COLUMN phone TEXT", "ALTER TABLE events ADD COLUMN host TEXT"}) {
            try (Connection c = conn(); Statement s = c.createStatement()) { s.execute(alter); } catch (SQLException ignored) { }
        }
        if (Dao.count("SELECT COUNT(*) FROM users") == 0) seed();
    }

    private static void seed() {
        update("INSERT INTO users(name,email,password_hash,role,department,created_at) VALUES(?,?,?,?,?,?)",
                "Administrator", "admin@college.edu", hash("admin123"), "ADMIN", "Administration", LocalDate.now().toString());
        String[][] st = {{"Aarav Sharma", "student@college.edu", "Computer Science"},
                {"Diya Patel", "diya@college.edu", "Electronics"},
                {"Rohan Mehta", "rohan@college.edu", "Mechanical"},
                {"Sneha Iyer", "sneha@college.edu", "Computer Science"}};
        for (String[] s : st)
            update("INSERT INTO users(name,email,password_hash,role,department,created_at) VALUES(?,?,?,?,?,?)",
                    s[0], s[1], hash("student123"), "STUDENT", s[2], LocalDate.now().toString());
        LocalDate d = LocalDate.now();
        Object[][] ev = {
                {"Code Sprint Hackathon", "24-hour hackathon to build campus solutions.", "Hackathon", d.plusDays(5), "09:00", "Main Auditorium", 120},
                {"AI & Future Careers Seminar", "Industry experts talk about careers in AI.", "Seminar", d.plusDays(12), "11:00", "Seminar Hall B", 80},
                {"Cultural Fest Night", "Music, dance and drama performances.", "Cultural", d.plusDays(20), "18:00", "Open Air Theatre", 300},
                {"Web Dev Workshop", "Hands-on workshop on modern web development.", "Workshop", d.minusDays(6), "10:00", "Computer Lab 3", 40},
                {"Inter-College Quiz", "General knowledge and tech quiz.", "Technical", d.minusDays(14), "14:00", "Room 204", 60}};
        for (Object[] e : ev)
            update("INSERT INTO events(title,description,category,event_date,event_time,venue,capacity,created_at) VALUES(?,?,?,?,?,?,?,?)",
                    e[0], e[1], e[2], e[3].toString(), e[4], e[5], e[6], d.toString());
        // demo registrations: students 2..5 -> users 2..5, events 1..5
        int[][] regs = {{1, 2}, {1, 3}, {2, 2}, {3, 4}, {4, 2}, {4, 3}, {4, 4}, {5, 2}, {5, 5}};
        for (int[] r : regs) {
            boolean past = r[0] >= 4;
            update("INSERT INTO registrations(event_id,user_id,reg_date,status,pass_code) VALUES(?,?,?,?,?)",
                    r[0], r[1], d.minusDays(20).toString(), past ? "ATTENDED" : "REGISTERED", "PASS-" + r[0] + "-" + Dao.rand(6));
        }
    }

    // ---------- password hashing (salted SHA-256) ----------
    static String hash(String pw) {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        String s = HexFormat.of().formatHex(salt);
        return s + "$" + sha(s + pw);
    }

    static boolean verify(String pw, String stored) {
        int i = stored.indexOf('$');
        return i > 0 && sha(stored.substring(0, i) + pw).equals(stored.substring(i + 1));
    }

    private static String sha(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8")));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ---------- query helpers ----------
    static List<Object[]> query(String sql, Object... p) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                List<Object[]> out = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) row[i] = rs.getObject(i + 1);
                    out.add(row);
                }
                return out;
            }
        } catch (SQLException e) { throw new RuntimeException(e.getMessage(), e); }
    }

    static int update(String sql, Object... p) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            return ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e.getMessage(), e); }
    }
}
