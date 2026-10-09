/* Data stays on this origin. Schema upgrades must preserve the document and use explicit migrations. */
(() => {
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
        if (file.size > 10 * 1024 * 1024) return finish('', 'El backup supera el límite de 10 MB.');
        try { finish(await file.text(), ''); } catch { finish('', 'No se pudo leer el archivo.'); }
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
        if (file.size > 20 * 1024 * 1024) return finish('', 'Elegí una imagen de hasta 20 MB.');
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
          finish('', 'No se pudo reducir la foto. Elegí otra imagen.');
        } catch { finish('', 'No se pudo leer la foto. Probá con JPEG o PNG.'); }
        finally { image?.close(); }
      };
      input.click();
    },
    ready() { document.getElementById('loading').hidden = true; },
    failed() {
      document.getElementById('loading').textContent = 'No pudimos abrir tus datos. Probá un navegador actualizado con almacenamiento habilitado. No se borró ningún dato. Recargá para reintentar.';
    }
  };
})();
