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
  }
  authorize(user) {
    if (!this.env.ADMIN_GOOGLE_EMAIL || user.email?.toLowerCase() !== this.env.ADMIN_GOOGLE_EMAIL.toLowerCase()) return false;
    // Pin once to the subject from a verified Google identity. An email match alone
    // can never transfer an already established administrator role.
    this.sql.exec('INSERT OR IGNORE INTO administrator VALUES (1, ?)', user.id);
    return this.sql.exec('SELECT subject FROM administrator WHERE singleton=1').one().subject === user.id;
  }
  record(user, document) {
    this.ctx.storage.transactionSync(() => {
      const old = this.sql.exec('SELECT revision FROM accounts WHERE subject=?', user.id).toArray()[0];
      if (old && old.revision > document.revision) return;
      const now = Date.now();
      if (old && old.revision === document.revision) {
        this.sql.exec('UPDATE accounts SET email=?, confirmedAt=? WHERE subject=?', user.email, now, user.id);
        return;
      }
      const shots = document.backup.shots;
      this.sql.exec('INSERT OR REPLACE INTO accounts VALUES (?, ?, ?, ?, ?, ?)', user.id, user.email, document.revision, now, shots.length, shots.reduce((n, s) => n + s.rating, 0));
      this.sql.exec('DELETE FROM daily WHERE subject=?', user.id);
      this.sql.exec('DELETE FROM styles WHERE subject=?', user.id);
      const days = new Map(), styles = new Map();
      for (const shot of shots) {
        const day = new Date(shot.createdAt).toISOString().slice(0, 10);
        const style = shot.style?.trim() || 'Sin estilo';
        days.set(day, (days.get(day) || 0) + 1);
        styles.set(style, (styles.get(style) || 0) + 1);
      }
      for (const [day, count] of days) this.sql.exec('INSERT INTO daily VALUES (?, ?, ?)', user.id, day, count);
      for (const [style, count] of styles) this.sql.exec('INSERT INTO styles VALUES (?, ?, ?)', user.id, style, count);
    });
  }
  overview(after = '') {
    const totals = this.sql.exec('SELECT COUNT(*) AS accounts, COALESCE(SUM(shots),0) AS shots, COALESCE(SUM(ratingSum),0) AS ratingSum FROM accounts').one();
    const accounts = this.sql.exec('SELECT * FROM accounts WHERE subject > ? ORDER BY subject LIMIT 51', after).toArray();
    const cutoff = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10);
    const week = new Date(Date.now() - 6 * 86400000).toISOString().slice(0, 10);
    const shots7Days = this.sql.exec('SELECT COALESCE(SUM(count),0) AS count FROM daily WHERE day >= ?', week).one().count;
    return { ...totals, shots7Days, accountsPage: accounts.slice(0, 50), next: accounts.length > 50 ? accounts[49].subject : null,
      daily: this.sql.exec('SELECT day AS label, SUM(count) AS count FROM daily WHERE day >= ? GROUP BY day ORDER BY day DESC', cutoff).toArray(),
      styles: this.sql.exec('SELECT style AS label, SUM(count) AS count FROM styles GROUP BY style ORDER BY count DESC, style LIMIT 10').toArray() };
  }
  account(subject) { return this.sql.exec('SELECT * FROM accounts WHERE subject=?', subject).toArray()[0] || null; }
}
