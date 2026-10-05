# Coffee Dial: login y sync

OAuth Google para web y fallback Android, y sync autenticado en Cloudflare. Ver [protocolo, migración y límites](../docs/SYNC.md).

## Verificar

```sh
npm ci
npm test
npm run check
```

Los tests de runtime usan el Miniflare y esbuild incluidos en el lockfile de Wrangler, sin conexiones a almacenamiento remoto. La prueba opcional de callback cancelado requiere el Worker local:

```sh
npx wrangler dev --config wrangler.local.jsonc --local --port 8787
WORKER_TEST_ORIGIN=http://localhost:8787 npm test
```

La configuración local contiene un secreto ficticio y no permite login real.

## Publicar

```sh
npm run deploy
```

Cuenta, dominio, allowlist y clientes públicos de Google están en wrangler.jsonc. El secreto GOOGLE_CLIENT_SECRET existente se mantiene en Cloudflare: no pasarlo como argumento ni guardarlo en Git. Google conserva el retorno `https://auth-coffee.muralooo.win/google/callback`.

Publicar la web compatible inmediatamente después y actualizar las apps nativas. Comprobar que /health devuelve ready y /api/sync sin Bearer devuelve 401, incluso con un email. Nunca probar escrituras con datos reales como test técnico.

Logs de invocación y trazas siguen deshabilitados para no conservar parámetros OAuth. Las respuestas usan no-store y no-referrer. Las sesiones tienen vencimiento y revocación; las operaciones de datos usan revisión transaccional y tombstones. Los backups antiguos permanecen intactos durante la migración autenticada.
