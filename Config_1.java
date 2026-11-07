package eventms;

import java.io.*;
import java.util.Properties;

/** Reads / writes config.properties (SMTP, AI key, DB url). */
final class Config {
    private static final File FILE = new File("config.properties");
    private static final Properties P = new Properties();

    static {
        try (InputStream in = new FileInputStream(FILE)) {
            P.load(in);
        } catch (IOException ignored) { }
    }

    static boolean has(String k) { return P.containsKey(k); }
    static String get(String k, String d) { return P.getProperty(k, d).trim(); }
    static String get(String k) { return get(k, ""); }
    static int getInt(String k, int d) {
        try { return Integer.parseInt(get(k)); } catch (Exception e) { return d; }
    }
    static void set(String k, String v) { P.setProperty(k, v == null ? "" : v.trim()); }
    static void save() throws IOException {
        try (OutputStream o = new FileOutputStream(FILE)) { P.store(o, "Smart Event Manager settings"); }
    }
}
