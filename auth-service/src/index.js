export { AdminDirectory } from './admin.js';
import { ServiceError, serviceError } from './errors.js';
import { createAccess, readAccess } from './access.js';
import { emptyBackup, emptySync, updateSync, seedLegacy } from './sync.js';
import { loginDestination, callbackDestination } from './returns.js';
import { DurableObject } from 'cloudflare:workers';
import { Session, random, hash, validProof, verifySyncToken } from './core.js';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization'
};

const headers = {
  ...corsHeaders,
  'Cache-Control': 'no-store',
  'Referrer-Policy': 'no-referrer',
  'X-Content-Type-Options': 'nosniff',
  'Content-Security-Policy': "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"
};

const json = (value, status = 200) => new Response(JSON.stringify(value), { status, headers: { ...headers, 'Content-Type': 'application/json' } });
const html = (body, status = 200, extra = {}) => new Response(`<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Coffee Dial</title><body><h1>Coffee Dial</h1>${body}</body></html>`, { status, headers: { ...headers, 'Content-Type': 'text/html; charset=utf-8', ...extra } });
const cookieName = state => `__Host-coffee-${state}`;
const cookie = (state, value, maxAge) => `${cookieName(state)}=${value}; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=${maxAge}`;

async function boundedJson(request, maxBytes = 100_000) {
  const length = Number.parseInt(request.headers.get('Content-Length') || '0', 10);
  if (Number.isFinite(length) && length > maxBytes) throw new Error('payload_too_large');
  const reader = request.body?.getReader();
  if (!reader) throw new Error('invalid');
  let bytes = 0; const chunks = [];
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    bytes += value.byteLength;
    if (bytes > maxBytes) throw new Error('payload_too_large');
    chunks.push(value);
  }
  const merged = new Uint8Array(bytes);
  let offset = 0;
  for (const chunk of chunks) { merged.set(chunk, offset); offset += chunk.byteLength; }
  return JSON.parse(new TextDecoder().decode(merged));
}

async function call(env, id, method, body) {
  const ns = env.SESSIONS.idFromName(id);
  const response = await env.SESSIONS.get(ns).fetch('https://internal/do', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ method, body }),
  });
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new ServiceError(data.error || 'service_unavailable', response.status);
  }
  return response.json();
}

async function recordAdmin(env, user, document) {
  if (!env.ADMIN_DIRECTORY) return;
  try { await env.ADMIN_DIRECTORY.getByName('directory-v1').record(user, document); }
  catch { console.warn(JSON.stringify({ event: 'admin_index_delayed' })); }
}

export class LoginAttempt extends DurableObject {
  async fetch(request) {
    try {
    const url = new URL(request.url);
    const input = await request.json();
    const method = input.method || url.pathname.slice(1);
    const body = input.body || input;
    if (method === 'init') return json(await new Session(this.ctx.storage, this.env).init(body));
    if (method === 'callback') return json(await new Session(this.ctx.storage, this.env).callback(body));
    if (method === 'consume') return json(await new Session(this.ctx.storage, this.env).consume(body));
    if (method === 'access_create') { await createAccess(this.ctx.storage, body.user); return json({ ok: true }); }
    if (method === 'access_read') return json(await readAccess(this.ctx.storage));
    if (method === 'access_revoke') { await this.ctx.storage.deleteAll(); return json({ ok: true }); }
    if (method === 'sync_read') return json(await this.ctx.storage.get('sync_v2') || emptySync());
    if (method === 'sync_write') return json(await updateSync(this.ctx.storage, body));
    if (method === 'sync_seed') return json(await seedLegacy(this.ctx.storage, body.backup));
    if (method === 'legacy_read') return json({ data: await this.ctx.storage.get('user_sync_data') || await this.ctx.storage.get('backup_v1') || null });
    return json({ error: 'invalid_method' }, 400);
    } catch (error) {
      const failure = serviceError(error);
      return json({ error: failure.message }, failure.status);
    }
  }
  async alarm() {
    await this.ctx.storage.transaction(async tx => {
      if (await tx.get('session')) await tx.delete('session');
      const access = await tx.get('access');
      if (access && access.expires <= Date.now()) await tx.delete('access');
      else if (access) await tx.setAlarm(access.expires);
    });
  }
}

export default {
  async fetch(request, env) {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers });
    }

    const url = new URL(request.url);
    if (url.origin !== env.PUBLIC_ORIGIN) return json({ error: 'wrong_origin' }, 400);
    if (url.pathname === '/health' && request.method === 'GET') return json({ status: env.GOOGLE_CLIENT_SECRET ? 'ready' : 'not_configured' });
    if (url.pathname === '/' && request.method === 'GET') return html('<p>Servicio de acceso de Coffee Dial. Iniciá sesión desde la app.</p>');
    if (url.pathname === '/api/session' || url.pathname === '/api/sync' || url.pathname === '/api/logout' || url.pathname.startsWith('/api/admin/')) {
      const bearer = request.headers.get('Authorization') || '';
      if (!bearer.startsWith('Bearer ') || bearer.length > 16384) return json({ error: 'unauthorized' }, 401);
      const token = bearer.slice(7);
      let user;
      try {
        user = token.startsWith('cd.') && validProof(token.slice(3))
          ? await call(env, `access_${await hash(token)}`, 'access_read', {})
          : await verifySyncToken(token, env);
        if (!user || typeof user.email !== 'string') return json({ error: 'unauthorized' }, 401);
      } catch (error) {
        const unavailable = error instanceof ServiceError && error.status >= 500 ||
          error?.code === 'ERR_JWKS_TIMEOUT' || error instanceof TypeError;
        return json({ error: unavailable ? 'provider_unavailable' : 'unauthorized' }, unavailable ? 503 : 401);
      }
      if (url.pathname === '/api/session') {
        if (request.method === 'GET') return json(user);
        if (request.method !== 'POST') return json({ error: 'method_not_allowed' }, 405);
        // Only a freshly verified provider token can create another long-lived session.
        if (token.startsWith('cd.')) return json({ error: 'provider_token_required' }, 400);
        const ip = request.headers.get('CF-Connecting-IP') || 'local';
        if (!(await env.RATE_LIMITER.limit({ key: ip })).success) return json({ error: 'too_many_requests' }, 429);
        const syncToken = `cd.${random()}`;
        await call(env, `access_${await hash(syncToken)}`, 'access_create', { user });
        return json({ ...user, syncToken });
      }
      if (url.pathname === '/api/logout' && request.method === 'POST') {
        if (token.startsWith('cd.')) await call(env, `access_${await hash(token)}`, 'access_revoke', {});
        return json({ ok: true });
      }
      if (url.pathname.startsWith('/api/admin/')) {
        if (request.method !== 'GET') return json({ error: 'method_not_allowed' }, 405);
        try {
          const directory = env.ADMIN_DIRECTORY.getByName('directory-v1');
          const allowed = await directory.authorize(user);
          if (url.pathname === '/api/admin/access') return json({ allowed });
          if (!allowed) return json({ error: 'forbidden' }, 403);
          if (url.pathname === '/api/admin/overview') {
            const after = url.searchParams.get('after') || '';
            if (after.length > 512) return json({ error: 'invalid' }, 400);
            return json(await directory.overview(after));
          }
          if (url.pathname === '/api/admin/account') {
            const subject = url.searchParams.get('subject') || '';
            if (!subject || subject.length > 512) return json({ error: 'invalid' }, 400);
            const accountInfo = await directory.account(subject);
            if (!accountInfo) return json({ error: 'not_found' }, 404);
            const emailHash = accountInfo.email ? await hash(accountInfo.email.trim().toLowerCase()) : '';
            let document = emailHash ? await call(env, `sync_v2_email_${emailHash}`, 'sync_read', {}) : { revision: 0, backup: emptyBackup() };
            if (document.revision === 0) {
              const identities = await directory.subjectsByEmail(accountInfo.email);
              for (const identity of identities) {
                const legacy = await call(env, `sync_v2_google_${identity.subject}`, 'sync_read', {});
                if (legacy.revision > 0) { document = legacy; break; }
              }
            }
            return json({ account: accountInfo, revision: document.revision,
              beans: document.backup.beans, shots: document.backup.shots });
          }
          return json({ error: 'not_found' }, 404);
        } catch { return json({ error: 'admin_unavailable' }, 503); }
      }
      const normalizedEmail = user.email.trim().toLowerCase();
      const emailHash = await hash(normalizedEmail);
      const account = `sync_v2_email_${emailHash}`;
      try {
        if (request.method === 'GET') {
          let document = await call(env, account, 'sync_read', {});
          if (document.revision === 0) {
            // Check legacy google namespace sync_v2_google_${user.id} first
            let googleLegacy = await call(env, `sync_v2_google_${user.id}`, 'sync_read', {});
            if (googleLegacy.revision > 0 && googleLegacy.backup?.shots?.length > 0) {
              document = await call(env, account, 'sync_seed', { backup: googleLegacy.backup });
            } else {
              // Look up all subjects for this email in AdminDirectory
              let foundLegacy = false;
              if (env.ADMIN_DIRECTORY) {
                try {
                  const directory = env.ADMIN_DIRECTORY.getByName('directory-v1');
                  const subjects = await directory.subjectsByEmail(normalizedEmail);
                  for (const sub of subjects) {
                    if (sub.subject === user.id) continue;
                    const subLegacy = await call(env, `sync_v2_google_${sub.subject}`, 'sync_read', {});
                    if (subLegacy.revision > 0 && subLegacy.backup?.shots?.length > 0) {
                      document = await call(env, account, 'sync_seed', { backup: subLegacy.backup });
                      foundLegacy = true;
                      break;
                    }
                  }
                } catch {}
              }
              if (!foundLegacy && document.revision === 0 && normalizedEmail) {
                // Check legacy email namespace sync_${normalizedEmail}
                const legacy = await call(env, `sync_${normalizedEmail}`, 'legacy_read', {});
                if (legacy.data) document = await call(env, account, 'sync_seed', { backup: JSON.parse(legacy.data) });
              }
            }
          }
          await recordAdmin(env, user, document);
          return json(document);
        }
        if (request.method === 'POST') {
          const input = await boundedJson(request, 1500000);
          const result = await call(env, account, 'sync_write', input);
          if (!result.conflict) await recordAdmin(env, user, result);
          return json(result, result.conflict ? 409 : 200);
        }
        return json({ error: 'method_not_allowed' }, 405);
      } catch (error) {
        const failure = serviceError(error);
        console.warn(JSON.stringify({ event: 'sync_rejected', code: failure.message, status: failure.status }));
        return json({ error: failure.message }, failure.status);
      }
    }

    if (!env.GOOGLE_CLIENT_SECRET) return html('<p>El acceso web todavía no está configurado. Volvé a la app.</p>', 503);

    try {
      const ip = request.headers.get('CF-Connecting-IP') || 'local';
      if (!(await env.RATE_LIMITER.limit({ key: ip })).success) return json({ error: 'too_many_requests' }, 429);
      if (url.pathname === '/google/start' && request.method === 'GET') {
        const challenge = url.searchParams.get('challenge'), appState = url.searchParams.get('state');
        if (!validProof(challenge) || !validProof(appState)) return json({ error: 'invalid' }, 400);
        const destination = loginDestination(url, request.headers.get('User-Agent') || '');
        const state = random(), browser = random();
        const result = await call(env, state, 'init', { challenge, appState, browserHash: await hash(browser), ...destination });
        const google = new URL('https://accounts.google.com/o/oauth2/v2/auth');
        google.search = new URLSearchParams({ client_id: env.GOOGLE_CLIENT_ID, redirect_uri: `${env.PUBLIC_ORIGIN}/google/callback`,
          response_type: 'code', scope: 'openid email profile', state, nonce: result.nonce,
          code_challenge: result.googleChallenge, code_challenge_method: 'S256', prompt: 'select_account' }).toString();
        return new Response(null, { status: 302, headers: { ...headers, Location: google.toString(), 'Set-Cookie': cookie(state, browser, 300) } });
      }
      if (url.pathname === '/google/callback' && request.method === 'GET') {
        const state = url.searchParams.get('state');
        if (!validProof(state)) throw new Error('state');
        const browser = (request.headers.get('Cookie') || '').split(';').map(v => v.trim()).find(v => v.startsWith(`${cookieName(state)}=`))?.split('=')[1];
        if (!validProof(browser)) throw new Error('cookie');
        const result = await call(env, state, 'callback', { browserHash: await hash(browser), code: url.searchParams.get('code'), error: url.searchParams.get('error') });
        const destination = callbackDestination(result, state);
        const extra = { 'Set-Cookie': cookie(state, '', 0) };
        if (destination.platform === 'web') {
          return new Response(null, { status: 302, headers: { ...headers, ...extra, Location: destination.location } });
        }
        const link = destination.location.replaceAll('&', '&amp;');
        const label = destination.platform === 'ios' ? 'iOS' : 'Android';
        return html(`<p>${result.ticket ? 'Acceso confirmado.' : 'No se completó el acceso.'}</p><p><a href="${link}">Volver a Coffee Dial (${label})</a></p>`, 200, extra);
      }
      if (url.pathname === '/exchange' && request.method === 'POST') {
        const input = await boundedJson(request);
        if (!validProof(input.attempt)) throw new Error('attempt');
        const user = await call(env, input.attempt, 'consume', input);
        const syncToken = `cd.${random()}`;
        await call(env, `access_${await hash(syncToken)}`, 'access_create', { user });
        return json({ ...user, syncToken });
      }
      return json({ error: 'not_found' }, 404);
    } catch { return html('<p>Este intento venció o no es válido. Volvé a Coffee Dial e iniciá sesión nuevamente.</p>', 400); }
  },
};
