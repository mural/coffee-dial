# Cuenta: activación y alcance

La app funciona sin cuenta. El login identifica un perfil local; todavía no crea una cuenta en un servidor, ni sincroniza o separa los cafés por usuario. Cerrar sesión no borra los cafés. Los backups no contienen credenciales.

## Estado

- Android / Google: Credential Manager prueba cuentas previamente autorizadas, y si no hay ninguna ofrece el flujo explícito de Google para elegir o agregar una cuenta. La interfaz y el acceso web los gestiona Google Play Services; no se enumeran las cuentas del teléfono ni se pide permiso GET_ACCOUNTS. Cancelar no abre otro login automáticamente.
- iOS / Google: GoogleSignIn 9.0.0 mediante Swift Package Manager. El SDK gestiona la sesión de autenticación web del sistema, el retorno y las credenciales en Keychain. Se restaura la sesión previa mediante el SDK, cuando corresponde.
- iOS / Apple: AuthenticationServices usa la cuenta Apple del dispositivo y el consentimiento del usuario. No puede leer cuentas sin autorización. Restaura una identidad previa solo después de consultar su estado con Apple. El email puede faltar y no se inventa; Apple suele entregar nombre/email solo en el primer consentimiento.
- Android / Apple: pendiente de servicio web OAuth. El botón explica esta limitación. No abre un enlace de autorización incompleto.
- Sin configuración: los botones muestran un mensaje y no fabrican sesiones. Se descartaron los perfiles antiguos guardados únicamente a partir de un email escrito a mano.

## Google Android

1. Crear un proyecto en Google Cloud y configurar Google Auth Platform: branding, audiencia y usuarios de prueba si la app está en modo Testing. No hace falta Firebase ni una base de datos cloud.
2. Crear un cliente OAuth Android con package `com.coffeedial.app` y el SHA-1 del certificado. Obtener el SHA-1 de debug con `./gradlew :androidApp:signingReport`; registrar también el certificado de distribución al publicar.
3. Crear un cliente OAuth de tipo **Web application** en el mismo proyecto. Credential Manager necesita ese client ID como audiencia (no el Android client ID). No colocar ningún client secret en la app.
4. Agregar en `~/.gradle/gradle.properties`:

   ```properties
   coffeeDial.googleWebClientId=TU_CLIENT_ID.apps.googleusercontent.com
   ```

5. Volver a compilar. Probar en un dispositivo/emulador con Google Play Services. El proceso no conserva un token propio: después de reiniciar la app se vuelve a confirmar el acceso con el proveedor. Rotación/cancelación no produce una sesión simulada.

## Google iOS

1. Crear un cliente OAuth **iOS** con bundle ID `com.coffeedial.app`.
2. Copiar `iosApp/Auth.example.xcconfig` a `iosApp/Auth.local.xcconfig` y completar `COFFEE_GOOGLE_IOS_CLIENT_ID` y `COFFEE_GOOGLE_REVERSED_CLIENT_ID` con los valores de Google (el segundo es el URL scheme).
3. En Xcode, Project → Info → Configurations, seleccionar `Auth.local.xcconfig` como configuración del target CoffeeDial, Debug y Release. Alternativamente crear esas dos User-Defined Build Settings en el target. El archivo de ejemplo por sí solo no activa nada.
4. Elegir tu Signing Team. La app debe estar firmada para usar correctamente Keychain en dispositivo.
5. Xcode resuelve GoogleSignIn automáticamente; no hace falta CocoaPods. `Info.plist` y `onOpenURL` ya contienen la conexión al SDK.

## Apple iOS

1. En Apple Developer, habilitar **Sign in with Apple** para el App ID `com.coffeedial.app`, y actualizar el provisioning profile. Se necesita una membresía que permita esta capability.
2. Configurar el Signing Team en Xcode.
3. En la misma configuración local, establecer:

   ```xcconfig
   COFFEE_APPLE_SIGN_IN_ENABLED = YES
   COFFEE_APPLE_ENTITLEMENTS = CoffeeDial.entitlements
   ```

4. Probar en un iPhone con una cuenta Apple activa: consentimiento inicial, ocultar email, segundo acceso sin nombre/email, cancelar, cerrar sesión y revocar acceso desde ajustes de Apple. La comprobación de restauración se hace al iniciar la app; no hay sesión backend ni monitoreo continuo de revocaciones.

## Apple web / Android (pendiente)

Requiere Services ID asociado a un App ID con Sign in with Apple, dominio y retorno HTTPS registrados, además de un servidor que reciba `form_post`, intercambie el código y valide los tokens. La clave privada de Apple y el client secret generado deben permanecer en el servidor. El flujo debe correlacionar `state` y `nonce`, utilizar PKCE cuando lo soporte el proveedor y devolver a la app un código de un solo uso; nunca un email aceptado como prueba de identidad. Esto se implementará cuando se elija/configure ese servicio: no existe actualmente un fallback web de Apple para Android.

No hace falta Google Cloud hosting, Firebase Auth, Supabase ni analytics para los flujos nativos actuales. Google Cloud se usa solamente para registrar OAuth. Antes de habilitar sync, el backend debe verificar firma, issuer, audience, expiry y nonce de los tokens y emitir su propia sesión; un perfil local no autoriza acceso a datos remotos.

## Pruebas manuales tras activar

- Sin configuración: ambos botones responden con explicación, sin spinner persistente.
- Google Android: cuenta autorizada; cuenta nueva; teléfono sin cuentas; cancelación; red desconectada; client ID/SHA-1 equivocados.
- Google iOS: acceso web y retorno a app; cancelación; restauración; cerrar y reabrir sesión.
- Apple iOS: primer/segundo consentimiento; Hide My Email; cancelación; cierre; revocación y reinicio.
- En todos los casos: los shots locales permanecen intactos al entrar/salir/cambiar de proveedor.

## Documentación oficial

- https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation
- https://developers.google.com/identity/sign-in/ios/start-integrating
- https://developers.google.com/identity/sign-in/ios/sign-in
- https://developer.apple.com/documentation/signinwithapple/configuring-your-environment-for-sign-in-with-apple
- https://developer.apple.com/help/account/capabilities/configure-sign-in-with-apple-for-the-web/

## Verificación local — 3 de octubre de 2026

- `./gradlew ktlintCheck :shared:iosSimulatorArm64Test :shared:testAndroidHostTest :androidApp:assembleDebug --continue --console=plain`: OK; 24 tests JVM y 17 tests iOS (4 nuevos del puente de Cuenta). Se usó el JDK incluido con Android Studio.
- `xcodebuild -project iosApp/CoffeeDial.xcodeproj -scheme CoffeeDial -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath /tmp/coffee-dial-auth-derived CODE_SIGNING_ALLOWED=NO build`: OK.
- Instalación y lanzamiento en iPhone 18 Pro / iOS 27 Simulator: OK.
- El build conserva un warning del framework Kotlin/ICU por deployment target 16 frente a un objeto compilado para 18.5; no se verificó runtime en iOS 16.
- Login completo, ventanas de consentimiento y retorno OAuth: pendientes de credenciales/configuración y de las pruebas manuales listadas arriba. Un build correcto no prueba esos flujos externos.

## Proyecto Google configurado

Proyecto: `coffee-dial-510518`. Clientes Android Debug, iOS y Web creados el 3/10/2026. IDs públicos conectados en `gradle.properties` y `iosApp/Auth.xcconfig` (Debug y Release). No se usa ni guarda el secreto Web en la app. `Auth.local.xcconfig` es una sobreescritura opcional incluida automáticamente: ya no hace falta seleccionarla como base en Xcode. No copiar valores Google vacíos del ejemplo si solo se configura Apple. Apple permanece desactivado hasta finalizar la membresía y el provisioning.

El cliente Android registrado corresponde al SHA-1 debug local `AE:DA:48:00:9C:76:16:41:9F:7E:1C:5B:81:52:44:11:C1:B5:00:22`; para distribución hay que registrar el certificado de release/Play Signing.

Verificación con IDs reales: Android `assembleDebug` y build Xcode Simulator OK. El Info.plist compilado contiene GIDClientID y URL scheme correctos. Cuenta del propietario agregada y verificada en la lista de usuarios de prueba; login interactivo aún no verificado.

## Google web para Android

El fallback está implementado en Android y el servicio está desplegado en `https://auth-coffee.muralooo.win`. Sin Play Services se abre el navegador; Cuenta también ofrece acceso web manual. Cancelar el flujo nativo no dispara otro login.

Retorno autorizado de Google: `https://auth-coffee.muralooo.win/google/callback`. `GOOGLE_CLIENT_SECRET` está cargado como secreto protegido del Worker. Nunca incluirlo en Gradle, APK o Git.

Cada intento usa state, nonce, PKCE y cookie Secure/HttpOnly, y vence a los cinco minutos. El Worker valida la firma y claims del ID token de Google. La app recibe un código de un solo uso ligado al verificador que conserva localmente; vence como máximo a los 60 segundos y se consume atómicamente. No se persisten tokens Google ni se sincronizan shots. ALLOWED_EMAILS restringe inicialmente el acceso al propietario.

El 3/10/2026 se verificó HTTPS y doce pruebas unitarias del servicio. `/health` responde `ready` tras cargar el secreto. El login completo en Android sigue pendiente.
