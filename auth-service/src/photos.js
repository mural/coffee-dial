import { hash } from './core.js';

// All object keys are scoped by the server-verified account, never a client email.
export async function photoRequest(request, env, user, id, json, boundedJson) {
  if (!/^[a-f0-9-]{36}$/.test(id)) return json({ error: 'invalid_photo' }, 400);
  if (!env.PHOTOS) return json({ error: 'photos_unavailable' }, 503);
  const key = `${await hash(user.email.trim().toLowerCase())}/${id}`;
  try {
    if (request.method === 'GET') {
      const object = await env.PHOTOS.get(key);
      if (!object) return json({ error: 'photo_not_found' }, 404);
      return json({ id, jpeg: await object.text() });
    }
    if (request.method !== 'POST') return json({ error: 'method_not_allowed' }, 405);
    const input = await boundedJson(request, 140000);
    if (input.id !== id || typeof input.jpeg !== 'string' || input.jpeg.length > 136536) return json({ error: 'invalid_photo' }, 400);
    let bytes;
    try { bytes = Uint8Array.from(atob(input.jpeg), c => c.charCodeAt(0)); } catch { return json({ error: 'invalid_photo' }, 400); }
    if (!validJpeg(bytes)) return json({ error: 'invalid_photo' }, 400);
    // Immutable IDs allow safe retries without replacing another device's cached image.
    const previous = await env.PHOTOS.get(key);
    if (previous) {
      if (await previous.text() !== input.jpeg) return json({ error: 'photo_conflict' }, 409);
    } else {
      if (!(await env.RATE_LIMITER.limit({ key: `photo:${await hash(user.email)}` })).success) return json({ error: 'too_many_requests' }, 429);
      const stored = await env.PHOTOS.put(key, input.jpeg, { onlyIf: { etagDoesNotMatch: '*' } });
      if (!stored) {
        const winner = await env.PHOTOS.get(key);
        if (!winner || await winner.text() !== input.jpeg) return json({ error: 'photo_conflict' }, 409);
      }
    }
    return json({ ok: true });
  } catch { return json({ error: 'photos_unavailable' }, 503); }
}

export function validJpeg(bytes) {
  if (bytes.length < 4 || bytes.length > 102400 || bytes[0] !== 255 || bytes[1] !== 216 || bytes.at(-2) !== 255 || bytes.at(-1) !== 217) return false;
  let i = 2;
  while (i + 8 < bytes.length) {
    if (bytes[i++] !== 255) return false;
    const marker = bytes[i++];
    const length = bytes[i] * 256 + bytes[i + 1];
    if (length < 2 || i + length > bytes.length) return false;
    if ([192, 193, 194].includes(marker)) {
      const height = bytes[i + 3] * 256 + bytes[i + 4], width = bytes[i + 5] * 256 + bytes[i + 6];
      return height > 0 && width > 0 && height <= 640 && width <= 640;
    }
    i += length;
  }
  return false;
}
