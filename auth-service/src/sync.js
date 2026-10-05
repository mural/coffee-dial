// Versioned snapshot CAS. Tombstones are permanent: UUIDs must never be reused.
export const emptyBackup = () => ({ format: 'coffee-dial-backup', schemaVersion: 1, exportedAt: new Date().toISOString(), beans: [], shots: [], machines: [] });
export const emptySync = () => ({ protocol: 2, revision: 0, backup: emptyBackup(), deleted: { beans: [], shots: [], machines: [] } });
export function validateBackup(b) {
  if (!b || b.format !== 'coffee-dial-backup' || b.schemaVersion !== 1 || !Number.isFinite(Date.parse(b.exportedAt))) throw new Error('invalid_backup');
  const text = (v, max = 10000) => typeof v === 'string' && v.length <= max;
  const id = v => text(v, 200) && v.trim().length > 0;
  const positive = v => typeof v === 'number' && Number.isFinite(v) && v > 0;
  for (const kind of ['beans', 'shots', 'machines']) {
    if (!Array.isArray(b[kind]) || b[kind].length > 50000 || b[kind].some(x => !x || !id(x.id)) || new Set(b[kind].map(x => x.id)).size !== b[kind].length) throw new Error('invalid_ids');
  }
  const beans = new Set(b.beans.map(x => x.id));
  if (b.beans.some(x => !text(x.name) || !x.name.trim() || !text(x.roaster))) throw new Error('invalid_bean');
  if (b.machines.some(x => !text(x.name) || !x.name.trim() || !text(x.type) || !text(x.year))) throw new Error('invalid_machine');
  if (b.shots.some(x => !beans.has(x.beanId) || !Number.isSafeInteger(x.createdAt) || x.createdAt < 0 || x.createdAt > 253402300799999 || !positive(x.dose) || !positive(x.output) || !positive(x.seconds) || !text(x.grind) || !x.grind.trim() || !text(x.notes, 1000000) || !Number.isInteger(x.rating) || x.rating < 1 || x.rating > 5 || (x.temperature != null && !positive(x.temperature)) || (x.milk != null && (!positive(x.milk) || x.milk < 1 || x.milk > 200)) || (x.machine != null && !text(x.machine)))) throw new Error('invalid_shot');
}
export async function updateSync(storage, input) {
  validateBackup(input.backup);
  if (input.protocol !== 2 || !Number.isSafeInteger(input.baseRevision) || input.baseRevision < 0) throw new Error('invalid_protocol');
  return storage.transaction(async tx => {
    const previous = await tx.get('sync_v2') || emptySync();
    if (previous.revision !== input.baseRevision) return { conflict: true };
    const deleted = {};
    for (const kind of ['beans', 'shots', 'machines']) {
      const tombstones = new Set(previous.deleted[kind]);
      const ids = new Set(input.backup[kind].map(x => x.id));
      if ([...ids].some(id => tombstones.has(id))) return { conflict: true };
      for (const record of previous.backup[kind]) if (!ids.has(record.id)) tombstones.add(record.id);
      deleted[kind] = [...tombstones].sort();
    }
    const next = { protocol: 2, revision: previous.revision + 1, backup: input.backup, deleted };
    // SQLite-backed KV values are bounded. Leave headroom under its 2 MiB value limit.
    if (new TextEncoder().encode(JSON.stringify(next)).length > 1500000) throw new Error('sync_capacity');
    await tx.put('sync_v2', next);
    return next;
  });
}

export async function seedLegacy(storage, backup) {
  backup.machines ??= [];
  validateBackup(backup);
  return storage.transaction(async tx => {
    const existing = await tx.get('sync_v2');
    if (existing) return existing;
    const next = { ...emptySync(), revision: 1, backup };
    if (new TextEncoder().encode(JSON.stringify(next)).length > 1500000) throw new Error('sync_capacity');
    await tx.put('sync_v2', next);
    return next;
  });
}
