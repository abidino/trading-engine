import type { EventType, ServerEvent } from '~/types'

export function useEventStream(types?: EventType[]) {
  const config = useRuntimeConfig()
  const events = ref<ServerEvent[]>([])
  const isConnected = ref(false)
  const lastEvent = ref<ServerEvent | null>(null)

  let eventSource: EventSource | null = null

  function connect() {
    if (import.meta.server) return

    const params = types?.length ? `?types=${types.join(',')}` : ''
    const url = `${config.public.apiBaseUrl}/api/v1/events/stream${params}`

    eventSource = new EventSource(url)

    eventSource.onopen = () => {
      isConnected.value = true
    }

    eventSource.onmessage = (event) => {
      try {
        const parsed: ServerEvent = JSON.parse(event.data)
        lastEvent.value = parsed
        events.value = [parsed, ...events.value.slice(0, 99)]
      } catch {
        // skip malformed events
      }
    }

    eventSource.onerror = () => {
      isConnected.value = false
      eventSource?.close()

      // reconnect after 5 seconds
      setTimeout(() => {
        connect()
      }, 5000)
    }
  }

  function disconnect() {
    eventSource?.close()
    eventSource = null
    isConnected.value = false
  }

  function clearEvents() {
    events.value = []
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    events: readonly(events),
    lastEvent: readonly(lastEvent),
    isConnected: readonly(isConnected),
    connect,
    disconnect,
    clearEvents,
  }
}
