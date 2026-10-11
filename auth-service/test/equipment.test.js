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

test('v10 accepts a grinder link and rejects a downgrade or an unknown id', () => {
  const backup = {
    ...emptyBackup(), schemaVersion: 10,
    beans: [{ id: 'b', name: 'Brasil', roaster: '' }],
    grinders: [{ id: 'g1', name: 'Niche', type: 'Eléctrico' }],
    shots: [shot({ grinder: 'Niche', grinderId: 'g1' })]
  };
  validateBackup(backup);
  assert.throws(() => validateBackup({ ...backup, schemaVersion: 9 }));
  assert.throws(() => validateBackup({
    ...backup, shots: [shot({ grinderId: 'missing' })]
  }));
  assert.throws(() => validateBackup({
    ...backup, grinders: [{ id: 'g1', name: ' ', type: 'Manual' }]
  }));
});
