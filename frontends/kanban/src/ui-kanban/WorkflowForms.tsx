import { useForm } from 'react-hook-form'
import { distinctBy } from '../utils'
import { useQueryClient } from 'react-query'
import { toast } from 'react-toastify'
import {
  CreateWorkflowMutationVariables,
  useCreateWorkflowMutation,
  useStatesOptions
} from '../site'
import { ModalStateProps, ModalForm, Option } from '../ui-common'
import { defaultMutationProps } from './utils'

type AddWorkflowInput = Omit<
  CreateWorkflowMutationVariables,
  'workflowStateIds'
> & {
  workflowStateIds: Option[] | undefined
}

type AddWorkflowModalProps = ModalStateProps & {
  workflowId: string
}

export function AddWorkflowModal({
  isOpen,
  handleClose,
  workflowId
}: AddWorkflowModalProps) {
  const queryClient = useQueryClient()
  const addWorkflowMutation = useCreateWorkflowMutation({
    ...defaultMutationProps(queryClient, workflowId)
  })

  const addWorkflow = (input: AddWorkflowInput) => {
    handleClose()
    const { workflowStateIds, ...workflowInput } = input

    const newWorkflowStates =
      workflowStateIds?.map((c) => ({
        name: c.label,
        id: `col${Math.random().toString()}`
      })) || []
    const data = {
      ...workflowInput,
      workflowStates: newWorkflowStates,
      workflowStateIds: newWorkflowStates?.map((c) => c.id)
    }
    toast.promise(addWorkflowMutation.mutateAsync(data), {
      pending: 'Creating workflow...',
      success: 'Workflow created!',
      error: 'Error creating workflow'
    })
  }

  const formHooks = useForm<AddWorkflowInput>()
  const [, cols] = useStatesOptions({ workflowId })
  const workflowStates: Option[] =
    distinctBy<typeof cols[0]>(cols, 'label') || []
  return (
    <ModalForm<AddWorkflowInput>
      title="Add Workflow"
      formHooks={formHooks}
      fields={[
        {
          id: 'workflowStates',
          type: 'multiselect',
          options: workflowStates,
          path: 'workflowStateIds',
          label: 'WorkflowStates'
        },
        {
          id: 'WorkflowName',
          placeholder: 'Workflow Name',
          type: 'text',
          rules: {
            required: true
          },
          path: 'name'
        },
        {
          id: 'WorkflowDescription',
          placeholder: 'Workflow Description',
          type: 'text',
          path: 'description'
        }
      ]}
      onSubmit={formHooks.handleSubmit(addWorkflow, console.warn)}
      isOpen={isOpen}
      handleClose={handleClose}
    />
  )
}
