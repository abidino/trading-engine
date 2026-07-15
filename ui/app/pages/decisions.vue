<script setup lang="ts">
const {
  decisions, accuracy, total, page, pageSize, tickerFilter, actionFilter,
  loading, error, totalPages, fetchDecisions, fetchAccuracy, nextPage, prevPage, applyFilters
} = useDecisions()

onMounted(() => {
  fetchDecisions()
  fetchAccuracy()
})

const fmtPct = (v?: number) => v == null ? '—' : `${(Number(v) * 100).toFixed(0)}%`

const actionColor = (action: string) => {
  switch (action) {
    case 'BUY': return 'badge-green'
    case 'SELL': return 'badge-red'
    case 'HOLD': return 'badge-yellow'
    default: return 'badge-gray'
  }
}

const formatTime = (iso?: string) =>
  iso ? new Date(iso).toLocaleString() : '—'

const formatConfidence = (confidence: number) =>
  `${(Number(confidence) * 100).toFixed(0)}%`

const outcomeColor = (outcome: string) => {
  switch (outcome) {
    case 'CORRECT': return 'text-green-400'
    case 'INCORRECT': return 'text-red-400'
    default: return 'text-gray-500'
  }
}

const selectedDecision = ref<(typeof decisions.value)[number] | null>(null)
const openDetail = (d: (typeof decisions.value)[number]) => {
  selectedDecision.value = d
}
const closeDetail = () => {
  selectedDecision.value = null
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-white">Decisions</h1>
      <p class="text-sm text-gray-400">All AI trading decisions — paginated and filterable</p>
    </div>

    <!-- Accuracy summary (evidence-based hit-rate) -->
    <div v-if="accuracy" class="card">
      <h3 class="card-title mb-3">İsabet Oranı</h3>
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div>
          <p class="text-xs text-gray-500">Hit-rate</p>
          <p class="text-xl font-bold text-green-400">{{ fmtPct(accuracy.hitRate) }}</p>
        </div>
        <div>
          <p class="text-xs text-gray-500">Doğrulandı</p>
          <p class="text-lg font-semibold text-green-300">{{ accuracy.validated }}</p>
        </div>
        <div>
          <p class="text-xs text-gray-500">Yanlış çıktı</p>
          <p class="text-lg font-semibold text-red-300">{{ accuracy.invalidated }}</p>
        </div>
        <div>
          <p class="text-xs text-gray-500">Bekleyen</p>
          <p class="text-lg font-semibold text-gray-400">{{ accuracy.pending }}</p>
        </div>
      </div>
      <div v-if="accuracy.byAction.length" class="mt-3 flex flex-wrap gap-2">
        <span
          v-for="b in accuracy.byAction"
          :key="b.key"
          class="rounded bg-gray-800 px-2 py-1 text-xs text-gray-300"
        >
          {{ b.key }}: {{ fmtPct(b.hitRate) }} ({{ b.validated }}/{{ b.validated + b.invalidated }})
        </span>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <form class="flex flex-wrap items-end gap-4" @submit.prevent="applyFilters">
        <div class="flex-1 min-w-[180px]">
          <label class="mb-1 block text-xs font-medium text-gray-400">Ticker</label>
          <input
            v-model="tickerFilter"
            type="text"
            placeholder="e.g. NVDA, AAPL"
            class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
          />
        </div>
        <div class="min-w-[140px]">
          <label class="mb-1 block text-xs font-medium text-gray-400">Action</label>
          <select
            v-model="actionFilter"
            class="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-white focus:border-blue-500 focus:outline-none"
          >
            <option value="">All</option>
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
            <option value="HOLD">HOLD</option>
            <option value="WAIT">WAIT</option>
          </select>
        </div>
        <button type="submit" class="btn-primary">
          <Icon name="heroicons:funnel" class="mr-1.5 h-4 w-4" />
          Filter
        </button>
      </form>
    </div>

    <!-- Error -->
    <div v-if="error" class="rounded-lg bg-red-900/20 p-4">
      <p class="text-sm text-red-400">{{ error }}</p>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden p-0">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-gray-700 bg-gray-800/50">
            <tr>
              <th class="px-4 py-3 font-medium text-gray-400">Ticker</th>
              <th class="px-4 py-3 font-medium text-gray-400">Type</th>
              <th class="px-4 py-3 font-medium text-gray-400">Action</th>
              <th class="px-4 py-3 font-medium text-gray-400">Confidence</th>
              <th class="px-4 py-3 font-medium text-gray-400">Reasoning</th>
              <th class="px-4 py-3 font-medium text-gray-400">Outcome</th>
              <th class="px-4 py-3 font-medium text-gray-400">Date</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-800">
            <tr
              v-for="d in decisions"
              :key="d.id"
              class="cursor-pointer hover:bg-gray-800/30"
              @click="openDetail(d)"
            >
              <td class="px-4 py-3 font-medium text-white">{{ d.ticker }}</td>
              <td class="px-4 py-3 text-gray-400">{{ d.decisionType }}</td>
              <td class="px-4 py-3">
                <span :class="actionColor(d.action)" class="badge">{{ d.action }}</span>
              </td>
              <td class="px-4 py-3 text-gray-300">{{ formatConfidence(d.confidence) }}</td>
              <td class="px-4 py-3 text-gray-300">
                <div class="flex max-w-xs items-center gap-2">
                  <span class="line-clamp-2">{{ d.reasoning || '—' }}</span>
                  <Icon
                    v-if="d.reasoning"
                    name="heroicons:document-magnifying-glass"
                    class="h-4 w-4 shrink-0 text-gray-500"
                  />
                </div>
              </td>
              <td class="px-4 py-3" :class="outcomeColor(d.outcome)">{{ d.outcome }}</td>
              <td class="px-4 py-3 text-gray-400">{{ formatTime(d.decidedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Empty State -->
      <div v-if="!loading && !decisions.length" class="py-12 text-center">
        <Icon name="heroicons:document-magnifying-glass" class="mx-auto h-10 w-10 text-gray-600" />
        <p class="mt-2 text-sm text-gray-500">No decisions found</p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="total > 0" class="flex items-center justify-between">
      <p class="text-sm text-gray-400">
        Showing {{ (page - 1) * pageSize + 1 }}–{{ Math.min(page * pageSize, total) }} of {{ total }}
      </p>
      <div class="flex items-center gap-2">
        <button
          class="btn-secondary text-xs"
          :disabled="page <= 1"
          @click="prevPage"
        >
          <Icon name="heroicons:chevron-left" class="h-4 w-4" />
          Previous
        </button>
        <span class="text-sm text-gray-400">Page {{ page }} / {{ totalPages }}</span>
        <button
          class="btn-secondary text-xs"
          :disabled="page >= totalPages"
          @click="nextPage"
        >
          Next
          <Icon name="heroicons:chevron-right" class="h-4 w-4" />
        </button>
      </div>
    </div>

    <CommonDecisionDetailModal :decision="selectedDecision" @close="closeDetail" />
  </div>
</template>
