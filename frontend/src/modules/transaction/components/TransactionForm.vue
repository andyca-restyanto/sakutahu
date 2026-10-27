<!-- filepath: /frontend/src/modules/transaction/components/TransactionForm.vue -->
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import { todayIn } from '@/shared/utils/date'
import { useAuthStore } from '@/modules/auth'
import { useCategoryStore, type TransactionType } from '@/modules/category'
import { transactionService } from '../services/transaction.service'
import { transactionSchema, type TransactionForm } from '../types/transaction.schemas'
import type { TransactionResponse } from '../types/transaction.types'
import AmountInput from './AmountInput.vue'
import CategoryPicker from './CategoryPicker.vue'

const props = defineProps<{ initial: TransactionResponse | null; defaultType: TransactionType }>()
const emit = defineEmits<{ saved: [tx: TransactionResponse]; delete: [tx: TransactionResponse]; cancel: [] }>()

const auth = useAuthStore()
const categories = useCategoryStore()
const today = todayIn(auth.user?.timezone)

const { handleSubmit, errors, defineField, setErrors, setFieldError, isSubmitting } = useForm({
  validationSchema: toTypedSchema(transactionSchema(today)),
  initialValues: {
    type: props.initial?.type ?? props.defaultType,
    categoryId: props.initial?.category.id,
    amount: props.initial?.amount,
    occurredOn: props.initial?.occurredOn ?? today,
    note: props.initial?.note ?? '',
  },
})
const cfg = { validateOnModelUpdate: false }
const [type] = defineField('type')
const [categoryId] = defineField('categoryId', cfg)
const [amount] = defineField('amount', cfg)
const [occurredOn, occurredOnAttrs] = defineField('occurredOn', { validateOnBlur: true, validateOnModelUpdate: false })
const [note, noteAttrs] = defineField('note', cfg)

const formError = ref<string | null>(null)
const isEdit = computed(() => !!props.initial)
const visibleCategories = computed(() => categories.ofType(type.value ?? 'EXPENSE'))

// Ganti jenis transaksi → kategori lama tidak cocok lagi, kosongkan
watch(type, (t) => {
  const current = categoryId.value != null ? categories.byId.get(categoryId.value) : undefined
  if (current && current.type !== t) categoryId.value = undefined
})

onMounted(() => {
  void categories.ensureLoaded()
})

const onSubmit = handleSubmit(async (values: TransactionForm) => {
  formError.value = null
  const payload = {
    type: values.type,
    categoryId: values.categoryId,
    amount: values.amount,
    occurredOn: values.occurredOn,
    note: values.note?.trim() || null,
  }
  try {
    const saved = props.initial
      ? await transactionService.update(props.initial.id, payload)
      : await transactionService.create(payload)
    emit('saved', saved)
  } catch (e) {
    const err = toApiError(e)
    if (err.code === 'CATEGORY_TYPE_MISMATCH') {
      setFieldError('categoryId', err.message)
    } else if (err.code === 'VALIDATION_ERROR' && err.errors.length) {
      setErrors(fieldErrorsToRecord<keyof TransactionForm>(err.errors))
    } else {
      formError.value = err.message
    }
  }
})
</script>

<template>
  <form class="flex flex-col gap-5" novalidate @submit="onSubmit">
    <FormAlert v-if="formError">{{ formError }}</FormAlert>

    <!-- Jenis transaksi -->
    <div role="radiogroup" aria-label="Jenis transaksi" class="grid grid-cols-2 gap-1 rounded-2xl bg-krem-100 p-1">
      <button
        v-for="opt in [{ v: 'EXPENSE', label: 'Pengeluaran' }, { v: 'INCOME', label: 'Pemasukan' }] as const"
        :key="opt.v"
        type="button"
        role="radio"
        :aria-checked="type === opt.v"
        class="rounded-xl py-2.5 text-[15px] font-semibold transition"
        :class="
          type === opt.v
            ? opt.v === 'EXPENSE'
              ? 'bg-white text-arang-900 shadow-sm'
              : 'bg-white text-daun-700 shadow-sm'
            : 'text-arang-600 hover:text-arang-900'
        "
        @click="type = opt.v"
      >
        {{ opt.label }}
      </button>
    </div>

    <AmountInput v-model="amount" label="Nominal" :error="errors.amount" autofocus />

    <div v-if="!categories.loaded" class="grid grid-cols-3 gap-2 sm:grid-cols-4" aria-hidden="true">
      <div v-for="i in 8" :key="i" class="h-[84px] animate-pulse rounded-2xl bg-krem-100" />
    </div>
    <CategoryPicker v-else v-model="categoryId" :categories="visibleCategories" :error="errors.categoryId" />

    <div class="grid gap-5 sm:grid-cols-2">
      <BaseInput
        v-model="occurredOn"
        v-bind="occurredOnAttrs"
        label="Tanggal"
        type="date"
        :max="today"
        min="2000-01-01"
        :error="errors.occurredOn"
      />
      <BaseInput
        v-model="note"
        v-bind="noteAttrs"
        label="Catatan (opsional)"
        placeholder="mis. kopi susu"
        maxlength="255"
        :error="errors.note"
      />
    </div>

    <div class="mt-1 flex flex-col-reverse gap-2 sm:flex-row sm:items-center">
      <button
        v-if="isEdit && initial"
        type="button"
        class="rounded-xl px-4 py-3 text-[15px] font-semibold text-red-600 hover:bg-red-50 sm:mr-auto"
        @click="emit('delete', initial)"
      >
        Hapus
      </button>
      <BaseButton variant="secondary" class="sm:ml-auto" @click="emit('cancel')">Batal</BaseButton>
      <BaseButton type="submit" :loading="isSubmitting">{{ isEdit ? 'Simpan perubahan' : 'Simpan' }}</BaseButton>
    </div>
  </form>
</template>
