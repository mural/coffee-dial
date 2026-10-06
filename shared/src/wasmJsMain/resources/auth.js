/* Browser login: transient PKCE proof stays in this tab; URL data never establishes identity. */
(() => {
  const origin = 'https://auth-coffee.muralooo.win';
  const key = 'coffee_oauth_attempt';
  const userKey = 'coffee_verified_profile_v1';
  const proof = value => typeof value === 'string' && /^[A-Za-z0-9_-]{43}$/.test(value);
  const b64 = bytes => btoa(String.fromCharCode(...bytes)).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
  const random = () => b64(crypto.getRandomValues(new Uint8Array(32)));
  globalThis.CoffeeAuth = {
    async start() {
      const verifier = random(), state = random();
      const challenge = b64(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))));
      sessionStorage.setItem(key, JSON.stringify({ verifier, state, created: Date.now() }));
      const url = new URL('/google/start', origin);
      url.search = new URLSearchParams({ challenge, state, platform: 'web', return_to: location.origin + '/' });
      location.assign(url.href);
      return '';
    },
    async complete() {
      const params = new URLSearchParams(location.hash.slice(1));
      if (!params.has('attempt')) {
        const saved = localStorage.getItem(userKey) || sessionStorage.getItem(userKey);
        if (!saved) return '';
        const user = JSON.parse(saved);
        if (typeof user.syncToken !== 'string') return '';
        try {
          const response = await fetch(origin + '/api/session', {
            headers: { Authorization: 'Bearer ' + user.syncToken },
            signal: AbortSignal.timeout(10000)
          });
          if (response.status === 401) { this.clear(); return ''; }
          if (!response.ok) throw new Error('No se pudo validar la sesión');
          const verified = await response.json();
          if (verified.id !== user.id) { this.clear(); return ''; }
          const value = JSON.stringify({ ...verified, syncToken: user.syncToken });
          localStorage.setItem(userKey, value);
          sessionStorage.removeItem(userKey);
          return value;
        } catch {
          // Offline profile is only UI state; the server still validates every sync.
          return saved;
        }
      }
      localStorage.removeItem(userKey);
      history.replaceState(null, '', location.pathname + location.search);
      const pending = JSON.parse(sessionStorage.getItem(key) || 'null');
      sessionStorage.removeItem(key);
      if (!pending || pending.state !== params.get('state') || Date.now() - pending.created < 0 || Date.now() - pending.created > 300000) throw new Error('Intento vencido');
      if (params.has('error')) throw new Error('No se completó el acceso');
      const attempt = params.get('attempt'), ticket = params.get('code');
      if (!proof(attempt) || !proof(ticket) || !proof(pending.verifier)) throw new Error('Retorno inválido');
      const response = await fetch(origin + '/exchange', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ attempt, ticket, verifier: pending.verifier, appState: pending.state })
      });
      if (!response.ok) throw new Error('No se pudo verificar el acceso');
      const user = await response.json();
      if (typeof user.id !== 'string' || !user.id) throw new Error('Identidad inválida');
      const value = JSON.stringify(user);
      localStorage.setItem(userKey, value);
      sessionStorage.removeItem(userKey);
      return value;
    },
    clear() {
      sessionStorage.removeItem(key);
      sessionStorage.removeItem(userKey);
      localStorage.removeItem(userKey);
      localStorage.removeItem('coffee_user_email');
      localStorage.removeItem('coffee_user_name');
    }
  };
})();
