<script setup lang="ts">
const {
  items, loading, error,
  fetchWatchlist, addToWatchlist, removeFromWatchlist,
  updateTargetPrice, approveItem, analyzeItem,
} = useWatchlist()

onMounted(() => {
  fetchWatchlist()
})

const showAddForm = ref(false)
const newTicker = ref('')
const busyId = ref<string | null>(null)
const analyzingTicker = ref<string | null>(null)

const formatPrice = (value?: string | null) => {
  if (value == null || value === '') return '—'
  const n = Number(value)
  return Number.isNaN(n)
    ? value
    : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n)
}

const formatDate = (iso?: string) => (iso ? new Date(iso).toLocaleDateString() : '—')

async function handleSubmit() {
  if (!newTicker.value.trim()) return
  await addToWatchlist(newTicker.value.trim())
  newTicker.value = ''
  showAddForm.value = false
}

async function handleRemove(id: string) {
  busyId.value = id
  await removeFromWatchlist(id)
  busyId.value = null
}

async function handleApprove(id: string) {
  busyId.value = id
  await approveItem(id)
  busyId.value = null
}

async function handleAnalyze(ticker: string) {
  analyzingTicker.value = ticker
  await analyzeItem(ticker)
  analyzingTicker.value = null
}

async function handleSetTarget(id: string) {
  const input = window.prompt('Target price:')
  if (!input) return
  const price = Number(input)
  if (Number.isNaN(price)) return
  busyId.value = id
  await updateTargetPrice(id, price)
  busyId.value = null
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Watchlist</h1>
        <p class="text-sm text-gray-400">Track tickers you're interested in</p>
      </div>
      <button class="btn-primary" @click="showAddForm = !showAddForm">
        <Icon name="heroicons:plus" class="mr-1.5 h-4 w-4" />
        Add Ticker
      </button>
    </div>

    <!-- Add Form -->
    <div v-if="showAddForm" class="card">
      <h3 class="card-title mb-4">Add to Watchlist</h3>
      <form class="flex items-end gap-3" @submit.prevent="handleSubmit">
        <div class="flex-1">
          <label class="mb-1 block text-xs text-gray-400">Ticker *</label>
          <input
            v-model="newTicker"
            type="text"
            placeholder="TSLA"
            class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
            required
          >
        </div>
        <button type="submit" class="btn-primary" :disabled="!newTicker.trim()">
          <Icon name="heroicons:plus" class="mr-1.5 h-4 w-4" />
          Add
        </button>
        <button type="button" class="btn-secondary" @click="showAddForm = false">Cancel</button>
      </form>
    </div>

    <!-- Error -->
    <div v-if="error" class="card border-red-800">
      <p class="text-red-400">{{ error }}</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-10">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
    </div>

    <!-- Watchlist Table -->
    <div v-else class="card">
      <div class="card-header">
        <h3 class="card-title">Watching</h3>
        <span class="badge-blue">{{ items.length }}</span>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-800 text-left text-gray-400">
              <th class="pb-3 font-medium">Ticker</th>
              <th class="pb-3 font-medium">Added</th>
              <th class="pb-3 font-medium">Target Price</th>
              <th class="pb-3 font-medium">Stop Loss</th>
              <th class="pb-3 font-medium">Notes</th>
              <th class="pb-3 font-medium">Status</th>
              <th class="pb-3 font-medium" />
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-800">
            <tr v-for="item in items" :key="item.id">
              <td class="py-3 font-medium text-white">{{ item.ticker }}</td>
              <td class="py-3 text-gray-400">{{ formatDate(item.addedAt) }}</td>
              <td class="py-3 text-green-400">{{ formatPrice(item.targetPrice) }}</td>
              <td class="py-3 text-red-400">{{ formatPrice(item.stopLoss) }}</td>
              <td class="py-3 text-gray-400">{{ item.notes || '—' }}</td>
              <td class="py-3">
                <span
                  class="rounded px-1.5 py-0.5 text-[10px] font-medium"
                  :class="item.approved ? 'bg-green-900 text-green-300' : 'bg-gray-800 text-gray-500'"
                >
                  {{ item.approved ? 'APPROVED' : 'PENDING' }}
                </span>
              </td>
              <td class="py-3">
                <div class="flex items-center gap-2">
                  <button
                    class="text-gray-500 hover:text-blue-400"
                    title="Set target price"
                    :disabled="busyId === item.id"
                    @click="handleSetTarget(item.id)"
                  >
                    <Icon name="heroicons:currency-dollar" class="h-4 w-4" />
                  </button>
                  <button
                    class="text-gray-500 hover:text-yellow-400"
                    title="Request analysis"
                    :disabled="analyzingTicker === item.ticker"
                    @click="handleAnalyze(item.ticker)"
                  >
                    <Icon name="heroicons:sparkles" class="h-4 w-4" />
                  </button>
                  <button
                    v-if="!item.approved"
                    class="text-gray-500 hover:text-green-400"
                    title="Approve"
                    :disabled="busyId === item.id"
                    @click="handleApprove(item.id)"
                  >
                    <Icon name="heroicons:check-circle" class="h-4 w-4" />
                  </button>
                  <button
                    class="text-gray-500 hover:text-red-400"
                    title="Remove"
                    :disabled="busyId === item.id"
                    @click="handleRemove(item.id)"
                  >
                    <Icon name="heroicons:trash" class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!items.length" class="py-6 text-center text-sm text-gray-500">
          No watchlist items yet. Add a ticker to start tracking.
        </p>
      </div>
    </div>
  </div>
</template>
