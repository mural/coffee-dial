// Versioned snapshot CAS. Tombstones are permanent: UUIDs must never be reused.
export const emptyBackup = () => ({ format: 'coffee-dial-backup', schemaVersion: 1, exportedAt: new Date().toISOString(), beans: [], shots: [], machines: [], cups: [], grinders: [] });
export const emptySync = () => ({ protocol: 2, revision: 0, backup: emptyBackup(), deleted: { beans: [], shots: [], machines: [], cups: [], grinders: [] } });
export function validateBackup(b) {
  if (!b || b.format !== 'coffee-dial-backup' || ![1, 2, 3, 4, 5, 6, 7, 8, 9, 10].includes(b.schemaVersion) || !Number.isFinite(Date.parse(b.exportedAt))) throw new Error('invalid_backup');
  const text = (v, max = 10000) => typeof v === 'string' && v.length <= max;
  const id = v => text(v, 200) && v.trim().length > 0;
  const positive = v => typeof v === 'number' && Number.isFinite(v) && v > 0;
  b.machines ??= [];
  b.cups ??= [];
  b.grinders ??= [];
  for (const kind of ['beans', 'shots', 'machines', 'cups', 'grinders']) {
    if (!Array.isArray(b[kind]) || b[kind].length > 50000 || b[kind].some(x => !x || !id(x.id)) || new Set(b[kind].map(x => x.id)).size !== b[kind].length) throw new Error('invalid_ids');
  }
  if (b.beans.some(x => x.archived !== undefined && typeof x.archived !== 'boolean')) throw new Error('invalid_bean');
  if (b.beans.some(x => x.photo != null && (b.schemaVersion < 5 ||
    typeof x.photo !== 'object' || !/^[a-f0-9-]{36}$/.test(x.photo.id) || x.photo.jpeg != null))) throw new Error('invalid_photo');
  if (b.shots.some(x => x.firstDrop != null && (b.schemaVersion < 6 || typeof x.firstDrop !== 'number' || !Number.isFinite(x.firstDrop) || x.firstDrop < 0 || x.firstDrop > x.seconds))) throw new Error('invalid_shot');
  if (b.shots.some(x => x.taste != null && (b.schemaVersion < 7 || typeof x.taste !== 'object' || Array.isArray(x.taste) ||
    (x.taste.impression != null && !['TOO_SOUR', 'TOO_BITTER', 'WEAK', 'BALANCED', 'TOO_STRONG'].includes(x.taste.impression)) ||
    ['body', 'acidity', 'bitterness'].some(k => x.taste[k] != null && (!Number.isInteger(x.taste[k]) || x.taste[k] < 0 || x.taste[k] > 10))))) throw new Error('invalid_shot');
  const optionalChoice = (value, choices) => value == null || choices.includes(value);
  if (b.shots.some(x => x.dialIn != null && (b.schemaVersion < 8 || typeof x.dialIn !== 'object' || Array.isArray(x.dialIn) ||
    !optionalChoice(x.dialIn.roast, ['LIGHT','MEDIUM','DARK','UNKNOWN']) ||
    !optionalChoice(x.dialIn.method, ['ESPRESSO','FILTER','MOKA','OTHER']) ||
    !optionalChoice(x.dialIn.goal, ['STRONGER','BALANCED','MILDER']) ||
    !optionalChoice(x.dialIn.tasteBasis, ['ESPRESSO','FINISHED_DRINK']) ||
    (x.dialIn.grinder !== undefined && !text(x.dialIn.grinder, 200)) ||
    (x.dialIn.favorite !== undefined && typeof x.dialIn.favorite !== 'boolean') ||
    (x.dialIn.previousShotId != null && (!id(x.dialIn.previousShotId) || x.dialIn.previousShotId === x.id))))) throw new Error('invalid_shot');
  const beans = new Set(b.beans.map(x => x.id));
  if (b.beans.some(x => !text(x.name) || !x.name.trim() || !text(x.roaster))) throw new Error('invalid_bean');
  if (b.machines.some(x => !text(x.name) || !x.name.trim() || !text(x.type) || !text(x.year))) throw new Error('invalid_machine');
  if (b.cups.some(x => !text(x.name) || !x.name.trim() || (x.weight != null && !positive(x.weight)))) throw new Error('invalid_cup');
  if (b.grinders.some(x => !text(x.name) || !x.name.trim() || !text(x.type) || !x.type.trim())) throw new Error('invalid_grinder');
  if (b.shots.some(x => !beans.has(x.beanId) || !Number.isSafeInteger(x.createdAt) || x.createdAt < 0 || x.createdAt > 253402300799999 || !positive(x.dose) || !positive(x.output) || !positive(x.seconds) || !text(x.grind) || !x.grind.trim() || !text(x.notes, 1000000) || !Number.isInteger(x.rating) || x.rating < 1 || x.rating > 5 || (x.temperature != null && !positive(x.temperature)) || (x.milk != null && (!positive(x.milk) || x.milk < 1 || x.milk > 200)) || (x.machine != null && !text(x.machine)) || (x.cup != null && !text(x.cup)) || (x.extraWater != null && (!positive(x.extraWater) || x.extraWater < 1 || x.extraWater > 1000)) || (x.style != null && !text(x.style, 100)) || (b.schemaVersion < 2 && (x.extraWater != null || x.style != null)))) throw new Error('invalid_shot');
  const machineIds = new Set(b.machines.map(x => x.id));
  const cupIds = new Set(b.cups.map(x => x.id));
  const grinderIds = new Set(b.grinders.map(x => x.id));
  if (b.shots.some(x => (x.machineId != null || x.cupId != null) && b.schemaVersion < 9 ||
    (x.machineId != null && (!id(x.machineId) || !machineIds.has(x.machineId))) ||
    (x.cupId != null && (!id(x.cupId) || !cupIds.has(x.cupId))) ||
    (x.grinder != null && (b.schemaVersion < 10 || !text(x.grinder))) ||
    (x.grinderId != null && (b.schemaVersion < 10 || !id(x.grinderId) || !grinderIds.has(x.grinderId))))) throw new Error('invalid_shot');
}
export async function updateSync(storage, input) {
  if (!input || typeof input !== 'object') throw new Error('invalid_protocol');
  // Kotlin clients historically omitted values equal to serializer defaults.
  input.protocol ??= 2;
  if (input.force !== undefined && typeof input.force !== 'boolean') throw new Error('invalid_protocol');
  validateBackup(input.backup);
  if (input.protocol !== 2 || !Number.isSafeInteger(input.baseRevision) || input.baseRevision < 0) throw new Error('invalid_protocol');
  return storage.transaction(async tx => {
    const previous = await tx.get('sync_v2') || emptySync();
    if (input.backup.schemaVersion < previous.backup.schemaVersion) throw new Error("backup_upgrade_required");
    if (previous.revision !== input.baseRevision) return { conflict: true };
    const deleted = {};
    for (const kind of ['beans', 'shots', 'machines', 'cups', 'grinders']) {
      const tombstones = new Set(previous.deleted[kind] || []);
      const ids = new Set(input.backup[kind].map(x => x.id));
      if (!input.force && [...ids].some(id => tombstones.has(id))) return { conflict: true };
      for (const record of previous.backup[kind] || []) if (!ids.has(record.id)) tombstones.add(record.id);
      if (input.force) for (const id of ids) tombstones.delete(id);
      deleted[kind] = [...tombstones].sort();
    }
    const next = { protocol: 2, revision: previous.revision + 1, backup: input.backup, deleted };
    if (new TextEncoder().encode(JSON.stringify(next)).length > 1500000) throw new Error('sync_capacity');
    await tx.put('sync_v2', next);
    return next;
  });
}

export async function seedLegacy(storage, backup) {
  backup.machines ??= [];
  backup.cups ??= [];
  backup.grinders ??= [];
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
