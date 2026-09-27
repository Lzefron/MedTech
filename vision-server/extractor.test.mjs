import test from 'node:test';
import assert from 'node:assert/strict';
import { createExtractor, readRoutes } from './extractor.mjs';
import { createServer } from './server.mjs';
const route = (project, key = 'a'.repeat(32)) => ({ project, key, model: 'gemini-3.8-flash' });
const data = { rawText: 'Hb 14', date: '2024-01-01', laboratory: '', title: '', rows: [{ name: 'Hb', value: '14', unit: 'g/dL', reference: '', kind: 'number', warning: '' }] };
const ok = (value = data) => new Response(JSON.stringify({ candidates: [{ finishReason: 'STOP', content: { parts: [{ text: JSON.stringify(value) }] } }] }), { status: 200 });
const image = { mimeType: 'image/jpeg', image: '/9j/AA==' };
test('REST request uses the JSON response format enum', async () => {
  const extract = createExtractor([route('one')], async (_, options) => {
    const body = JSON.parse(options.body);
    assert.equal(body.generationConfig.responseFormat.text.mimeType, 'APPLICATION_JSON');
    return ok();
  });
  await extract(image, new AbortController().signal);
});
test('all eight configured credentials are reachable during failover', async () => {
  const routes = readRoutes(JSON.stringify(Array.from({ length: 8 }, (_, i) => route('one', String(i).repeat(32)))));
  let calls = 0;
  const extract = createExtractor(routes, async () => ++calls < 8 ? new Response('', { status: 401 }) : ok());
  assert.equal((await extract(image, new AbortController().signal)).rows[0].value, '14');
  assert.equal(calls, 8);
  assert.throws(() => readRoutes(JSON.stringify([...routes, route('two')])));
});

test('cancellation stops failover before another provider receives the photo', async () => {
  const controller = new AbortController();
  let calls = 0;
  const extract = createExtractor([route('one'), route('two')], async () => {
    calls++; controller.abort(); return new Response('', { status: 503 });
  });
  await assert.rejects(extract(image, controller.signal), e => e.status === 504);
  assert.equal(calls, 1);
});
test('tries a backup credential after authentication failure', async () => {
  let calls = 0;
  const extract = createExtractor([route('one'), route('one', 'b'.repeat(32))], async () => ++calls === 1 ? new Response('', { status: 401 }) : ok());
  assert.equal((await extract(image, new AbortController().signal)).rows[0].value, '14');
  assert.equal(calls, 2);
});
test('quota cooldown skips every key in the same project', async () => {
  const keys = [];
  const extract = createExtractor([route('one'), route('one', 'b'.repeat(32)), route('two', 'c'.repeat(32))], async (_, options) => {
    keys.push(options.headers['x-goog-api-key']); return keys.length === 1 ? new Response('', { status: 429, headers: { 'retry-after': '3600' } }) : ok();
  });
  await extract(image, new AbortController().signal);
  await extract(image, new AbortController().signal);
  assert.deepEqual(keys, ['a'.repeat(32), 'c'.repeat(32), 'c'.repeat(32)]);
});
test('malformed and truncated output falls back instead of silently dropping rows', async () => {
  let calls = 0;
  const extract = createExtractor([route('one'), route('two')], async () => ++calls === 1 ? ok({ ...data, rows: [{ name: 'Hb' }] }) : ok());
  assert.equal((await extract(image, new AbortController().signal)).rows.length, 1);
  assert.equal(calls, 2);
});
test('exhausted providers return an actionable failure without credentials', async () => {
  const extract = createExtractor([route('one'), route('two')], async () => { throw new Error('secret'); });
  await assert.rejects(extract(image, new AbortController().signal), e => e.status === 503 && !e.message.includes('secret'));
});
test('bad request does not rotate credentials', async () => {
  let calls = 0;
  const extract = createExtractor([route('one'), route('two')], async () => { calls++; return new Response('', { status: 400 }); });
  await assert.rejects(extract(image, new AbortController().signal)); assert.equal(calls, 1);
});
test('route configuration rejects arbitrary destinations and invalid models', () => {
  assert.throws(() => readRoutes(JSON.stringify([{ project: 'one', key: 'a'.repeat(32), model: '../../elsewhere' }])));
});
test('gateway requires authentication and explicit consent before forwarding', async () => {
  let calls = 0;
  const server = createServer({ tokens: ['t'.repeat(32)], extract: async () => { calls++; return data; } });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}/v1/extract`;
  try {
    assert.equal((await fetch(url, { method: 'POST' })).status, 401);
    const headers = { 'content-type': 'application/json', authorization: `Bearer ${'t'.repeat(32)}` };
    assert.equal((await fetch(url, { method: 'POST', headers, body: JSON.stringify(image) })).status, 400);
    assert.equal(calls, 0);
    assert.equal((await fetch(url, { method: 'POST', headers, body: JSON.stringify({ ...image, consent: true }) })).status, 200);
    assert.equal(calls, 1);
  } finally { await new Promise(resolve => server.close(resolve)); }
});
