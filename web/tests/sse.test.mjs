import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import ts from 'typescript'

const source = await readFile(new URL('../src/lib/sse.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } }).outputText
const { readSse } = await import(`data:text/javascript;base64,${Buffer.from(compiled).toString('base64')}`)

test('parses split UTF-8, CRLF, comments and multiline event data', async () => {
  const bytes = new TextEncoder().encode(':heartbeat\r\n\r\nevent: sync\r\ndata: {"message":"créée",\r\ndata: "tasks":true}\r\n\r\n')
  const stream = new ReadableStream({ start(controller) {
    for (const byte of bytes) controller.enqueue(new Uint8Array([byte]))
    controller.close()
  } })
  const events = []
  await readSse(new Response(stream), (event, data) => events.push({ event, data: JSON.parse(data) }))
  assert.deepEqual(events, [{ event: 'sync', data: { message: 'créée', tasks: true } }])
})

test('discards an incomplete event on disconnect', async () => {
  const events = []
  await readSse(new Response('event: sync\ndata: incomplete'), (...event) => events.push(event))
  assert.deepEqual(events, [])
})
