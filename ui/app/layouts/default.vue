<script setup lang="ts">
const route = useRoute()

const navigation = [
  { name: 'Dashboard', path: '/', icon: 'heroicons:chart-bar-square' },
  { name: 'Portfolio', path: '/portfolio', icon: 'heroicons:briefcase' },
  { name: 'Watchlist', path: '/watchlist', icon: 'heroicons:eye' },
  { name: 'Decisions', path: '/decisions', icon: 'heroicons:scale' },
  { name: 'Notifications', path: '/notifications', icon: 'heroicons:bell' },
  { name: 'Intelligence', path: '/intelligence', icon: 'heroicons:signal' },
  { name: 'Market Data', path: '/market', icon: 'heroicons:chart-bar-square' },
  { name: 'Analysis', path: '/analysis', icon: 'heroicons:cpu-chip' },
  { name: 'Discovery', path: '/discovery', icon: 'heroicons:magnifying-glass' },
  { name: 'Jobs', path: '/jobs', icon: 'heroicons:clock' },
]

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<template>
  <div class="flex h-screen">
    <!-- Sidebar -->
    <aside class="flex w-64 flex-col border-r border-gray-800 bg-gray-900">
      <!-- Logo -->
      <div class="flex h-16 items-center gap-3 border-b border-gray-800 px-6">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600">
          <Icon name="heroicons:chart-bar" class="h-5 w-5 text-white" />
        </div>
        <span class="text-lg font-bold text-white">TradingAgents</span>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 space-y-1 px-3 py-4">
        <NuxtLink
          v-for="item in navigation"
          :key="item.path"
          :to="item.path"
          :class="isActive(item.path) ? 'nav-link-active' : 'nav-link'"
        >
          <Icon :name="item.icon" class="h-5 w-5" />
          {{ item.name }}
        </NuxtLink>
      </nav>

      <!-- Connection Status -->
      <div class="border-t border-gray-800 px-4 py-3">
        <CommonEventStatus />
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 overflow-y-auto">
      <div class="p-6">
        <slot />
      </div>
    </main>
  </div>
</template>
