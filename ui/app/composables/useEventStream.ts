import type { EventType, ServerEvent } from '~/types'

export function useEventStream(types?: EventType[]) {
  const config = useRuntimeConfig()
  const events = ref<ServerEvent[]>([])
  const isConnected = ref(false)
  const lastEvent = ref<ServerEvent | null>(null)

  let eventSource: EventSource | null = null

  function connect() {
    // Disabled: Backend event stream endpoint not implemented
    return
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
