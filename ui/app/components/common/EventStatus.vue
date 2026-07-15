<script setup lang="ts">
const { isConnected } = useEventStream()
</script>

<template>
  <div class="flex items-center gap-2">
    <span
      class="h-2 w-2 rounded-full"
      :class="isConnected ? 'bg-green-500' : 'bg-red-500'"
    />
    <span class="text-xs text-gray-400">
      {{ isConnected ? 'Connected' : 'Disconnected' }}
    </span>
  </div>
</template>
