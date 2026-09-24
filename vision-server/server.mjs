import http from 'node:http';
import { timingSafeEqual } from 'node:crypto';
import { pathToFileURL } from 'node:url';
import { createExtractor, readRoutes, VisionError } from './extractor.mjs';

export function createServer({ tokens, extract, now = Date.now }) {
  const limits = new Map();
  let active = 0;
  const send = (res, status, data) => { res.writeHead(status, { 'content-type': 'application/json', 'cache-control': 'no-store' }); res.end(JSON.stringify(data)); };
  const server = http.createServer(async (req, res) => {
    if (req.method === 'GET' && req.url === '/health') return send(res, 200, { status: 'ok' });
    if (req.method !== 'POST' || req.url !== '/v1/extract') return send(res, 404, { error: 'Not found.' });
    const provided = Buffer.from((req.headers.authorization || '').replace(/^Bearer /, ''));
    const tokenIndex = tokens.findIndex(token => { const expected = Buffer.from(token); return expected.length === provided.length && timingSafeEqual(expected, provided); });
    if (tokenIndex < 0) return send(res, 401, { error: 'Gateway access token is invalid.' });
    const limit = limits.get(tokenIndex) || { start: now(), count: 0 };
    if (now() - limit.start >= 3600000) { limit.start = now(); limit.count = 0; }
    limits.set(tokenIndex, limit);
    if (limit.count >= 60 || active >= 2) return send(res, 429, { error: 'Gateway is busy. Keep local results and try later.' });
    if (!req.headers['content-type']?.startsWith('application/json')) return send(res, 415, { error: 'JSON required.' });
    active++; limit.count++;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 85000);
    res.on('close', () => { if (!res.writableEnded) controller.abort(); });
    try {
      const chunks = []; let size = 0;
      for await (const chunk of req) {
        size += chunk.length;
        if (size > 6 * 1024 * 1024) throw new VisionError(413, 'Photo is too large.');
        chunks.push(chunk);
      }
      let body;
      try { body = JSON.parse(Buffer.concat(chunks).toString('utf8')); } catch { throw new VisionError(400, 'Invalid request.'); }
      if (body.consent !== true || body.mimeType !== 'image/jpeg' || typeof body.image !== 'string' || body.image.length > 5600000 || !/^[A-Za-z0-9+/]+={0,2}$/.test(body.image)) throw new VisionError(400, 'A JPEG image and explicit consent are required.');
      const bytes = Buffer.from(body.image, 'base64');
      if (bytes.length < 4 || bytes[0] !== 255 || bytes[1] !== 216 || bytes[2] !== 255) throw new VisionError(400, 'Invalid JPEG image.');
      const result = await extract({ image: body.image, mimeType: body.mimeType }, controller.signal);
      if (!res.destroyed) send(res, 200, result);
    } catch (error) {
      if (!res.destroyed) send(res, error instanceof VisionError ? error.status : 502, { error: error instanceof VisionError ? error.message : 'Vision request failed. Local results are unchanged.' });
    } finally { clearTimeout(timer); active--; }
  });
  server.requestTimeout = 90000;
  server.headersTimeout = 10000;
  return server;
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  if (process.env.GEMINI_PAID_CONFIRMED !== 'true') throw new Error('Use billing-enabled Gemini services for health documents; set GEMINI_PAID_CONFIRMED=true after checking your account.');
  const tokens = (process.env.MEDTRACK_ACCESS_TOKENS || '').split(',').map(t => t.trim()).filter(Boolean);
  if (!tokens.length || tokens.some(t => t.length < 32)) throw new Error('Set private MEDTRACK_ACCESS_TOKENS (at least 32 characters each).');
  const extract = createExtractor(readRoutes(process.env.GEMINI_ROUTES_JSON));
  createServer({ tokens, extract }).listen(Number(process.env.PORT || 8080), process.env.HOST || '127.0.0.1', () => console.log('MedTrack vision gateway ready. Request bodies and credentials are not logged.'));
}
