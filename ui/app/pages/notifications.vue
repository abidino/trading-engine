<script setup lang="ts">
const { alerts, triggered, loading, error, fetchAlerts } = useNotifications()

onMounted(() => {
  fetchAlerts()
})

const formatTime = (iso?: string | null) =>
  iso ? new Date(iso).toLocaleString() : '—'

const alertTypeLabel = (t: string) => {
  switch (t) {
    case 'STOP_LOSS': return 'Stop-loss kırıldı'
    case 'TAKE_PROFIT': return 'Hedefe ulaştı'
    case 'ENTRY_ZONE': return 'Giriş bölgesi'
    case 'PORTFOLIO_DROP': return 'Portföy düşüşü'
    default: return t
  }
}

const alertTypeColor = (t: string) => {
  switch (t) {
    case 'STOP_LOSS': return 'bg-red-900 text-red-300'
    case 'TAKE_PROFIT': return 'bg-green-900 text-green-300'
    case 'ENTRY_ZONE': return 'bg-blue-900 text-blue-300'
    case 'PORTFOLIO_DROP': return 'bg-amber-900 text-amber-300'
    default: return 'bg-gray-800 text-gray-400'
  }
}

const statusColor = (status: string) => {
  switch (status) {
    case 'SENT': return 'bg-green-900 text-green-300'
    case 'FAILED': return 'bg-red-900 text-red-300'
    case 'SKIPPED': return 'bg-gray-800 text-gray-400'
    default: return 'bg-gray-800 text-gray-400'
  }
}

const actionColor = (action: string) => {
  switch (action) {
    case 'BUY': return 'badge-green'
    case 'SELL': return 'badge-red'
    case 'HOLD': return 'badge-yellow'
    default: return 'badge-gray'
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Notifications</h1>
        <p class="text-sm text-gray-400">Alerts dispatched for trading decisions</p>
      </div>
      <button class="btn-secondary" :disabled="loading" @click="fetchAlerts">
        <Icon name="heroicons:arrow-path" class="mr-1.5 h-4 w-4" :class="loading ? 'animate-spin' : ''" />
        Refresh
      </button>
    </div>

    <!-- Error -->
    <div v-if="error" class="card border-red-800">
      <p class="text-red-400">{{ error }}</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-10">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
    </div>

    <!-- Proactive threshold alerts (price hit stop / target / entry, portfolio drop) -->
    <div v-if="!loading && triggered.length" class="card">
      <h3 class="card-title mb-3">Seviye Uyarıları</h3>
      <ul class="space-y-2">
        <li
          v-for="(t, i) in triggered"
          :key="i"
          class="flex items-start gap-3 rounded border border-gray-800 bg-gray-900/40 px-3 py-2"
        >
          <span class="rounded px-1.5 py-0.5 text-[10px] font-medium" :class="alertTypeColor(t.alertType)">
            {{ alertTypeLabel(t.alertType) }}
          </span>
          <div class="min-w-0 flex-1">
            <p class="text-sm text-gray-200">{{ t.message }}</p>
            <p class="text-xs text-gray-500">{{ formatTime(t.triggeredAt) }}</p>
          </div>
        </li>
      </ul>
    </div>

    <!-- Alerts Table -->
    <div v-else class="card overflow-hidden p-0">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-gray-700 bg-gray-800/50">
            <tr>
              <th class="px-4 py-3 font-medium text-gray-400">Ticker</th>
              <th class="px-4 py-3 font-medium text-gray-400">Action</th>
              <th class="px-4 py-3 font-medium text-gray-400">Channel</th>
              <th class="px-4 py-3 font-medium text-gray-400">Status</th>
              <th class="px-4 py-3 font-medium text-gray-400">Sent At</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-800">
            <tr v-for="(a, i) in alerts" :key="i" class="hover:bg-gray-800/30">
              <td class="px-4 py-3 font-medium text-white">{{ a.ticker }}</td>
              <td class="px-4 py-3">
                <span :class="actionColor(a.action)" class="badge">{{ a.action }}</span>
              </td>
              <td class="px-4 py-3 text-gray-300">{{ a.channel }}</td>
              <td class="px-4 py-3">
                <span class="rounded px-1.5 py-0.5 text-[10px] font-medium" :class="statusColor(a.status)">
                  {{ a.status }}
                </span>
              </td>
              <td class="px-4 py-3 text-gray-400">{{ formatTime(a.sentAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="!alerts.length" class="py-12 text-center">
        <Icon name="heroicons:bell-slash" class="mx-auto h-10 w-10 text-gray-600" />
        <p class="mt-2 text-sm text-gray-500">No alerts yet</p>
      </div>
    </div>
  </div>
</template>
