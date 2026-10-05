const production = 'https://coffee.muralooo.win';
export function loginDestination(url, userAgent = '') {
  const platform = url.searchParams.get('platform') || (/iPhone|iPad|iPod/i.test(userAgent) ? 'ios' : 'android');
  if (!['web', 'android', 'ios'].includes(platform)) throw new Error('platform');
  if (platform !== 'web') {
    if (url.searchParams.has('return_to')) throw new Error('return_to');
    return { platform, returnTo: 'coffeedial://auth/google' };
  }
  const target = new URL(url.searchParams.get('return_to') || production);
  const loopback = ['localhost', '127.0.0.1', '[::1]'].includes(target.hostname);
  if (target.username || target.password || target.search || target.hash || target.pathname !== '/' ||
      !(target.origin === production || (loopback && ['http:', 'https:'].includes(target.protocol)))) throw new Error('return_to');
  return { platform, returnTo: target.origin + '/' };
}
export function callbackDestination(result, attempt) {
  const params = new URLSearchParams({ state: result.appState, attempt });
  if (result.ticket) params.set('code', result.ticket); else params.set('error', result.error);
  const platform = result.platform || 'android';
  const base = platform === 'web' ? loginDestination(new URL('https://auth.test/?platform=web&return_to=' + encodeURIComponent(result.returnTo))).returnTo : 'coffeedial://auth/google';
  return { platform, location: base + (platform === 'web' ? '#' : '?') + params };
}
