// Fetch streaming preserves the Authorization header without putting tokens in URLs.
export async function readSse(response: Response, onEvent: (event: string, data: string) => void) {
  if (!response.body) throw new Error('Flux temps réel indisponible.')
  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  let event = 'message'
  let data: string[] = []
  try {
    while (true) {
      const chunk = await reader.read()
      if (chunk.done) break
      buffer += chunk.value
      if (buffer.length > 65536) throw new Error('Événement temps réel trop volumineux.')
      let newline: number
      while ((newline = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, newline).replace(/\r$/, '')
        buffer = buffer.slice(newline + 1)
        if (line === '') {
          if (data.length) onEvent(event, data.join('\n'))
          event = 'message'; data = []
        } else if (line.startsWith('event:')) event = line.slice(6).trimStart()
        else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
      }
    }
  } finally { await reader.cancel().catch(() => {}); reader.releaseLock() }
}
