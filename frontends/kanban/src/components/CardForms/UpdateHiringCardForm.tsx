import {
  LocationGenerics,
  useUpdateHiringCardMutation,
  useCardByIdsQuery,
  useMoveCardMutation,
  useWorkflowStates,
  useCardById,
  useProjectOptions,
  juxters,
  TWorkflowState,
  UpdateHiringCardMutationVariables,
  TDetailedCard,
  purgeAllLists,
  useClientOptions,
  useAddClientTags,
  useRejectionReasons,
  useAddRejectionReason
} from '../../site'
import {
  ArchiveInactiveIcon,
  ArchiveActiveIcon,
  Option,
  Form,
  Button,
  useDirty,
  RenderField
} from '../../ui-common'
import { defaultMutationProps } from '../../ui-kanban'
import { notEmpty, useMobileDetect } from '../../utils'
import _ from 'lodash'
import { BaseSyntheticEvent, useCallback, useEffect } from 'react'
import ReactDOMServer from 'react-dom/server'
import { useForm, useFormState } from 'react-hook-form'
import { useSearch } from '@tanstack/react-location'
import { useQueryClient } from 'react-query'
import { toast } from 'react-toastify'
import { workflowId } from '../constants'
import { UpdateHiringCardInput } from './types'

function onHiringCardUpdate(
  id: string | undefined,
  callback: (cardId: string) => void
) {
  if (id) {
    purgeAllLists()
    callback(id)
  }
}

export function UpdateHiringCardForm({
  handleClose
}: {
  handleClose: () => void
}) {
  const { modalState } = useSearch<LocationGenerics>()
  const cardId = modalState?.cardId
  const queryClient = useQueryClient()
  const UpdateHiringCardMutation = useUpdateHiringCardMutation({
    onSuccess: (data) => {
      const id = data.updateHiringCard?.id
      onHiringCardUpdate(id, (cId) =>
        queryClient.refetchQueries(useCardByIdsQuery.getKey({ ids: [cId] }))
      )
    }
  })
  const moveCardMutation = useMoveCardMutation({
    ...defaultMutationProps(queryClient, workflowId),
    onSuccess: (data) => {
      splitbee.track('Move Card From Form', {
        data: JSON.stringify(data)
      })
    }
  })

  const cols = useWorkflowStates({ workflowId }).data || []
  const stateOptions = cols.map((c) => ({
    label: c.name,
    value: c.id
  }))

  const { card } = useCardById(cardId)

  const UpdateHiringCard = (input: UpdateHiringCardInput) => {
    handleClose()
    const { workflowState, project, ...cardInput } = input
    const cardData = {
      card: {
        ...cardInput.card,
        workflowProjectId: project?.value,
        stateStr: workflowState?.value
      },
      cardId: input.cardId
    }

    const state = cols.find((c) => c.id === workflowState?.value)
    if (card && !_.isEqual(cardData.card, card)) {
      UpdateHiringCardMutation.mutate({
        card: cardData.card,
        cardId: input.cardId
      })
    }
    if (state && state.id !== card?.workflowState?.id) {
      moveCardMutation.mutate({
        workflowStateId: state.id,
        cardId: input.cardId,
        previousCard: 'end'
      })
    }
  }

  const formHooks = useForm<UpdateHiringCardInput>({
    defaultValues: {
      card,
      cardId: card?.id
    }
  })

  const options = [
    {
      label: 'Archive',
      id: 'archive',
      Icon: ArchiveInactiveIcon,
      ActiveIcon: ArchiveActiveIcon,
      props: {
        onClick: () => {
          handleClose()
          if (card?.id) {
            toast.promise(
              UpdateHiringCardMutation.mutateAsync({
                cardId: card.id,
                card: {
                  ...card,
                  workflowProjectId: null
                }
              }),
              {
                pending: 'Archiving card...',
                success: 'Card archived!',
                error: 'Error archiving card'
              }
            )
          }
        }
      }
    }
  ]

  useEffect(() => {
    const processCard = async () => {
      if (!card) return
      const processFiles = card.files?.filter(notEmpty).map(async (f) => {
        const previewUrl = f.name.startsWith('image') && f.base64
        return {
          ...f,
          preview: previewUrl
        }
      })
      const doneFiles = processFiles && (await Promise.all(processFiles))
      formHooks.setValue('card.files', doneFiles)
    }
    if (card) {
      formHooks.setValue('workflowState', {
        label: card?.workflowState?.name || 'Select a state',
        value: card?.workflowState?.id || ''
      })
      const workflowProjectId = card?.project?.id
      formHooks.setValue('card', { ...card })
      if (card?.files) processCard()
      formHooks.setValue('card.cvPdf', card?.cvPdf)
      if (card.project?.name && workflowProjectId) {
        formHooks.setValue('project', {
          label: card.project?.name,
          value: workflowProjectId
        })
      }
    }
  }, [card, formHooks])

  const title = card?.title
    ? `${card.title}: ${card.workflowState?.name}`
    : 'Update Card'
  const projectOptions = useProjectOptions(workflowId)
  return (
    <div className="relative h-full">
      <Form
        title={title}
        formHooks={formHooks}
        options={options}
        fields={[
          {
            id: 'CardName',
            placeholder: 'Card Name',
            type: 'text',
            rules: {
              required: true
            },
            path: 'card.title',
            label: 'Name'
          },
          {
            id: 'CardProject',
            type: 'select',
            rules: {
              required: {
                value: true,
                message: 'Please select a project'
              }
            },
            options: projectOptions,
            label: 'Project',
            path: 'project'
          },
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
            label: 'CV PDF',
            id: 'CVPDF',
            type: 'file',
            accept: 'application/pdf',
            multiple: false,
            path: 'card.cvPdf'
          },
          {
            label: 'Description',
            id: 'CardDescription',
            placeholder: 'Card Description',
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
            label: 'Take Home Test Language',
            id: 'TakeHomeTestLanguage',
            type: 'text',
            path: 'card.takeHomeLanguage'
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
            label: 'Location',
            id: 'Location',
            type: 'text',
            path: 'card.location'
          },
          {
            label: 'Other Files (optional)',
            accept: 'image/jpeg, image/png, image/gif, application/pdf',
            id: 'CardFiles',
            type: 'multifile',
            path: 'card.files'
          }
        ]}
        onSubmit={formHooks.handleSubmit(UpdateHiringCard, console.warn)}
        className="fixed-form-height overflow-y-auto"
      />
      <div className="fixed inset-x-0 bottom-0 bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
        <button
          type="submit"
          form={title}
          className="inline-flex w-full justify-center rounded-lg border border-transparent bg-blue-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm"
        >
          Submit
        </button>
        <button
          type="button"
          className="mt-3 inline-flex w-full justify-center rounded-lg border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm"
          onClick={handleClose}
        >
          Cancel
        </button>
      </div>
    </div>
  )
}

export function QuickEditCard({
  card,
  clientOptions,
  usernameOptions,
  projectOptions,
  stateOptions,
  cardClientIds,
  cols
}: {
  card: TDetailedCard
  clientOptions: Option[]
  usernameOptions: Option[]
  projectOptions: Option[]
  stateOptions: Option[]
  cardClientIds?: string[]
  cols: TWorkflowState[]
}) {
  const formHooks = useForm<UpdateHiringCardInput>({
    defaultValues: {
      card,
      cardId: card?.id,
      potentialClients: clientOptions.filter((client) =>
        cardClientIds?.includes(client.value)
      ),
      owners: usernameOptions.filter((user) =>
        card?.currentOwnerUsernames?.includes(user.value)
      ),
      project: projectOptions?.find((p) => p.value === card?.project?.id),
      workflowState: stateOptions?.find(
        (s) => s.value === card?.workflowState?.id
      )
    }
  })
  const reset = () => {
    formHooks.reset()
  }

  const queryClient = useQueryClient()
  const UpdateHiringCardMutation = useUpdateHiringCardMutation({
    onSuccess: (data) => {
      toast.success('Card updated!')
      const id = data.updateHiringCard?.id
      onHiringCardUpdate(id, (cardId: string) =>
        queryClient.refetchQueries(useCardByIdsQuery.getKey({ ids: [cardId] }))
      )
    },
    onError: (error) => {
      toast.error(`Error updating card ${error.message}`)
    }
  })
  const moveCardMutation = useMoveCardMutation({
    ...defaultMutationProps(queryClient, workflowId),
    onSuccess: (data) => {
      splitbee.track('Move Card From Quick Edit', {
        data: JSON.stringify(data)
      })
    }
  })

  const UpdateHiringCard = (input: UpdateHiringCardInput) => {
    const { workflowState, project, owners, potentialClients, ...cardInput } =
      input
    const cardData: UpdateHiringCardMutationVariables = {
      card: {
        ...cardInput.card,
        workflowProjectId: project?.value,
        stateStr: workflowState?.value,
        currentOwnerUsernames: owners?.map((o) => o.value),
        potentialClientIds: input.potentialClients?.map(
          (client) => client.value
        )
      },
      cardId: input.cardId
    }

    const newState = cols.find((c) => c.id === workflowState?.value)
    const oldState = cols.find((c) =>
      c.cards?.find((wfCard) => wfCard?.id === card.id)
    )
    if (card && !_.isEqual(cardData.card, card)) {
      UpdateHiringCardMutation.mutate({
        card: cardData.card,
        cardId: input.cardId
      })
    }
    if (newState && newState.id !== oldState?.id) {
      moveCardMutation.mutate({
        workflowStateId: newState.id,
        cardId: input.cardId,
        previousCard: 'end'
      })
    }
    formHooks.reset(input)
  }
  const onSubmit = formHooks.handleSubmit(UpdateHiringCard, () =>
    toast.error(`WoopsieDoopsy, the form is invalid`)
  )

  const isMobile = useMobileDetect().isMobile()

  const { isDirty } = useFormState(formHooks)

  const isEditing = isDirty

  useDirty({ isDirty })

  useEffect(() => {
    const listener = (event: KeyboardEvent) => {
      if (!isEditing || isMobile || event.isComposing) {
        return
      }

      if (event.code === 'KeyS' && (event.ctrlKey || event.metaKey)) {
        event.preventDefault()
        event.stopImmediatePropagation()
        onSubmit()
      }
      if (event.code === 'Esc' || event.code === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        reset()
      }
    }
    document.addEventListener('keydown', listener)
    return () => {
      document.removeEventListener('keydown', listener)
    }
  }, [onSubmit])

  const title = 'Quick Edit Card'

  const handleCreateClient = useAddClientTags()
  return (
    <div className="relative h-full w-full text-left">
      {isEditing && (
        <div className="fixed inset-x-0 top-0 z-20 bg-red-50 p-4">
          {isMobile ? (
            <div className="mx-1 flex w-full items-center justify-around">
              <strong className="prose">Editing Card</strong>
              <div className="flex w-2/5 flex-nowrap space-x-4">
                <Button primary onClick={onSubmit}>
                  Save
                </Button>
                <Button
                  onClick={() => {
                    reset()
                  }}
                >
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <p className="text-center">
              Editing... Press ESC to cancel or Ctrl/Cmd+S to save
            </p>
          )}
        </div>
      )}
      <Form
        title={title}
        formHooks={formHooks}
        fields={[
          {
            id: 'owners',
            label: 'Owners',
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
            id: 'CardProject',
            type: 'select',
            rules: {
              required: {
                value: true,
                message: 'Please select a project'
              }
            },
            options: projectOptions,
            label: 'Project',
            path: 'project'
          },
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
            label: 'Agent',
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
            label: 'Take Home Test Language',
            id: 'TakeHomeTestLanguage',
            type: 'text',
            path: 'card.takeHomeLanguage'
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
          }
        ]}
        className="fixed-form-height overflow-y-auto"
      />
    </div>
  )
}

export function QuickEditCardWrapper({ cardId }: { cardId: string }) {
  const { card } = useCardById(cardId)
  const cols = useWorkflowStates({ workflowId }).data || []
  const stateOptions = cols.map((c) => ({
    label: c.name,
    value: c.id
  }))
  const projectOptions = useProjectOptions(workflowId)
  const usernameOptions = juxters.map((user) => ({
    label: user.name,
    value: user.staffRecord.juxtcode
  }))
  const { data: clientOptions } = useClientOptions()
  const cardClientIds = card?.potentialClients
    ?.map((c) => c?.id)
    .filter(notEmpty)

  return (
    <>
      {card && clientOptions && (
        <QuickEditCard
          card={card}
          clientOptions={clientOptions}
          usernameOptions={usernameOptions}
          projectOptions={projectOptions}
          stateOptions={stateOptions}
          cardClientIds={cardClientIds}
          cols={cols}
        />
      )}
    </>
  )
}

export function RejectionPanel({
  card,
  oldState,
  rejectionReasons
}: {
  card: TDetailedCard
  oldState: TWorkflowState
  rejectionReasons: Option[]
}) {
  const rejectedState = {
    value: 'WorkflowStateRejected'
  }
  const formHooks = useForm<UpdateHiringCardInput>({
    defaultValues: {
      card,
      cardId: card?.id,
      workflowState: rejectedState,
      rejectionReasons: rejectionReasons.filter((reason) =>
        card.rejectionReasons?.map((r) => r?.id)?.includes(reason.value)
      )
    }
  })
  const queryClient = useQueryClient()

  const UpdateHiringCardMutation = useUpdateHiringCardMutation({
    onSuccess: (data) => {
      toast.success('Card updated!')
      const id = data.updateHiringCard?.id
      onHiringCardUpdate(id, (cardId: string) =>
        queryClient.refetchQueries(useCardByIdsQuery.getKey({ ids: [cardId] }))
      )
    },
    onError: (error) => {
      toast.error(`Error updating card ${error.message}`)
    }
  })

  const moveCardMutation = useMoveCardMutation({
    ...defaultMutationProps(queryClient, workflowId),
    onSuccess: (data) => {
      splitbee.track('Reject Card', {
        data: JSON.stringify(data)
      })
    }
  })

  const UpdateHiringCard = (input: UpdateHiringCardInput) => {
    const { workflowState, ...cardInput } = input
    const cardData: UpdateHiringCardMutationVariables = {
      card: {
        ...cardInput.card,
        stateStr: workflowState?.value,
        rejectionReasonIds: input.rejectionReasons?.map(
          (reason) => reason.value
        )
      },
      cardId: input.cardId
    }

    if (card && !_.isEqual(cardData.card, card)) {
      UpdateHiringCardMutation.mutate({
        card: cardData.card,
        cardId: input.cardId
      })
    }
    if (rejectedState && rejectedState.value !== oldState?.id) {
      moveCardMutation.mutate({
        workflowStateId: rejectedState.value,
        cardId: input.cardId,
        previousCard: 'end'
      })
    }
    formHooks.reset(input)
  }
  const onSubmit = formHooks.handleSubmit(UpdateHiringCard, () =>
    toast.error(`WoopsieDoopsy, the form is invalid`)
  )

  const { isDirty } = useFormState(formHooks)

  useDirty({ isDirty })

  const handleRejectionReasonCreate = useAddRejectionReason()
  const rejected =
    oldState.id === 'WorkflowStateToReject' ||
    oldState.id === 'WorkflowStateRejected'

  return (
    <div className="relative h-full w-full text-left">
      {rejected ? (
        <div className="flex flex-col">
          rejected reasons:
          <ul>
            {card.rejectionReasons?.filter(notEmpty).map((reason) => (
              <li key={reason.id}>{reason.name}</li>
            ))}
          </ul>
          details:
          <p>{card.rejectionFeedback}</p>
        </div>
      ) : (
        <>
          <Form
            id="rejection-form"
            title="Reject Card"
            formHooks={formHooks}
            fields={[
              {
                id: 'reason',
                label: 'Rejection Reasons',
                type: 'multiselect',
                options: rejectionReasons,
                onCreateOption: handleRejectionReasonCreate,
                isCreatable: true,
                path: 'rejectionReasons'
              },
              {
                id: 'feedback',
                label: 'Rejection Feedback',
                type: 'textarea',
                path: 'card.rejectionFeedback'
              }
            ]}
          />
          <div className="px-6">
            <Button onClick={onSubmit} red>
              Reject
            </Button>
          </div>
        </>
      )}
    </div>
  )
}

export function RejectionPanelWrapper({ cardId }: { cardId: string }) {
  const { card } = useCardById(cardId)
  const { data: rejectionReasons } = useRejectionReasons()
  const cols = useWorkflowStates({ workflowId }).data || []
  const oldState = cols.find((c) =>
    c.cards?.find((wfCard) => wfCard?.id === card?.id)
  )
  return (
    <>
      {card && rejectionReasons && oldState && (
        <RejectionPanel
          card={card}
          rejectionReasons={rejectionReasons}
          oldState={oldState}
        />
      )}
    </>
  )
}

function tasksToDefaultContentJSX(state: TWorkflowState) {
  return (
    <ul data-type="taskList">
      {state.tasks?.map((task) => (
        <li key={state.id + task} data-type="taskItem" data-checked="false">
          {task}
        </li>
      ))}
    </ul>
  )
}

type TaskForm = { content: string }

export function TaskListForState({ card }: { card: TDetailedCard }) {
  const state = card?.workflowState
  const content = card?.taskHtml
    ? card.taskHtml
    : state && state.tasks
    ? ReactDOMServer.renderToString(tasksToDefaultContentJSX(state))
    : 'no state'

  const formHooks = useForm<TaskForm>({
    defaultValues: { content }
  })
  useDirty({ isDirty: formHooks.formState.isDirty })
  const queryClient = useQueryClient()
  const updateCard = useUpdateHiringCardMutation({
    onSuccess: (data) => {
      toast.success('Card updated!')
      splitbee.track('tasklist update', {
        cardId: data.updateHiringCard?.id,
        task: content
      })
      const id = data.updateHiringCard?.id
      if (id) {
        queryClient.refetchQueries(useCardByIdsQuery.getKey({ ids: [id] }))
      }
    },
    onError: (error) => {
      toast.error(`Error updating card ${error.message}`)
    }
  })

  const handleUpdateCard = useCallback(
    ({ content: input }: TaskForm) => {
      if (input && input !== card.taskHtml) {
        updateCard.mutate({
          cardId: card.id,
          card: {
            taskHtml: input
          }
        })
      }
    },
    [formHooks]
  )

  const reset = () => {
    if (state) {
      formHooks.reset({
        content
      })
    }
  }

  useEffect(() => {
    reset()
  }, [content])

  const handleSubmit = useCallback((e?: BaseSyntheticEvent) => {
    if (!formHooks.formState.isDirty) {
      e?.preventDefault()
      return
    }
    formHooks.handleSubmit(handleUpdateCard, () =>
      toast.error('Error updating card...')
    )(e)
  }, [])
  return (
    <form onSubmit={handleSubmit}>
      <RenderField<TaskForm>
        props={{
          formHooks
        }}
        field={{
          type: 'tiptap',
          path: 'content',
          label: 'Tasks',
          unstyled: true,
          id: 'task-list',
          withTaskListExtension: true
        }}
      />
      {formHooks.formState.isDirty && (
        <div className="block justify-end sm:flex">
          <Button className="my-2 sm:my-0 sm:mx-2" primary type="submit">
            Save
          </Button>
          <Button
            className="inline-flex w-full justify-center sm:w-auto"
            onClick={() => reset()}
            type="reset"
          >
            Cancel
          </Button>
        </div>
      )}
    </form>
  )
}
