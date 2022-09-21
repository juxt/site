/* eslint-disable react/jsx-no-useless-fragment */
import { useEffect } from 'react'
import { useForm, UseFormReturn } from 'react-hook-form'
import { useSearch } from '@tanstack/react-location'
import { useQueryClient } from 'react-query'
import { defaultMutationProps } from './utils'
import {
  CreateWorkflowStateMutationVariables,
  useCreateWorkflowStateMutation,
  useWorkflowStates,
  UpdateWorkflowStateMutationVariables,
  useUpdateWorkflowStateMutation,
  useWorkflowState,
  LocationGenerics,
  TWorkflowState
} from '../site'
import { ModalForm, ModalStateProps } from '../ui-common'

type AddWorkflowStateInput = Omit<
  CreateWorkflowStateMutationVariables,
  'colId' | 'workflowStateIds' | 'workflowId'
>

type AddWorkflowStateModalProps = ModalStateProps & {
  workflowId: string
}

export function AddWorkflowStateModal({
  isOpen,
  handleClose,
  workflowId
}: AddWorkflowStateModalProps) {
  const queryClient = useQueryClient()
  const addColMutation = useCreateWorkflowStateMutation({
    ...defaultMutationProps(queryClient, workflowId)
  })
  const cols = useWorkflowStates({ workflowId }).data || []

  const addWorkflowState = (col: AddWorkflowStateInput) => {
    if (workflowId) {
      handleClose()
      const colId = `col-${Date.now()}`
      addColMutation.mutate({
        ...col,
        workflowStateIds: [...cols.map((c) => c.id), colId],
        workflowId,
        colId
      })
    }
  }
  const formHooks = useForm<AddWorkflowStateInput>()
  return (
    <ModalForm<AddWorkflowStateInput>
      title="Add WorkflowState"
      formHooks={formHooks}
      onSubmit={formHooks.handleSubmit(addWorkflowState, console.warn)}
      isOpen={isOpen}
      handleClose={handleClose}
      fields={[
        {
          id: 'name',
          path: 'workflowState.name',
          rules: {
            required: true
          },
          label: 'WorkflowState Name',
          type: 'text'
        }
      ]}
    />
  )
}

type UpdateWorkflowStateInput = Omit<
  UpdateWorkflowStateMutationVariables,
  'colId' | 'workflowStateIds' | 'workflowId'
> & {
  tasksString: string
  rolesString: string
}

type UpdateWorkflowStateModalProps = ModalStateProps & {
  workflowId: string
  workflowState: TWorkflowState
}

export function UpdateWorkflowStateModal({
  isOpen,
  handleClose,
  workflowId,
  workflowState
}: UpdateWorkflowStateModalProps) {
  const colId = workflowState.id
  const formHooks = useForm<UpdateWorkflowStateInput>({
    defaultValues: {
      workflowState,
      tasksString: workflowState?.tasks?.join('\n'),
      rolesString: workflowState?.roles?.join('\n'),
      id: colId
    }
  })
  const queryClient = useQueryClient()
  const updateColMutation = useUpdateWorkflowStateMutation({
    ...defaultMutationProps(queryClient, workflowId)
  })

  const updateWorkflowState = (col: UpdateWorkflowStateInput) => {
    if (colId) {
      handleClose()
      updateColMutation.mutate({
        workflowState: {
          ...col.workflowState,
          tasks: col.tasksString.split('\n'),
          roles: col.rolesString.split('\n')
        },
        id: colId
      })
    }
  }

  return (
    <ModalForm<UpdateWorkflowStateInput>
      title="Update Column"
      formHooks={formHooks}
      onSubmit={formHooks.handleSubmit(updateWorkflowState, console.warn)}
      isOpen={isOpen}
      handleClose={handleClose}
      fields={[
        {
          id: 'name',
          path: 'workflowState.name',
          rules: {
            required: true
          },
          label: 'Column Name',
          type: 'text'
        },
        {
          id: 'description',
          path: 'workflowState.description',
          label: 'Description',
          rows: 8,
          type: 'textarea'
        },
        {
          id: 'tasks',
          path: 'tasksString',
          label: 'Tasks',
          type: 'textarea'
        },
        {
          id: 'roles',
          path: 'rolesString',
          label: 'Roles',
          type: 'textarea'
        }
      ]}
    />
  )
}

export function UpdateWorkflowStateModalWrapper({
  isOpen,
  handleClose,
  workflowId
}: ModalStateProps & {
  workflowId: string
}) {
  const { modalState } = useSearch<LocationGenerics>()
  const colId = modalState?.workflowStateId
  const workflowState = useWorkflowState(workflowId, colId)?.data

  return (
    <>
      {colId && workflowState && (
        <UpdateWorkflowStateModal
          isOpen={isOpen}
          handleClose={handleClose}
          workflowId={workflowId}
          workflowState={workflowState}
        />
      )}
    </>
  )
}
