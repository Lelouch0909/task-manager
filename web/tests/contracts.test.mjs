import { test } from 'node:test'
import assert from 'node:assert/strict'
import { loadSource } from './load-source.mjs'
const { taskSchema, taskPageSchema, loginSchema } = await loadSource('../src/lib/contracts.ts')
test('rejects malformed backend contracts before rendering', () => {
  assert.equal(taskSchema.safeParse({ id: '1', title: 'x', status: 'UNKNOWN' }).success, false)
  assert.equal(taskPageSchema.safeParse({ items: [], totalElements: '20' }).success, false)
  assert.equal(loginSchema.safeParse({ accessToken: null }).success, false)
})
test('accepts nullable descriptions and real pagination', () => {
  const task = { id: '1', title: 'x', description: null, status: 'TODO', createdAt: '2026-01-01', updatedAt: '2026-01-01' }
  assert.equal(taskPageSchema.parse({ items: [task], page: 0, size: 12, totalElements: 1, totalPages: 1 }).items[0].description, null)
})
