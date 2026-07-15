<script setup lang="ts">
import type { TradingLevels, AnalysisRequestType } from '~/types'

const { runs, currentRun, loading, error, startAnalysis, suggestType, fetchRuns } = useAnalysis()

onMounted(() => {
  fetchRuns()
})

const tickerInput = ref('')
const analysisType = ref<AnalysisRequestType>('DISCOVERY')
const typeManuallyChosen = ref(false)

const typeOptions: { value: AnalysisRequestType, label: string }[] = [
  { value: 'WATCHLIST_REVIEW', label: 'Al / İzleme (BUY/WAIT)' },
  { value: 'PORTFOLIO_REVIEW', label: 'Sat / Portföy (SELL/HOLD)' },
  { value: 'DISCOVERY', label: 'Keşif (ADD/IGNORE)' },
]

// Auto-detect the intent from where the ticker lives, unless the user picked one manually.
let suggestTimer: ReturnType<typeof setTimeout> | null = null
watch(tickerInput, (val) => {
  if (typeManuallyChosen.value) return
  const t = val.trim().toUpperCase()
  if (suggestTimer) clearTimeout(suggestTimer)
  if (!t) return
  suggestTimer = setTimeout(async () => {
    if (!typeManuallyChosen.value) analysisType.value = await suggestType(t)
  }, 400)
})

async function handleRunAnalysis() {
  if (!tickerInput.value.trim()) return
  await startAnalysis(tickerInput.value.trim().toUpperCase(), analysisType.value)
  tickerInput.value = ''
  typeManuallyChosen.value = false
}

const statusColor = (status: string) => {
  switch (status) {
    case 'completed': return 'badge-green'
    case 'running': return 'badge-blue'
    case 'failed': return 'badge-red'
    default: return 'badge-yellow'
  }
}

const formatTime = (iso?: string) =>
  iso ? new Date(iso).toLocaleString() : '—'

const hasLevels = (l?: TradingLevels | null) =>
  !!l && [l.entryLow, l.entryHigh, l.aggressiveEntry, l.idealEntry, l.safeEntry,
    l.stopLoss, l.takeProfit, l.nearestSupport, l.nearestResistance]
    .some(v => v != null)

const fmtLevel = (v?: number | null) =>
  v == null ? '—' : `$${v.toFixed(2)}`
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Analysis</h1>
        <p class="text-sm text-gray-400">Run AI-powered multi-agent analysis on tickers</p>
      </div>
    </div>

    <!-- Run Analysis Form -->
    <div class="card">
      <h3 class="card-title mb-4">Run New Analysis</h3>
      <form class="flex flex-col gap-3 sm:flex-row" @submit.prevent="handleRunAnalysis">
        <input
          v-model="tickerInput"
          type="text"
          placeholder="Enter ticker (e.g., AAPL)"
          class="flex-1 rounded-lg border border-gray-700 bg-gray-800 px-4 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
          required
        />
        <select
          v-model="analysisType"
          class="rounded-lg border border-gray-700 bg-gray-800 px-4 py-2 text-sm text-white focus:border-blue-500 focus:outline-none"
          @change="typeManuallyChosen = true"
        >
          <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <button type="submit" class="btn-primary" :disabled="loading">
          <Icon name="heroicons:play" class="mr-1.5 h-4 w-4" />
          Analyze
        </button>
      </form>
    </div>

    <!-- Current Run Status -->
    <div v-if="currentRun" class="card border-blue-800">
      <div class="card-header">
        <h3 class="card-title">Current Analysis</h3>
        <span :class="statusColor(currentRun.status)">{{ currentRun.status }}</span>
      </div>
      <div class="space-y-3">
        <div class="flex items-center gap-4">
          <span class="text-lg font-bold text-white">{{ currentRun.ticker }}</span>
          <span class="text-xs text-gray-400">Run ID: {{ currentRun.runId }}</span>
        </div>

        <!-- Progress Indicator -->
        <div v-if="currentRun.status === 'running'" class="flex items-center gap-3">
          <div class="h-4 w-4 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
          <span class="text-sm text-blue-400">Agents are analyzing {{ currentRun.ticker }}...</span>
        </div>

        <!-- Result -->
        <div v-if="currentRun.result" class="space-y-3 rounded-lg bg-gray-800/50 p-4">
          <div class="flex items-center gap-3">
            <span
              class="badge text-base"
              :class="{
                'badge-green': currentRun.result.action === 'BUY',
                'badge-red': currentRun.result.action === 'SELL',
                'badge-yellow': currentRun.result.action === 'HOLD',
              }"
            >
              {{ currentRun.result.action }}
            </span>
            <span class="text-sm text-gray-400">
              Confidence: {{ (currentRun.result.confidence * 100).toFixed(0) }}%
            </span>
          </div>
          <p class="text-sm text-gray-300">{{ currentRun.result.reasoning }}</p>

          <!-- Counter-thesis & Risks (mandatory opposing view) -->
          <div
            v-if="(currentRun.result.counterThesis && currentRun.result.counterThesis.trim()) || (currentRun.result.keyRisks && currentRun.result.keyRisks.length)"
            class="rounded-lg border border-amber-800/60 bg-amber-900/15 p-3"
          >
            <p class="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-amber-400">
              <Icon name="heroicons:exclamation-triangle" class="h-4 w-4" /> Karşıt Görüş & Riskler
            </p>
            <p
              v-if="currentRun.result.counterThesis && currentRun.result.counterThesis.trim()"
              class="mb-2 text-sm text-amber-100/90"
            >
              {{ currentRun.result.counterThesis }}
            </p>
            <ul v-if="currentRun.result.keyRisks && currentRun.result.keyRisks.length" class="list-disc space-y-1 pl-5 text-sm text-amber-100/80">
              <li v-for="(risk, i) in currentRun.result.keyRisks" :key="i">{{ risk }}</li>
            </ul>
          </div>

          <!-- Trading levels -->
          <div
            v-if="currentRun.result.levels && hasLevels(currentRun.result.levels)"
            class="grid grid-cols-2 gap-2 rounded bg-gray-800 p-3 sm:grid-cols-3"
          >
            <div v-if="currentRun.result.levels.entryLow != null || currentRun.result.levels.entryHigh != null">
              <p class="text-xs text-gray-400">Buy Zone</p>
              <p class="text-sm font-semibold text-green-400">
                {{ fmtLevel(currentRun.result.levels.entryLow) }} – {{ fmtLevel(currentRun.result.levels.entryHigh) }}
              </p>
            </div>
            <div v-if="currentRun.result.levels.aggressiveEntry != null">
              <p class="text-xs text-gray-400">Agresif Giriş</p>
              <p class="text-sm font-semibold text-orange-400">{{ fmtLevel(currentRun.result.levels.aggressiveEntry) }}</p>
            </div>
            <div v-if="currentRun.result.levels.idealEntry != null">
              <p class="text-xs text-gray-400">İdeal Giriş</p>
              <p class="text-sm font-semibold text-green-400">{{ fmtLevel(currentRun.result.levels.idealEntry) }}</p>
            </div>
            <div v-if="currentRun.result.levels.safeEntry != null">
              <p class="text-xs text-gray-400">Güvenli Giriş</p>
              <p class="text-sm font-semibold text-emerald-300">{{ fmtLevel(currentRun.result.levels.safeEntry) }}</p>
            </div>
            <div v-if="currentRun.result.levels.stopLoss != null">
              <p class="text-xs text-gray-400">Stop-Loss</p>
              <p class="text-sm font-semibold text-red-400">{{ fmtLevel(currentRun.result.levels.stopLoss) }}</p>
            </div>
            <div v-if="currentRun.result.levels.takeProfit != null">
              <p class="text-xs text-gray-400">Take-Profit</p>
              <p class="text-sm font-semibold text-blue-400">{{ fmtLevel(currentRun.result.levels.takeProfit) }}</p>
            </div>
            <div v-if="currentRun.result.levels.nearestSupport != null">
              <p class="text-xs text-gray-400">Support</p>
              <p class="text-sm text-gray-200">{{ fmtLevel(currentRun.result.levels.nearestSupport) }}</p>
            </div>
            <div v-if="currentRun.result.levels.nearestResistance != null">
              <p class="text-xs text-gray-400">Resistance</p>
              <p class="text-sm text-gray-200">{{ fmtLevel(currentRun.result.levels.nearestResistance) }}</p>
            </div>
          </div>

          <!-- Aggregated Analyst Summaries -->
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div v-if="currentRun.result.technicalSummary" class="rounded bg-gray-800 p-3">
              <p class="mb-1 flex items-center gap-1.5 text-xs font-medium text-blue-400">
                <Icon name="heroicons:chart-bar" class="h-3.5 w-3.5" /> Technical
              </p>
              <p class="text-xs text-gray-300">{{ currentRun.result.technicalSummary }}</p>
            </div>
            <div v-if="currentRun.result.fundamentalSummary" class="rounded bg-gray-800 p-3">
              <p class="mb-1 flex items-center gap-1.5 text-xs font-medium text-green-400">
                <Icon name="heroicons:building-library" class="h-3.5 w-3.5" /> Fundamental
              </p>
              <p class="text-xs text-gray-300">{{ currentRun.result.fundamentalSummary }}</p>
            </div>
            <div v-if="currentRun.result.newsSummary" class="rounded bg-gray-800 p-3">
              <p class="mb-1 flex items-center gap-1.5 text-xs font-medium text-yellow-400">
                <Icon name="heroicons:newspaper" class="h-3.5 w-3.5" /> News
              </p>
              <p class="text-xs text-gray-300">{{ currentRun.result.newsSummary }}</p>
            </div>
            <div v-if="currentRun.result.socialSummary" class="rounded bg-gray-800 p-3">
              <p class="mb-1 flex items-center gap-1.5 text-xs font-medium text-purple-400">
                <Icon name="heroicons:chat-bubble-left-right" class="h-3.5 w-3.5" /> Social
              </p>
              <p class="text-xs text-gray-300">{{ currentRun.result.socialSummary }}</p>
            </div>
          </div>
        </div>

        <!-- Error -->
        <div v-if="currentRun.error" class="rounded-lg bg-red-900/20 p-4">
          <p class="text-sm text-red-400">{{ currentRun.error }}</p>
        </div>
      </div>
    </div>

    <!-- Analysis History -->
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">Analysis History</h3>
        <button class="btn-secondary text-xs" @click="fetchRuns">
          <Icon name="heroicons:arrow-path" class="mr-1 h-3 w-3" />
          Refresh
        </button>
      </div>
      <div class="space-y-2">
        <div
          v-for="run in runs"
          :key="run.runId"
          class="flex items-center justify-between rounded-lg bg-gray-800/50 px-4 py-3"
        >
          <div class="flex items-center gap-3">
            <span class="font-medium text-white">{{ run.ticker }}</span>
            <span :class="statusColor(run.status)">{{ run.status }}</span>
            <span
              v-if="run.result"
              class="badge"
              :class="{
                'badge-green': run.result.action === 'BUY',
                'badge-red': run.result.action === 'SELL',
                'badge-yellow': run.result.action === 'HOLD',
              }"
            >
              {{ run.result.action }}
            </span>
          </div>
          <div class="text-right">
            <p class="text-xs text-gray-500">{{ formatTime(run.startedAt) }}</p>
            <p v-if="run.result" class="text-xs text-gray-400">
              {{ (run.result.confidence * 100).toFixed(0) }}% confidence
            </p>
          </div>
        </div>
        <p v-if="!runs.length" class="py-6 text-center text-sm text-gray-500">
          No analysis runs yet. Enter a ticker above to start.
        </p>
      </div>
    </div>
  </div>
</template>
