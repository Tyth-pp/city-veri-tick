import soundUrl from '../assets/audio/reminder-three-notes.wav?url'

let context, buffer, loading

function loadSound() {
  if (buffer) return Promise.resolve(buffer)
  if (!loading) {
    loading = (async () => {
      const response = await fetch(soundUrl)
      if (!response.ok) throw new Error('Reminder audio unavailable')
      buffer = await context.decodeAudioData(await response.arrayBuffer())
      return buffer
    })().finally(() => { loading = undefined })
  }
  return loading
}
export async function unlockReminderSound() {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext
    if (!AudioContext) return false
    context ||= new AudioContext()
    await context.resume()
    await loadSound()
    return context.state === 'running'
  } catch { return false }
}
export function playReminderSound() {
  if (!context || context.state !== 'running' || !buffer) return false
  try {
    const source = context.createBufferSource()
    source.buffer = buffer
    source.connect(context.destination)
    source.onended = () => source.disconnect()
    source.start()
    return true
  } catch { return false }
}
