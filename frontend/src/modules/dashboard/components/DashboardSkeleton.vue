<!-- filepath: /frontend/src/modules/dashboard/components/DashboardSkeleton.vue -->
<template>
  <div class="flex flex-col gap-4" aria-hidden="true">
    <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <div v-for="i in 4" :key="i" class="h-[118px] animate-pulse rounded-3xl bg-white" />
    </div>
    <div class="grid gap-4 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
      <div class="h-80 animate-pulse rounded-3xl bg-white" />
      <div class="h-80 animate-pulse rounded-3xl bg-white" />
    </div>
  </div>
</template>
