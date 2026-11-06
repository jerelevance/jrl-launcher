// node drive.js preview | stills | loop <kind> <cover|inner>
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
const SIZES = { cover: [1080, 2520], inner: [1968, 2184] };
const THEMES = ['jrl', 'paper', 'pastel', 'bauhaus', 'mono', 'night', 'marathon', 'riso', 'phosphor', 'ocean', 'candy', 'forest', 'acid', 'boot'];
(async () => {
  const [mode, a1, a2] = process.argv.slice(2);
  const b = await chromium.launch({ executablePath: '/usr/bin/google-chrome', args: ['--no-sandbox', '--allow-file-access-from-files'] });
  const p = await (await b.newContext({ viewport: { width: 400, height: 400 } })).newPage();
  p.on('pageerror', e => console.log('PAGEERR', e.message)); p.on('console', m => { if (m.type() === 'error') console.log('CONSOLE', m.text()); });
  await p.goto('file://' + path.join(__dirname, 'render.html'));
  const NJ = !!process.env.NJ, OUT = NJ ? 'out-nj' : 'out'; await p.evaluate(v => R.nj(v), NJ);
  await p.evaluate(() => Promise.all(['700 20px "JRL Archivo"', '400 20px "JRL Mono"', '700 20px "JRL Mono"'].map(f => document.fonts.load(f))));
  const grab = async (file, type) => { const d = await p.evaluate(t => document.getElementById('c').toDataURL(t, .95), type || 'image/png'); fs.writeFileSync(file, Buffer.from(d.split(',')[1], 'base64')); };
  if (mode === 'preview') {
    fs.mkdirSync('preview', { recursive: true });
    if (NJ) { for (const [k, t] of [['glitch', 0.3], ['breathe', 1.1], ['breathe', 6.0]]) { await p.evaluate(([k, t]) => { R.size(1080, 2520); R.frame(k, t); }, [k, t]); await grab('preview/nj-' + k + '-' + t + '.png'); } await p.evaluate(() => { R.size(1968, 2184); R.still('bauhaus'); }); await grab('preview/nj-still-inner.png'); await p.evaluate(() => { R.size(1080, 2520); R.still('night'); }); await grab('preview/nj-still-cover.png'); await b.close(); return; }
    for (const [k, t] of [['field', 2.2], ['field', 5.0], ['field', 9.3], ['glitch', 0.5], ['glitch', 1.5], ['glitch', 3.6], ['glitch', 7.6], ['breathe', 1.1], ['breathe', 6.0]]) { await p.evaluate(([k, t]) => { R.size(1080, 2520); R.frame(k, t); }, [k, t]); await grab('preview/' + k + '-' + t + '.png'); }
    for (const th of ['jrl', 'bauhaus', 'boot']) { await p.evaluate(th => { R.size(1080, 2520); R.still(th); }, th); await grab('preview/still-' + th + '.png'); }
    await p.evaluate(() => { R.size(1968, 2184); R.still('riso'); }); await grab('preview/still-riso-inner.png');
  } else if (mode === 'stills') {
    for (const sz of Object.keys(SIZES)) { const dir = path.join(OUT, sz, 'stills'); fs.mkdirSync(dir, { recursive: true });
      for (const th of THEMES.filter(t => !(NJ && t === 'boot'))) { await p.evaluate(([w, h, th]) => { R.size(w, h); R.still(th); }, [...SIZES[sz], th]); await grab(path.join(dir, 'jrl-' + th + '.png')); } }
  } else if (mode === 'loop') {
    const [w, h] = SIZES[a2], dir = path.join('frames', (NJ ? 'nj-' : '') + a1 + '-' + a2); fs.rmSync(dir, { recursive: true, force: true }); fs.mkdirSync(dir, { recursive: true });
    const N = 12 * 24;
    for (let i = 0; i < N; i++) { await p.evaluate(([w, h, k, t]) => { if (R._w !== w) { R.size(w, h); R._w = w; } R.frame(k, t); }, [w, h, a1, i / 24]); await grab(path.join(dir, String(i).padStart(4, '0') + '.jpg'), 'image/jpeg'); }
  }
  await b.close();
})();
