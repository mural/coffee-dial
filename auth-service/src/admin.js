import { DurableObject } from 'cloudflare:workers';

// Internal binding only. No route accepts a client-supplied identity for authorization.
export class AdminDirectory extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.sql = ctx.storage.sql;
    this.sql.exec(`CREATE TABLE IF NOT EXISTS administrator (singleton INTEGER PRIMARY KEY CHECK(singleton=1), subject TEXT NOT NULL);
      CREATE TABLE IF NOT EXISTS accounts (subject TEXT PRIMARY KEY, email TEXT NOT NULL, revision INTEGER NOT NULL, confirmedAt INTEGER NOT NULL, shots INTEGER NOT NULL, ratingSum INTEGER NOT NULL);
      CREATE TABLE IF NOT EXISTS daily (subject TEXT NOT NULL, day TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(subject,day));
      CREATE TABLE IF NOT EXISTS styles (subject TEXT NOT NULL, style TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(subject,style));`);
    // Keep legacy indexes intact for identity lookup and recovery. New summaries
    // are keyed by verified email, just like the authoritative sync namespace.
    this.sql.exec(`
      CREATE TABLE IF NOT EXISTS email_accounts (subject TEXT PRIMARY KEY, email TEXT NOT NULL UNIQUE, revision INTEGER NOT NULL, confirmedAt INTEGER NOT NULL, shots INTEGER NOT NULL, ratingSum INTEGER NOT NULL);
      CREATE TABLE IF NOT EXISTS email_daily (subject TEXT NOT NULL, day TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(subject,day));
      CREATE TABLE IF NOT EXISTS email_styles (subject TEXT NOT NULL, style TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(subject,style));
      CREATE TABLE IF NOT EXISTS identities (provider TEXT NOT NULL, subject TEXT NOT NULL, email TEXT NOT NULL, PRIMARY KEY(provider,subject));
      CREATE VIEW IF NOT EXISTS legacy_latest AS
        SELECT * FROM (SELECT accounts.*, ROW_NUMBER() OVER (
          PARTITION BY LOWER(TRIM(email)) ORDER BY confirmedAt DESC, revision DESC, subject
        ) AS position FROM accounts) WHERE position=1;
      CREATE VIEW IF NOT EXISTS visible_accounts AS
        SELECT * FROM email_accounts UNION ALL
        SELECT 'email:' || LOWER(TRIM(email)), LOWER(TRIM(email)), revision, confirmedAt, shots, ratingSum
        FROM legacy_latest l WHERE NOT EXISTS (SELECT 1 FROM email_accounts e WHERE e.email=LOWER(TRIM(l.email)));
      CREATE VIEW IF NOT EXISTS visible_daily AS
        SELECT * FROM email_daily UNION ALL
        SELECT 'email:' || LOWER(TRIM(l.email)), d.day, d.count FROM daily d
        JOIN legacy_latest l ON l.subject=d.subject
        WHERE NOT EXISTS (SELECT 1 FROM email_accounts e WHERE e.email=LOWER(TRIM(l.email)));
      CREATE VIEW IF NOT EXISTS visible_styles AS
        SELECT * FROM email_styles UNION ALL
        SELECT 'email:' || LOWER(TRIM(l.email)), d.style, d.count FROM styles d
        JOIN legacy_latest l ON l.subject=d.subject
        WHERE NOT EXISTS (SELECT 1 FROM email_accounts e WHERE e.email=LOWER(TRIM(l.email)));`);
  }
  authorize(user) {
    if (!this.env.ADMIN_GOOGLE_EMAIL || user.email?.toLowerCase() !== this.env.ADMIN_GOOGLE_EMAIL.toLowerCase()) return false;
    // Pin once to the subject from a verified Google identity. An email match alone
    // can never transfer an already established administrator role.
    this.sql.exec('INSERT OR IGNORE INTO administrator VALUES (1, ?)', user.id);
    return this.sql.exec('SELECT subject FROM administrator WHERE singleton=1').one().subject === user.id;
  }
  record(user, document) {
    const email = user.email.trim().toLowerCase();
    const account = `email:${email}`;
    this.ctx.storage.transactionSync(() => {
      this.sql.exec('INSERT OR REPLACE INTO identities VALUES (?, ?, ?)', user.provider || 'google', user.id, email);
      const old = this.sql.exec('SELECT revision FROM email_accounts WHERE subject=?', account).toArray()[0];
      if (old && old.revision > document.revision) return;
      const now = Date.now();
      if (old && old.revision === document.revision) {
        this.sql.exec('UPDATE email_accounts SET email=?, confirmedAt=? WHERE subject=?', email, now, account);
        return;
      }
      const shots = document.backup.shots;
      this.sql.exec('INSERT OR REPLACE INTO email_accounts VALUES (?, ?, ?, ?, ?, ?)', account, email, document.revision, now, shots.length, shots.reduce((n, s) => n + s.rating, 0));
      this.sql.exec('DELETE FROM email_daily WHERE subject=?', account);
      this.sql.exec('DELETE FROM email_styles WHERE subject=?', account);
      const days = new Map(), styles = new Map();
      for (const shot of shots) {
        const day = new Date(shot.createdAt).toISOString().slice(0, 10);
        const style = shot.style?.trim() || 'Sin estilo';
        days.set(day, (days.get(day) || 0) + 1);
        styles.set(style, (styles.get(style) || 0) + 1);
      }
      for (const [day, count] of days) this.sql.exec('INSERT INTO email_daily VALUES (?, ?, ?)', account, day, count);
      for (const [style, count] of styles) this.sql.exec('INSERT INTO email_styles VALUES (?, ?, ?)', account, style, count);
    });
  }
  overview(after = '') {
    const totals = this.sql.exec('SELECT COUNT(*) AS accounts, COALESCE(SUM(shots),0) AS shots, COALESCE(SUM(ratingSum),0) AS ratingSum FROM visible_accounts').one();
    const accounts = this.sql.exec('SELECT * FROM visible_accounts WHERE subject > ? ORDER BY subject LIMIT 51', after).toArray();
    const cutoff = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10);
    const week = new Date(Date.now() - 6 * 86400000).toISOString().slice(0, 10);
    const shots7Days = this.sql.exec('SELECT COALESCE(SUM(count),0) AS count FROM visible_daily WHERE day >= ?', week).one().count;
    return { ...totals, shots7Days, accountsPage: accounts.slice(0, 50), next: accounts.length > 50 ? accounts[49].subject : null,
      daily: this.sql.exec('SELECT day AS label, SUM(count) AS count FROM visible_daily WHERE day >= ? GROUP BY day ORDER BY day DESC', cutoff).toArray(),
      styles: this.sql.exec('SELECT style AS label, SUM(count) AS count FROM visible_styles GROUP BY style ORDER BY count DESC, style LIMIT 10').toArray() };
  }
  account(subject) {
    const direct = this.sql.exec('SELECT * FROM visible_accounts WHERE subject=?', subject).toArray()[0];
    if (direct) return direct;
    const aliases = this.sql.exec(`SELECT LOWER(TRIM(email)) AS email FROM accounts WHERE subject=?
      UNION SELECT email FROM identities WHERE subject=?`, subject, subject).toArray();
    if (aliases.length !== 1) return null;
    return this.sql.exec('SELECT * FROM visible_accounts WHERE email=?', aliases[0].email).toArray()[0] || null;
  }
  subjectsByEmail(email) {
    if (!email) return [];
    return this.sql.exec(`SELECT subject, revision, shots FROM accounts WHERE LOWER(TRIM(email))=?
      UNION SELECT subject, 0 AS revision, 0 AS shots FROM identities WHERE email=?
      ORDER BY shots DESC, revision DESC`, email.trim().toLowerCase(), email.trim().toLowerCase()).toArray();
  }
}
