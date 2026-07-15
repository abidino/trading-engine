<script setup lang="ts">
const { summary, sectors, performance, loading, error, fetchSummary, fetchSectors, fetchPerformance, refreshQuotes, runAutoPromote, runScheduledAnalysis } = useDashboard()
const { events } = useEventStream()

onMounted(async () => {
  await refreshQuotes()
  await Promise.all([fetchSummary(), fetchSectors(), fetchPerformance()])
})

const formatCurrency = (value: number | string) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value))

const formatPercent = (value: number | string) => {
  const n = Number(value)
  return `${n >= 0 ? '+' : ''}${n.toFixed(2)}%`
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Dashboard</h1>
        <p class="text-sm text-gray-400">Portfolio overview & real-time events</p>
      </div>
      <div class="flex gap-2">
        <button class="btn-secondary" @click="runAutoPromote">
          <Icon name="heroicons:arrow-up-circle" class="mr-1.5 h-4 w-4" />
          Auto Promote
        </button>
        <button class="btn-primary" @click="runScheduledAnalysis">
          <Icon name="heroicons:cpu-chip" class="mr-1.5 h-4 w-4" />
          Run Analysis
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="card border-red-800">
      <p class="text-red-400">{{ error }}</p>
    </div>

    <!-- Summary Cards -->
    <template v-else-if="summary">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <!-- Total Value -->
        <div class="card">
          <p class="stat-label">Total Market Value</p>
          <p class="stat-value text-white">{{ formatCurrency(summary.totalMarketValue) }}</p>
        </div>

        <!-- Unrealized P&L -->
        <div class="card">
          <p class="stat-label">Unrealized P&L</p>
          <p
            class="stat-value"
            :class="summary.totalUnrealizedPnl >= 0 ? 'text-green-400' : 'text-red-400'"
          >
            {{ formatCurrency(summary.totalUnrealizedPnl) }}
          </p>
          <p
            class="text-sm"
            :class="summary.totalUnrealizedPnlPercent >= 0 ? 'text-green-500' : 'text-red-500'"
          >
            {{ formatPercent(summary.totalUnrealizedPnlPercent) }}
          </p>
        </div>

        <!-- Positions -->
        <div class="card">
          <p class="stat-label">Active Positions</p>
          <p class="stat-value text-white">{{ summary.positionCount }}</p>
        </div>

        <!-- Decision Accuracy -->
        <div class="card">
          <p class="stat-label">Decision Accuracy</p>
          <p class="stat-value text-blue-400">{{ formatPercent(summary.decisionAccuracy) }}</p>
        </div>
      </div>

      <!-- Top Movers -->
      <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <!-- Top Gainers -->
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Top Gainers</h3>
            <span class="badge-green">{{ summary.topGainers.length }}</span>
          </div>
          <div class="space-y-3">
            <div
              v-for="item in summary.topGainers"
              :key="item.ticker"
              class="flex items-center justify-between"
            >
              <span class="font-medium text-white">{{ item.ticker }}</span>
              <div class="text-right">
                <span class="text-green-400">{{ formatCurrency(item.pnl) }}</span>
                <span class="ml-2 text-xs text-green-500">{{ formatPercent(item.pnlPercent) }}</span>
              </div>
            </div>
            <p v-if="!summary.topGainers.length" class="text-sm text-gray-500">No data</p>
          </div>
        </div>

        <!-- Top Losers -->
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Top Losers</h3>
            <span class="badge-red">{{ summary.topLosers.length }}</span>
          </div>
          <div class="space-y-3">
            <div
              v-for="item in summary.topLosers"
              :key="item.ticker"
              class="flex items-center justify-between"
            >
              <span class="font-medium text-white">{{ item.ticker }}</span>
              <div class="text-right">
                <span class="text-red-400">{{ formatCurrency(item.pnl) }}</span>
                <span class="ml-2 text-xs text-red-500">{{ formatPercent(item.pnlPercent) }}</span>
              </div>
            </div>
            <p v-if="!summary.topLosers.length" class="text-sm text-gray-500">No data</p>
          </div>
        </div>
      </div>

      <!-- Sector Allocation -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">Sector Allocation</h3>
        </div>
        <div class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          <div v-for="sector in sectors" :key="sector.sector" class="space-y-1">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-300">{{ sector.sector }}</span>
              <span class="text-xs text-gray-500">{{ Number(sector.percentage || 0).toFixed(1) }}%</span>
            </div>
            <div class="h-2 rounded-full bg-gray-800">
              <div
                class="h-2 rounded-full bg-blue-500"
                :style="{ width: `${Number(sector.percentage || 0)}%` }"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Performance Table -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">Position Performance</h3>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-800 text-left text-gray-400">
                <th class="pb-3 font-medium">Ticker</th>
                <th class="pb-3 font-medium">Market Value</th>
                <th class="pb-3 font-medium">Cost Basis</th>
                <th class="pb-3 font-medium">P&L</th>
                <th class="pb-3 font-medium">Win Rate</th>
                <th class="pb-3 font-medium">Decisions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-800">
              <tr v-for="entry in performance" :key="entry.ticker">
                <td class="py-3 font-medium text-white">{{ entry.ticker }}</td>
                <td class="py-3 text-gray-300">{{ formatCurrency(entry.marketValue) }}</td>
                <td class="py-3 text-gray-300">{{ formatCurrency(entry.costBasis) }}</td>
                <td
                  class="py-3"
                  :class="Number(entry.unrealizedPnl) >= 0 ? 'text-green-400' : 'text-red-400'"
                >
                  {{ formatCurrency(entry.unrealizedPnl) }}
                  <span class="ml-1 text-xs">{{ formatPercent(entry.unrealizedPnlPercent) }}</span>
                </td>
                <td class="py-3 text-gray-300">{{ (Number(entry.winRate || 0) * 100).toFixed(0) }}%</td>
                <td class="py-3 text-gray-300">{{ entry.decisionCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Live Events Feed -->
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">Live Events</h3>
        <span class="badge-blue">{{ events.length }}</span>
      </div>
      <div class="max-h-64 space-y-2 overflow-y-auto">
        <div
          v-for="(event, index) in events.slice(0, 20)"
          :key="index"
          class="flex items-center gap-3 rounded-lg bg-gray-800/50 px-3 py-2"
        >
          <span class="badge-blue text-[10px]">{{ event.type }}</span>
          <span class="flex-1 truncate text-xs text-gray-300">
            {{ JSON.stringify(event.data) }}
          </span>
          <span class="text-[10px] text-gray-500">
            {{ new Date(event.timestamp).toLocaleTimeString() }}
          </span>
        </div>
        <p v-if="!events.length" class="text-center text-sm text-gray-500">
          No events yet. Waiting for live data...
        </p>
      </div>
    </div>
  </div>
</template>
