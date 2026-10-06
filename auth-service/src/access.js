// Coffee Dial sessions expire after 30 days without authenticated use.
export const SESSION_TTL = 30 * 24 * 60 * 60 * 1000;
export async function createAccess(storage, user, now = Date.now()) {
  const expires = now + SESSION_TTL;
  await storage.transaction(async tx => {
    await tx.put('access', { user, expires });
    await tx.setAlarm(expires);
  });
}
export async function readAccess(storage, now = Date.now()) {
  return storage.transaction(async tx => {
    const access = await tx.get('access');
    if (!access || access.expires <= now) return null;
    // Renew at most once per day; an expired credential never renews itself.
    if (access.expires - now < SESSION_TTL - 24 * 60 * 60 * 1000) {
      access.expires = now + SESSION_TTL;
      await tx.put('access', access);
      await tx.setAlarm(access.expires);
    }
    return access.user;
  });
}
