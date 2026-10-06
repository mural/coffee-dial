export { AdminDirectory } from './admin.js';
import { createAccess, readAccess } from './access.js';
import { emptySync, updateSync, seedLegacy } from './sync.js';
import { loginDestination, callbackDestination } from './returns.js';
import { DurableObject } from 'cloudflare:workers';
import { Session, random, hash, validProof, verifySyncGoogle } from './core.js';

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
const session = (env, id) => env.SESSIONS.get(env.SESSIONS.idFromName(id));

async function call(env, id, action, input) {
  const result = await session(env, id).fetch(new Request(`https://session/${action}`, { method: 'POST', body: JSON.stringify(input) }));
  if (!result.ok) throw new Error('invalid');
  return result.json();
}

async function boundedJson(request, limit = 16384) {
  if (!(request.headers.get('Content-Type') || '').startsWith('application/json')) throw new Error('type');
  const reader = request.body?.getReader(); if (!reader) throw new Error('body');
  let size = 0, text = ''; const decoder = new TextDecoder();
  while (true) { const { done, value } = await reader.read(); if (done) break;
    size += value.length; if (size > limit) { await reader.cancel(); throw new Error('size'); }
    text += decoder.decode(value, { stream: true }); }
  return JSON.parse(text + decoder.decode());
}

export class LoginAttempt extends DurableObject {
  async fetch(request) {
    try {
      const s = new Session(this.ctx.storage, this.env);
      const action = new URL(request.url).pathname;
      const input = await request.json();
      if (action === '/legacy_read') return json({ data: await this.ctx.storage.get('user_sync_data') || null });
      if (action === '/sync_seed') return json(await seedLegacy(this.ctx.storage, input.backup));
      if (action === '/sync_read') return json(await this.ctx.storage.get('sync_v2') || emptySync());
      if (action === '/sync_write') return json(await updateSync(this.ctx.storage, input));
      if (action === '/access_create') {
        await createAccess(this.ctx.storage, input.user);
        return json({ ok: true });
      }
      if (action === '/access_read') {
        const user = await readAccess(this.ctx.storage);
        if (!user) return json({ error: 'unauthorized' }, 401);
        return json(user);
      }
      if (action === '/access_revoke') { await this.ctx.storage.deleteAll(); return json({ ok: true }); }
      if (action === '/init') return json(await s.init(input));
      if (action === '/callback') return json(await s.callback(input));
      if (action === '/consume') return json(await s.consume(input));
      return json({ error: 'invalid' }, 400);
    } catch { return json({ error: 'invalid_or_expired' }, 400); }
  }
  async alarm() {
    await this.ctx.storage.transaction(async tx => {
      const access = await tx.get('access');
      if (access && access.expires > Date.now()) await tx.setAlarm(access.expires);
      else await tx.deleteAll();
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
          : await verifySyncGoogle(token, env);
      } catch { return json({ error: 'unauthorized' }, 401); }
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
            if (after.length > 200) return json({ error: 'invalid' }, 400);
            return json(await directory.overview(after));
          }
          if (url.pathname === '/api/admin/account') {
            const subject = url.searchParams.get('subject') || '';
            if (!subject || subject.length > 200) return json({ error: 'invalid' }, 400);
            const account = await directory.account(subject);
            if (!account) return json({ error: 'not_found' }, 404);
            const document = await call(env, `sync_v2_google_${subject}`, 'sync_read', {});
            return json({ account, revision: document.revision,
              beans: document.backup.beans, shots: document.backup.shots });
          }
          return json({ error: 'not_found' }, 404);
        } catch { return json({ error: 'admin_unavailable' }, 503); }
      }
      const account = `sync_v2_google_${user.id}`;
      try {
        if (request.method === 'GET') {
          let document = await call(env, account, 'sync_read', {});
          if (document.revision === 0) {
            // Only a cryptographically verified email may recover the legacy namespace.
            // Its original contents remain untouched for recovery.
            const legacy = await call(env, `sync_${user.email.trim().toLowerCase()}`, 'legacy_read', {});
            if (legacy.data) document = await call(env, account, 'sync_seed', { backup: JSON.parse(legacy.data) });
          }
          if (env.ADMIN_DIRECTORY) await env.ADMIN_DIRECTORY.getByName('directory-v1').record(user, document);
          return json(document);
        }
        if (request.method === 'POST') {
          const input = await boundedJson(request, 1500000);
          const result = await call(env, account, 'sync_write', input);
          if (!result.conflict && env.ADMIN_DIRECTORY) await env.ADMIN_DIRECTORY.getByName('directory-v1').record(user, result);
          return json(result, result.conflict ? 409 : 200);
        }
        return json({ error: 'method_not_allowed' }, 405);
      } catch { return json({ error: 'sync_failed' }, 400); }
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
