import {
  purgeQueries,
  TWorkflowState,
  useAddClientTags,
  useCreateHiringCardMutation
} from '../../site'
import { ModalForm, Option } from '../../ui-common'
import { defaultMutationProps } from '../../ui-kanban'
import { notEmpty } from '../../utils'
import { useForm } from 'react-hook-form'
import { useQueryClient } from 'react-query'
import { toast } from 'react-toastify'
import { workflowId } from '../constants'
import { AddHiringCardInput, AddHiringCardModalProps } from './types'

export function AddHiringCardModal({
  isOpen,
  handleClose,
  defaultValues,
  cols,
  clientOptions,
  usernameOptions,
  projectOptions,
  stateOptions
}: AddHiringCardModalProps & {
  defaultValues: Partial<AddHiringCardInput>
  cols: TWorkflowState[]
  clientOptions: Option[]
  usernameOptions: Option[]
  projectOptions: Option[]
  stateOptions: Option[]
}) {
  const formHooks = useForm<AddHiringCardInput>({
    defaultValues
  })

  const handleCreateClient = useAddClientTags()
  const queryClient = useQueryClient()
  const AddHiringCardMutation = useCreateHiringCardMutation({
    ...defaultMutationProps(queryClient, workflowId),
    onSuccess: (data) => {
      purgeQueries(['workflow'])
    }
  })
  const AddHiringCard = (card: AddHiringCardInput) => {
    if (!cols.length) {
      toast.error('No workflowStates to add card to')
      return
    }
    handleClose()
    const newId = `hiring-card-${card.card.title}-${Date.now()}`
    const { project, workflowState, owners, potentialClients, ...cardInput } =
      card
    const colId = workflowState?.value || 'WorkflowStateAwaitScreen'
    toast.promise(
      AddHiringCardMutation.mutateAsync({
        cardId: newId,
        workflowStateId: colId,
        workflowState: {
          cardIds: [
            newId,
            ...(cols
              .find((c) => c.id === workflowState?.value)
              ?.cards?.filter(notEmpty)
              .map((c) => c.id) || [])
          ]
        },
        card: {
          ...cardInput.card,
          currentOwnerUsernames: owners?.map((o) => o.value),
          potentialClientIds: potentialClients?.map((client) => client.value),
          workflowProjectId: project?.value
        }
      }),
      {
        pending: 'Creating card...',
        success: 'Card created!',
        error: 'Error creating card'
      }
    )

    formHooks.resetField('card')
  }

  return (
    <ModalForm<AddHiringCardInput>
      title="Add Card"
      formHooks={formHooks}
      fields={[
        {
          id: 'CardState',
          label: 'Card State',
          rules: {
            required: true
          },
          options: stateOptions,
          path: 'workflowState',
          type: 'select'
        },
        {
          id: 'CardProject',
          type: 'select',
          options: projectOptions,
          label: 'Project',
          path: 'project'
        },
        {
          id: 'owners',
          label: 'Owners (people responsible for this card)',
          type: 'multiselect',
          options: usernameOptions,
          path: 'owners'
        },
        {
          id: 'potential clients',
          label: 'Potential Clients',
          type: 'multiselect',
          options: clientOptions,
          onCreateOption: handleCreateClient,
          isCreatable: true,
          path: 'potentialClients'
        },
        {
          label: 'CV PDF',
          id: 'CVPDF',
          type: 'file',
          accept: 'application/pdf',
          multiple: false,
          path: 'card.cvPdf'
        },
        {
          id: 'CardName',
          placeholder: 'Candidate Name',
          label: 'Name',
          type: 'text',
          rules: {
            required: true
          },
          path: 'card.title'
        },
        {
          id: 'CardDescription',
          label: 'Description',
          placeholder: 'Cover letter/initial email',
          type: 'tiptap',
          path: 'card.description'
        },
        {
          label: 'Source',
          id: 'Agent',
          type: 'text',
          path: 'card.agent'
        },
        {
          label: 'Location',
          id: 'Location',
          type: 'text',
          path: 'card.location'
        },
        {
          label: 'Has Remote.com fee',
          id: 'HasRemoteFee',
          type: 'checkbox',
          path: 'card.hasRemoteFee'
        },
        {
          label: 'Fast Tracked',
          id: 'FastTracked',
          type: 'checkbox',
          path: 'card.isFastTrack'
        },
        {
          label: 'Files',
          accept: 'image/jpeg, image/png, image/gif, application/pdf',
          id: 'CardFiles',
          type: 'multifile',
          path: 'card.files'
        }
      ]}
      onSubmit={formHooks.handleSubmit(AddHiringCard, console.warn)}
      isOpen={isOpen}
      handleClose={handleClose}
    />
  )
}
