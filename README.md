# Smart Event Manager

A desktop event management system written in Java. Swing user interface with animated 3D and glow effects, JDBC database (SQLite by default), admin and student logins, passes, certificates, email delivery, reports and an AI assistant.

## Features
- **Admin and student logins**, each with their own view
- **Events**: create, edit, duplicate and delete your own events (title, category, date, time, venue, host, capacity)
- **Participants** with phone numbers, added by the admin or registered by students
- **Registrations** with status tracking
- **Passes** and **certificates** generated as images, with a certificate designer (heading, college name, logo, signer names, titles and signature images)
- **Email**: send passes and certificates straight to a student's inbox (SMTP)
- **Reports**: animated 3D charts and an analysis dashboard
- **AI assistant**: works with an Anthropic key or an OpenRouter key (free models available)
- **Database**: JDBC with SQLite; can be pointed at MySQL or another database

## Requirements
- JDK 17 or newer
- These jars in a `lib` folder next to the source files:
  - `sqlite-jdbc` (3.46.1.3)
  - `jakarta.mail` (2.0.1)
  - `jakarta.activation` (2.0.1)
  - `slf4j-api`

## Build and run (Windows PowerShell)
```powershell
javac -cp "lib/*" -d out (Get-ChildItem *.java).Name
java -cp "out;lib/*" eventms.Main
```
On first start the app creates `eventms.db` with demo data.

## Default logins (change them before real use)
| Role    | Email                | Password   |
|---------|----------------------|------------|
| Admin   | admin@college.edu    | admin123   |
| Student | student@college.edu  | student123 |

New participants added by the admin get the password `student123`.

## Configuration
Settings are stored in `config.properties` (never commit this file). Copy `config.properties.example` to get started, or use **Admin > Settings** in the app.

- **Email**: SMTP host, port, username and password. For Gmail use `smtp.gmail.com`, port 587 and an App Password (needs 2-step verification).
- **AI**: paste an Anthropic key (`sk-ant-...`) or an OpenRouter key (`sk-or-...`). With OpenRouter the free model router is used by default.
- **Database**: set `db.url`, `db.user` and `db.password` to use MySQL or another JDBC database. Add its driver jar to `lib`. The table definitions in `Db.java` use SQLite syntax (change `AUTOINCREMENT` to `AUTO_INCREMENT` for MySQL).

## Project layout
| File | Purpose |
|------|---------|
| `Main.java`, `LoginFrame.java`, `AppFrame.java` | Start-up, login and main window |
| `Ui.java`, `Page.java`, `Charts.java`, `Art.java` | Theme, animated pages, charts, pass and certificate drawing |
| `*Page.java` | Dashboard, events, registrations, students, reports, AI and settings screens |
| `Db.java`, `Dao.java` | Database connection, tables and all queries |
| `Mailer.java`, `Ai.java`, `Config.java`, `CertDesigner.java` | Email, AI client, settings, certificate designer |
