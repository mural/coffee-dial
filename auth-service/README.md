# Coffee Dial: Google web login

Fallback para Android sin Play Services. Los datos de café permanecen en el dispositivo.

## Verificación local

```sh
npm ci
npm test
npm run check
npx wrangler dev --config wrangler.local.jsonc --local --port 8787
# En otra terminal:
WORKER_TEST_ORIGIN=http://localhost:8787 npm test
```

La configuración local contiene un secreto ficticio: no permite login real. La integración prueba el retorno cancelado y el rechazo de replays.

## Despliegue

Cuenta y dominio están en wrangler.jsonc.

```sh
npm run deploy
npx wrangler secret put GOOGLE_CLIENT_SECRET
```

Ingresar el secreto mediante el prompt, nunca como argumento. El comando de secreto despliega una nueva versión. Registrar en Google el retorno exacto https://auth-coffee.muralooo.win/google/callback. Comprobar /health y probar Android de extremo a extremo.

Logs y trazas están deshabilitados para evitar persistir parámetros OAuth sensibles. Las respuestas usan no-store y no-referrer. Los intentos transitorios se eliminan al consumirse, fallar o vencer; hay límites por IP.

Al ampliar las pruebas, actualizar los usuarios de prueba de Google y ALLOWED_EMAILS. Este login identifica un perfil local; no autoriza acceso a un backend de datos.
