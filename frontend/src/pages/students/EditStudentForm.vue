<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item label="Status:" prop="enabled">
      <!-- An active student is deactivated only through the confirmed Deactivate action (UC-STU-deactivate-student) -->
      <template v-if="loadedAsActive">
        <el-tag type="success" data-cy="student-status">Active</el-tag>
        <el-button
          type="danger"
          size="small"
          style="margin-left: 12px"
          data-cy="deactivate-student"
          @click="confirmDeactivation()"
        >
          Deactivate
        </el-button>
      </template>
      <!-- A deactivated student: the switch can only turn the account back on (until UC-STU-reactivate-student) -->
      <el-switch
        v-else
        v-model="studentBeingEdited.enabled"
        inline-prompt
        active-text="Active"
        inactive-text="Deactivated"
        data-cy="student-status-switch"
        style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
      />
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button type="primary" @click="updateExistingStudent()"> Confirm </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { h, ref, onMounted } from 'vue'

const { studentId } = defineProps(['studentId'])
// close-dialog: the dialog is done; student-changed: the student's status changed, so the list should reload
const emit = defineEmits(['close-dialog', 'student-changed'])

const loadedAsActive = ref<boolean>(false) // The status as loaded from the server, not as edited in the form

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  enabled: [{ required: true, message: 'Please select the status of the user.', trigger: 'blur' }],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent(studentId: number) {
  // Load the user to be edited
  const result = await findStudentById(studentId)

  studentBeingEdited.value.id = result.data.id as number
  studentBeingEdited.value.username = result.data.username
  studentBeingEdited.value.enabled = result.data.enabled
  studentBeingEdited.value.firstName = result.data.firstName
  studentBeingEdited.value.lastName = result.data.lastName
  studentBeingEdited.value.email = result.data.email
  studentBeingEdited.value.teamName = result.data.teamName
  loadedAsActive.value = result.data.enabled === true
}

onMounted(() => {
  loadStudent(studentId) // Load the artifact to be edited
})

async function updateExistingStudent() {
  await studentForm.value!.validate() // Validate the form

  const updatedStudent: Student = {
    id: studentBeingEdited.value.id,
    username: studentBeingEdited.value.username,
    enabled: studentBeingEdited.value.enabled,
    firstName: studentBeingEdited.value.firstName,
    lastName: studentBeingEdited.value.lastName,
    email: studentBeingEdited.value.email
  }
  // Call the API to update the user
  await updateStudent(updatedStudent)

  ElMessage.success('Student updated successfully')
  emit('close-dialog')
}

// UC-STU-deactivate-student, steps 3-6. The consequences come from the use case's Associated Information and
// BR-deactivated-not-evaluated. The server decides 3a (409) and 3b (403); the shared request instance has already shown
// its toast or routed to the forbidden page, so a refusal only reloads the student to replace a stale view.
async function confirmDeactivation() {
  const name = `${studentBeingEdited.value.firstName} ${studentBeingEdited.value.lastName}`
  try {
    await ElMessageBox.confirm(
      h('div', { 'data-cy': 'deactivate-consequences' }, [
        h('p', `You are about to deactivate ${name}. After deactivation:`),
        h('ul', [
          h(
            'li',
            'The student can no longer log in or submit weekly activity reports and peer evaluations. A session the student already has open ends on its next request.'
          ),
          h(
            'li',
            "The student's account and previously submitted work are kept, and the student stays in the same course section and team."
          ),
          h('li', 'Teammates are no longer asked to evaluate the student.')
        ]),
        h('p', 'This is not a deletion. The student can be reactivated later.')
      ]),
      'Deactivate this student?',
      {
        confirmButtonText: 'Deactivate',
        cancelButtonText: 'Cancel',
        confirmButtonClass: 'deactivate-confirm',
        cancelButtonClass: 'deactivate-cancel',
        type: 'warning'
      }
    )
  } catch {
    return // 5a: cancelled, nothing is sent and the student is unchanged
  }

  try {
    await deactivateStudent(studentBeingEdited.value.id as number)
    ElMessage.success('The student has been deactivated.')
  } catch {
    // 3a/3b: already reported by the request instance; fall through to reload the student
  }
  await loadStudent(studentBeingEdited.value.id as number)
  emit('student-changed')
}
</script>

<style scoped lang="scss"></style>
