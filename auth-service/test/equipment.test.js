import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateBackup, emptyBackup } from '../src/sync.js';

const shot = (extra) => ({
  id: 's', beanId: 'b', createdAt: 1, dose: 18, output: 36, seconds: 28, grind: 'Medio', notes: '', rating: 4,
  ...extra
});

test('v9 accepts equipment ids and rejects unknown or downgraded links', () => {
  const backup = {
    ...emptyBackup(), schemaVersion: 9,
    beans: [{ id: 'b', name: 'Brasil', roaster: '' }],
    machines: [{ id: 'm1', name: 'Bianca', type: 'Espresso', year: '' }],
    cups: [{ id: 'c1', name: 'Cerámica', weight: 180 }],
    shots: [shot({ machine: 'Bianca', machineId: 'm1', cup: 'Cerámica', cupId: 'c1' })]
  };
  validateBackup(backup);
  assert.throws(() => validateBackup({ ...backup, schemaVersion: 8 }));
  assert.throws(() => validateBackup({
    ...backup, shots: [shot({ machineId: 'missing' })]
  }));
  assert.throws(() => validateBackup({
    ...backup, shots: [shot({ cupId: 'missing' })]
  }));
});
