import { readFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { pathToFileURL, fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import ts from 'typescript'

const require = createRequire(import.meta.url)
let run = 0
// Exercise the actual TypeScript modules under Node, without a browser or build output.
export async function loadSource(relative, { mocks = {}, env = {} } = {}) {
  const cache = new Map()
  const id = run++
  async function compile(filename) {
    if (cache.has(filename)) return cache.get(filename)
    let source = await readFile(filename, 'utf8')
    source = source.replaceAll('import.meta.env', JSON.stringify(env))
    let js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } }).outputText
    const imports = [...js.matchAll(/from\s+['"]([^'"]+)['"]/g)]
    for (const match of imports) {
      const specifier = match[1]
      let url
      if (mocks[specifier]) url = 'data:text/javascript;base64,' + Buffer.from(mocks[specifier] + '\n// test instance ' + id).toString('base64')
      else if (specifier.startsWith('@/')) url = await compile(fileURLToPath(new URL('../src/' + specifier.slice(2) + '.ts', import.meta.url)))
      else if (specifier.startsWith('.')) url = await compile(resolve(dirname(filename), specifier + (specifier.endsWith('.ts') ? '' : '.ts')))
      else url = pathToFileURL(require.resolve(specifier)).href
      js = js.replace(match[0], 'from ' + JSON.stringify(url))
    }
    const url = 'data:text/javascript;base64,' + Buffer.from(js + '\n// test instance ' + id).toString('base64')
    cache.set(filename, url)
    return url
  }
  return import(await compile(fileURLToPath(new URL(relative, import.meta.url))))
}
