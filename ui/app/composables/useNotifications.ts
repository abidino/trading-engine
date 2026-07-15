export interface AlertRecord {
  ticker: string
  action: string
  channel: string
  sentAt?: string | null
  status: string
}

export interface TriggeredAlert {
  ticker: string
  alertType: string
  triggeredOn?: string | null
  price: number
  level: number
  message: string
  triggeredAt?: string | null
}

export function useNotifications() {
  const api = useApi()

  const alerts = ref<AlertRecord[]>([])
  const triggered = ref<TriggeredAlert[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchAlerts() {
    loading.value = true
    error.value = null
    try {
      const [a, t] = await Promise.all([
        api.get<AlertRecord[]>('/notifications/alerts'),
        api.get<TriggeredAlert[]>('/notifications/alerts/triggered').catch(() => [] as TriggeredAlert[]),
      ])
      alerts.value = a
      triggered.value = t
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch alerts'
    } finally {
      loading.value = false
    }
  }

  return {
    alerts: readonly(alerts),
    triggered: readonly(triggered),
    loading: readonly(loading),
    error: readonly(error),
    fetchAlerts,
  }
}
