import { createRemoteJWKSet, jwtVerify } from 'jose';

export const TTL = 5 * 60_000;
export const validProof = value => typeof value === 'string' && /^[A-Za-z0-9_-]{43}$/.test(value);
export const random = () => b64(crypto.getRandomValues(new Uint8Array(32)));
const b64 = bytes => btoa(String.fromCharCode(...bytes)).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
export const hash = async value => b64(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))));
export class AuthError extends Error {}
const googleKeys = createRemoteJWKSet(new URL('https://www.googleapis.com/oauth2/v3/certs'));
export async function verifyGoogle(token, env, nonce, keys = googleKeys) {
  const { payload } = await jwtVerify(token, keys, {
    issuer: ['https://accounts.google.com', 'accounts.google.com'], audience: env.GOOGLE_CLIENT_ID,
    algorithms: ['RS256'], requiredClaims: ['exp', 'iat', 'sub', 'nonce'], maxTokenAge: '10m', clockTolerance: 10,
  });
  if (payload.nonce !== nonce || !payload.sub || payload.email_verified !== true || typeof payload.email !== 'string') throw new AuthError('identity');
  const allowed = env.ALLOWED_EMAILS.split(',').map(s => s.trim().toLowerCase());
  if (!allowed.includes(payload.email.toLowerCase())) throw new AuthError('not_allowed');
  return { id: payload.sub, email: payload.email, displayName: typeof payload.name === 'string' ? payload.name : null };
}
export async function googleExchange(code, record, env) {
  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ code, client_id: env.GOOGLE_CLIENT_ID, client_secret: env.GOOGLE_CLIENT_SECRET,
      redirect_uri: `${env.PUBLIC_ORIGIN}/google/callback`, grant_type: 'authorization_code', code_verifier: record.googleVerifier }),
    signal: AbortSignal.timeout(15_000),
  });
  if (!response.ok) throw new AuthError('provider');
  const body = await response.json();
  if (typeof body.id_token !== 'string') throw new AuthError('provider');
  return verifyGoogle(body.id_token, env, record.nonce);
}

// Each instance owns one short-lived attempt. Storage transactions serialize consume/replay checks.
export class Session {
  constructor(storage, env, exchange = googleExchange, now = () => Date.now()) {
    this.storage = storage; this.env = env; this.exchange = exchange; this.now = now;
  }
  async init(input) {
    if (!validProof(input.challenge) || !validProof(input.appState) || !validProof(input.browserHash)) throw new AuthError('invalid');
    const record = { ...input, expires: this.now() + TTL, status: 'pending', nonce: random(), googleVerifier: random() };
    await this.storage.transaction(async tx => {
      if (await tx.get('session')) throw new AuthError('duplicate');
      await tx.put('session', record);
      await tx.setAlarm(record.expires);
    });
    return { nonce: record.nonce, googleChallenge: await hash(record.googleVerifier) };
  }
  async callback(input) {
    const record = await this.storage.transaction(async tx => {
      const r = await tx.get('session');
      if (!r || r.expires < this.now() || r.status !== 'pending' || r.browserHash !== input.browserHash) throw new AuthError('invalid');
      await tx.put('session', { ...r, status: 'verifying' });
      return r;
    });
    if (input.error) { await this.storage.deleteAll(); return { appState: record.appState, error: 'cancelled' }; }
    try {
      if (typeof input.code !== 'string' || input.code.length > 4096) throw new AuthError('code');
      const user = await this.exchange(input.code, record, this.env);
      const ticket = random();
      await this.storage.put('session', { appState: record.appState, challenge: record.challenge,
        status: 'ready', ticketHash: await hash(ticket), expires: Math.min(record.expires, this.now() + 60_000), user });
      return { appState: record.appState, ticket };
    } catch { await this.storage.deleteAll(); return { appState: record.appState, error: 'failed' }; }
  }
  async consume(input) {
    if (!validProof(input.verifier) || !validProof(input.ticket) || !validProof(input.appState)) throw new AuthError('invalid');
    const challenge = await hash(input.verifier), ticketHash = await hash(input.ticket);
    return this.storage.transaction(async tx => {
      const r = await tx.get('session');
      if (!r || r.status !== 'ready' || r.expires < this.now() || r.challenge !== challenge ||
          r.ticketHash !== ticketHash || r.appState !== input.appState) throw new AuthError('invalid');
      await tx.delete('session');
      await tx.deleteAlarm();
      return r.user;
    });
  }
}
