/* Data stays on this origin. Schema upgrades must preserve the document and use explicit migrations. */
(() => {
  const shellCopy = {
    en: {
      notice: 'Data stays in this browser. Sign in or use Backup to move it to your phone or keep a copy.',
      loading: 'Loading Coffee Dial…',
      failed: 'Could not open your data. Try an up-to-date browser with storage enabled. Nothing was deleted. Reload to retry.'
    },
    es: {
      notice: 'Datos guardados en este navegador. Logueate o usa el Backup para pasarlos al teléfono o conservar una copia.',
      loading: 'Cargando Coffee Dial…',
      failed: 'No pudimos abrir tus datos. Probá un navegador actualizado con almacenamiento habilitado. No se borró ningún dato. Recargá para reintentar.'
    }
  };

  // Same rules as AppLanguage: ENGLISH/en and SPANISH/es win; anything else follows the browser.
  function shellEnglish(forced) {
    if (forced === true || forced === false) return forced;
    let stored = '';
    try { stored = (localStorage.getItem('coffee_language') || '').toLowerCase(); } catch (e) {}
    if (stored === 'english' || stored === 'en') return true;
    if (stored === 'spanish' || stored === 'es') return false;
    let nav = '';
    try { nav = (navigator.language || '').toLowerCase(); } catch (e) {}
    return nav.indexOf('en') === 0;
  }

  globalThis.coffeeApplyShell = function (forced) {
    const english = shellEnglish(forced);
    const copy = english ? shellCopy.en : shellCopy.es;
    globalThis.coffeeShellCopy = copy;
    if (typeof document === 'undefined') return;
    try { document.documentElement.lang = english ? 'en' : 'es'; } catch (e) {}
    const notice = document.getElementById('web-notice');
    if (notice && notice.textContent !== copy.notice) {
      const visible = notice.style.display !== 'none';
      notice.textContent = copy.notice;
      if (visible && typeof requestAnimationFrame === 'function') {
        requestAnimationFrame(() => window.dispatchEvent(new Event('resize')));
      }
    }
    const loading = document.getElementById('loading');
    if (loading && !loading.hidden && loading.dataset.mode !== 'error') loading.textContent = copy.loading;
  };

  function applyPageTheme() {
    if (typeof document === 'undefined' || typeof localStorage === 'undefined') return;
    try {
      const mode = localStorage.getItem('coffee_theme');
      const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      const dark = mode === 'DARK' || (mode !== 'LIGHT' && systemDark);
      if (!dark) return;
      const root = document.documentElement;
      root.style.setProperty('--page-bg', '#1c1612');
      root.style.setProperty('--page-fg', '#f6efe6');
      root.style.setProperty('--notice-bg', '#3a2c22');
      root.style.setProperty('color-scheme', 'dark');
      const meta = document.querySelector('meta[name="theme-color"]');
      if (meta) meta.setAttribute('content', '#1c1612');
    } catch (e) {}
  }

  let database;
  function open() {
    if (!database) database = new Promise((resolve, reject) => {
      const request = indexedDB.open('coffee-dial', 1);
      request.onupgradeneeded = () => request.result.createObjectStore('documents');
      request.onerror = () => reject(request.error);
      request.onblocked = () => reject(new Error('Close other Coffee Dial tabs and retry'));
      request.onsuccess = () => {
        request.result.onversionchange = () => { request.result.close(); database = null; };
        resolve(request.result);
      };
    }).catch(error => { database = null; throw error; });
    return database;
  }
  globalThis.CoffeeBrowser = {
    async read() {
      const db = await open();
      return new Promise((resolve, reject) => {
        const tx = db.transaction('documents', 'readonly');
        const request = tx.objectStore('documents').get('snapshot');
        tx.oncomplete = () => resolve(request.result ?? '');
        tx.onabort = () => reject(tx.error);
        tx.onerror = () => reject(tx.error);
      });
    },
    async write(expected, next) {
      const db = await open();
      return new Promise((resolve, reject) => {
        const tx = db.transaction('documents', 'readwrite');
        const store = tx.objectStore('documents');
        const request = store.get('snapshot');
        let result = 'conflict';
        request.onsuccess = () => {
          if ((request.result ?? '') === expected) { store.put(next, 'snapshot'); result = 'ok'; }
        };
        tx.oncomplete = () => resolve(result);
        tx.onabort = () => reject(tx.error ?? new Error('Storage failed'));
        tx.onerror = () => reject(tx.error);
      });
    },
    download(text) {
      const url = URL.createObjectURL(new Blob([text], { type: 'application/json' }));
      const link = document.createElement('a'); link.href = url;
      link.download = `coffee-dial-${new Date().toISOString().slice(0,10)}.json`;
      document.body.append(link); link.click(); link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    },
    pick(done) {
      const input = document.createElement('input'); input.type = 'file'; input.accept = '.json,application/json';
      input.hidden = true; document.body.append(input);
      let finished = false;
      const finish = (text, error) => { if (!finished) { finished = true; input.remove(); done(text, error); } };
      input.oncancel = () => finish('', '');
      input.onchange = async () => {
        const file = input.files?.[0];
        if (!file) return finish('', '');
        if (file.size > 10 * 1024 * 1024) return finish('', 'i18n.backup_size');
        try { finish(await file.text(), ''); } catch { finish('', 'i18n.could_not_read_backup'); }
      };
      input.click();
    },
    pickPhoto(done) {
      const input = document.createElement('input'); input.type = 'file'; input.accept = 'image/*';
      input.hidden = true; document.body.append(input);
      let finished = false;
      const finish = (data = '', error = '') => { if (!finished) { finished = true; input.remove(); done(data, error); } };
      input.oncancel = () => finish();
      input.onchange = async () => {
        const file = input.files?.[0];
        if (!file) return finish();
        if (file.size > 20 * 1024 * 1024) return finish('', 'i18n.photo_pick_limit');
        let image;
        try {
          image = await createImageBitmap(file);
          const scale = Math.min(1, 640 / Math.max(image.width, image.height));
          const canvas = document.createElement('canvas');
          canvas.width = Math.max(1, Math.round(image.width * scale)); canvas.height = Math.max(1, Math.round(image.height * scale));
          const ctx = canvas.getContext('2d'); ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
          let data;
          for (const quality of [0.75, 0.6, 0.45, 0.3]) {
            data = canvas.toDataURL('image/jpeg', quality).split(',')[1];
            if (data.length <= 136532) return finish(data);
          }
          finish('', 'i18n.could_not_prepare_photo');
        } catch { finish('', 'i18n.could_not_read_photo'); }
        finally { image?.close(); }
      };
      input.click();
    },
    ready() { document.getElementById('loading').hidden = true; },
    failed() {
      const loading = document.getElementById('loading');
      loading.hidden = false;
      loading.dataset.mode = 'error';
      coffeeApplyShell();
      loading.textContent = coffeeShellCopy.failed;
    }
  };
  if (typeof document !== 'undefined' && document.getElementById && document.getElementById('loading')) {
    applyPageTheme();
    coffeeApplyShell();
  }
})();
