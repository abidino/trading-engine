<script setup lang="ts">
import type { NewsRecord } from '~/types/intelligence'

const {
  news, social, ticker, activeTab, loading, scanning, error, scanMessage,
  fetchAll, scanNews, scanMacro,
} = useIntelligence()

const tickerInput = ref('')

async function handleSearch() {
  if (!tickerInput.value.trim()) return
  await fetchAll(tickerInput.value.trim())
}

async function handleScan() {
  if (!tickerInput.value.trim()) return
  await scanNews(tickerInput.value.trim())
}

const sentimentColor = (sentiment: string) => {
  switch (sentiment) {
    case 'POSITIVE':
    case 'BULLISH': return 'badge-green'
    case 'NEGATIVE':
    case 'BEARISH': return 'badge-red'
    default: return 'badge-yellow'
  }
}

/** The tag most relevant to the searched ticker, falling back to the macro/first tag. */
const primaryTag = (item: NewsRecord) => {
  if (!item.tags?.length) return null
  return item.tags.find(t => t.ticker === ticker.value)
    ?? item.tags.find(t => t.ticker === 'ALL')
    ?? item.tags[0]
}

const formatTime = (iso: string) => (iso ? new Date(iso).toLocaleString() : '')
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-white">Intelligence</h1>
      <p class="text-sm text-gray-400">News &amp; social signals — scan on demand, classified by AI</p>
    </div>

    <!-- Ticker Search + Scan actions -->
    <div class="card space-y-3">
      <form class="flex flex-wrap gap-3" @submit.prevent="handleSearch">
        <input
          v-model="tickerInput"
          type="text"
          placeholder="Enter ticker (e.g. AAPL, NVDA)"
          class="flex-1 min-w-[12rem] rounded-lg border border-gray-700 bg-gray-800 px-4 py-2 text-sm text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none"
          required
        >
        <button type="submit" class="btn-primary" :disabled="loading || scanning">
          <Icon name="heroicons:magnifying-glass" class="mr-1.5 h-4 w-4" />
          View
        </button>
        <button type="button" class="btn-primary" :disabled="scanning || !tickerInput.trim()" @click="handleScan">
          <Icon name="heroicons:arrow-path" class="mr-1.5 h-4 w-4" :class="scanning ? 'animate-spin' : ''" />
          Scan News
        </button>
        <button type="button" class="btn-secondary" :disabled="scanning" @click="scanMacro()">
          <Icon name="heroicons:globe-alt" class="mr-1.5 h-4 w-4" />
          Scan Macro
        </button>
      </form>
      <p v-if="scanMessage" class="text-xs text-green-400">{{ scanMessage }}</p>
    </div>

    <!-- Error -->
    <div v-if="error" class="rounded-lg bg-red-900/20 p-4">
      <p class="text-sm text-red-400">{{ error }}</p>
    </div>

    <template v-if="ticker">
      <!-- Tabs -->
      <div class="flex gap-1 rounded-lg border border-gray-700 bg-gray-800/50 p-1 w-fit">
        <button
          :class="activeTab === 'news' ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'"
          class="rounded-md px-4 py-1.5 text-sm font-medium transition-colors"
          @click="activeTab = 'news'"
        >
          <Icon name="heroicons:newspaper" class="mr-1.5 h-4 w-4 inline" />
          News ({{ news.length }})
        </button>
        <button
          :class="activeTab === 'social' ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'"
          class="rounded-md px-4 py-1.5 text-sm font-medium transition-colors"
          @click="activeTab = 'social'"
        >
          <Icon name="heroicons:chat-bubble-left-right" class="mr-1.5 h-4 w-4 inline" />
          Social ({{ social.length }})
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
      </div>

      <!-- News Tab -->
      <div v-else-if="activeTab === 'news'" class="space-y-3">
        <div
          v-for="item in news"
          :key="item.url || item.headline"
          class="card space-y-2"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <div class="flex flex-wrap items-center gap-2 mb-1">
                <span class="text-xs font-medium text-gray-500 uppercase">{{ item.source }}</span>
                <span class="badge badge-blue text-xs">{{ item.category }}</span>
                <span v-if="primaryTag(item)" :class="sentimentColor(primaryTag(item)!.sentiment)" class="badge text-xs">
                  {{ primaryTag(item)!.sentiment }}
                </span>
                <span v-if="primaryTag(item)?.ticker === 'ALL'" class="badge badge-yellow text-xs">MACRO</span>
              </div>
              <a
                v-if="item.url"
                :href="item.url"
                target="_blank"
                rel="noopener"
                class="text-sm font-medium text-white hover:text-blue-400 leading-snug"
              >
                {{ item.headline }}
                <Icon name="heroicons:arrow-top-right-on-square" class="inline h-3 w-3 ml-0.5 opacity-60" />
              </a>
              <p v-else class="text-sm font-medium text-white leading-snug">{{ item.headline }}</p>
              <p v-if="primaryTag(item)?.interpretation" class="mt-1 text-xs text-blue-300 leading-snug">
                💡 {{ primaryTag(item)!.interpretation }}
              </p>
              <p v-if="item.summary" class="mt-1 text-xs text-gray-400 line-clamp-2">{{ item.summary }}</p>
            </div>
            <span class="shrink-0 text-xs text-gray-500">{{ formatTime(item.publishedAt) }}</span>
          </div>
        </div>
        <p v-if="!news.length" class="py-8 text-center text-sm text-gray-500">
          No news for {{ ticker }} yet — try <span class="text-blue-400">Scan News</span>.
        </p>
      </div>

      <!-- Social Tab -->
      <div v-else class="space-y-3">
        <div
          v-for="(item, idx) in social"
          :key="idx"
          class="card space-y-2"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-xs font-medium text-gray-500 uppercase">{{ item.source }}</span>
                <span v-if="item.engagementScore" class="text-xs text-gray-500">
                  <Icon name="heroicons:fire" class="inline h-3 w-3 text-orange-400" />
                  {{ item.engagementScore.toFixed(2) }}
                </span>
              </div>
              <p class="text-sm text-gray-300 leading-snug line-clamp-3">{{ item.content }}</p>
            </div>
          </div>
        </div>
        <p v-if="!social.length" class="py-8 text-center text-sm text-gray-500">
          No social signals for {{ ticker }} (social sentiment is not yet implemented).
        </p>
      </div>
    </template>

    <!-- Empty prompt -->
    <div v-else class="card flex flex-col items-center py-16 text-center">
      <Icon name="heroicons:signal" class="h-12 w-12 text-gray-600" />
      <p class="mt-3 text-sm text-gray-500">Enter a ticker, then Scan News to collect &amp; classify articles</p>
    </div>
  </div>
</template>
