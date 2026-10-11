import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateBackup, emptyBackup } from '../src/sync.js';
test('v8 preserves dial-in context and rejects malformed or downgraded metadata', () => {
  const b = { ...emptyBackup(), schemaVersion: 8, beans: [{ id: 'b', name: 'Brasil', roaster: '' }],
    shots: [{ id: 's', beanId: 'b', createdAt: 1, dose: 16, output: 36, seconds: 28, grind: 'Medio', notes: '', rating: 4,
      dialIn: { roast: 'MEDIUM', method: 'ESPRESSO', goal: 'STRONGER', tasteBasis: 'ESPRESSO', grinder: 'J-Ultra', previousShotId: 'previous', favorite: true } }] };
  validateBackup(b);
  assert.deepEqual(JSON.parse(JSON.stringify(b)).shots[0].dialIn, b.shots[0].dialIn);
  assert.throws(() => validateBackup({ ...b, schemaVersion: 7 }));
  for (const patch of [{ grinder: null }, { favorite: null }, { favorite: 'true' }, { roast: 'invalid' }, { previousShotId: 's' }, { grinder: 'x'.repeat(201) }]) {
    assert.throws(() => validateBackup({ ...b, shots: [{ ...b.shots[0], dialIn: { ...b.shots[0].dialIn, ...patch } }] }));
  }
});
