import { afterEach, expect, it, vi } from 'vitest'

afterEach(() => { vi.unstubAllGlobals(); vi.resetModules() })

it('loads the approved recording on unlock and plays it again without downloading it again', async () => {
  const source = { connect: vi.fn(), start: vi.fn(), disconnect: vi.fn() }
  const buffer = {}
  const context = { state: 'running', destination: {}, resume: vi.fn(), decodeAudioData: vi.fn().mockResolvedValue(buffer), createBufferSource: vi.fn().mockReturnValue(source) }
  vi.stubGlobal('AudioContext', vi.fn(function () { return context }))
  const fetchAudio = vi.fn().mockResolvedValue({ ok: true, arrayBuffer: async () => new ArrayBuffer(8) })
  vi.stubGlobal('fetch', fetchAudio)
  const sound = await import('./reminderSound')
  expect(sound.playReminderSound()).toBe(false)
  expect(await sound.unlockReminderSound()).toBe(true)
  expect(sound.playReminderSound()).toBe(true)
  expect(source.buffer).toBe(buffer)
  expect(source.start).toHaveBeenCalledOnce()
  await sound.unlockReminderSound()
  expect(fetchAudio).toHaveBeenCalledOnce()
  source.onended()
  expect(source.disconnect).toHaveBeenCalledOnce()
})

it('handles audio download failure and can retry on the next user gesture', async () => {
  const context = { state: 'running', resume: vi.fn(), decodeAudioData: vi.fn().mockResolvedValue({}) }
  vi.stubGlobal('AudioContext', vi.fn(function () { return context }))
  vi.stubGlobal('fetch', vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ ok: true, arrayBuffer: async () => new ArrayBuffer(8) }))
  const sound = await import('./reminderSound')
  expect(await sound.unlockReminderSound()).toBe(false)
  expect(sound.playReminderSound()).toBe(false)
  expect(await sound.unlockReminderSound()).toBe(true)
})
