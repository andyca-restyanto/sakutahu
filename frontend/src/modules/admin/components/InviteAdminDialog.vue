<!-- filepath: /frontend/src/modules/admin/components/InviteAdminDialog.vue -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import BaseDialog from '@/shared/components/BaseDialog.vue'
import BaseInput from '@/shared/components/BaseInput.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import FormAlert from '@/shared/components/FormAlert.vue'
import { fieldErrorsToRecord, toApiError } from '@/shared/api/errors'
import { adminAccountService } from '../services/adminAccount.service'
import { inviteAdminSchema, type InviteAdminForm } from '../types/admin.schemas'
import type { AdminInvitationResponse } from '../types/admin.types'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: []; invited: [invitation: AdminInvitationResponse] }>()

const { handleSubmit, errors, defineField, setErrors, resetForm, isSubmitting } = useForm({
  validationSchema: toTypedSchema(inviteAdminSchema),
})
const cfg = { validateOnModelUpdate: false, validateOnBlur: true }
const [fullName, fullNameAttrs] = defineField('fullName', cfg)
const [email, emailAttrs] = defineField('email', cfg)
const formError = ref<string | null>(null)

watch(
  () => props.open,
  (open) => {
    if (open) {
      resetForm()
      formError.value = null
    }
  },
)

const onSubmit = handleSubmit(async (values) => {
  formError.value = null
  try {
    emit('invited', await adminAccountService.invite({ email: values.email, fullName: values.fullName }))
  } catch (e) {
    const err = toApiError(e)
    if (err.code === 'VALIDATION_ERROR' && err.errors.length) {
      setErrors(fieldErrorsToRecord<keyof InviteAdminForm>(err.errors))
    } else {
      formError.value = err.message
    }
  }
})
</script>

<template>
  <BaseDialog :open="open" title="Undang admin" @close="emit('close')">
    <form class="flex flex-col gap-5" novalidate @submit="onSubmit">
      <p class="text-sm text-arang-600">
        Link undangan dikirim ke email ini dan berlaku 2 hari. Penerima membuat password sendiri. Admin baru bisa
        mengelola user &amp; kuota struk, tapi tidak bisa mengelola admin.
      </p>
      <FormAlert v-if="formError">{{ formError }}</FormAlert>
      <BaseInput v-model="fullName" v-bind="fullNameAttrs" label="Nama" autocomplete="off" :error="errors.fullName" />
      <BaseInput
        v-model="email"
        v-bind="emailAttrs"
        label="Email"
        type="email"
        inputmode="email"
        autocomplete="off"
        :error="errors.email"
      />
      <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <BaseButton variant="secondary" @click="emit('close')">Batal</BaseButton>
        <BaseButton type="submit" :loading="isSubmitting">Kirim undangan</BaseButton>
      </div>
    </form>
  </BaseDialog>
</template>
