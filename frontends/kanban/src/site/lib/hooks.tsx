import { notEmpty } from '../../utils'
import { atom, useAtom } from 'jotai'
import { useEffect } from 'react'
import { DraggableLocation } from 'react-beautiful-dnd'
import { useNavigate, useSearch } from '@tanstack/react-location'
import { UseQueryOptions } from 'react-query'
import { toast } from 'react-toastify'
import {
  useKanbanDataQuery,
  CardByIdsQuery,
  useCardByIdsQuery,
  useCommentsForCardQuery,
  CardHistoryQuery,
  useCardHistoryQuery,
  LocationGenerics,
  CommentsForCardQuery,
  TWorkflow,
  TWorkflowState,
  useMoveCardMutation,
  userAvatar,
  useUpdateHiringCardMutation
} from '..'
import {
  useAllClientsQuery,
  useAllRejectionReasonsQuery,
  useCreateClientMutation,
  useCreateRejectionReasonMutation
} from '../generated/graphql'

type ModalState = LocationGenerics['Search']['modalState']

export function useModalForm(
  modalState: ModalState
): [boolean, (shouldOpen: boolean) => void] {
  const { modalState: currentModalState, ...search } =
    useSearch<LocationGenerics>()
  const navigate = useNavigate()
  const isModalOpen =
    currentModalState?.formModalType === modalState.formModalType

  const setIsModalOpen = (shouldOpen: boolean) => {
    if (shouldOpen) {
      navigate({
        replace: true,
        search: (search) => ({
          ...search,
          modalState: { ...currentModalState, ...modalState }
        })
      })
    } else {
      navigate({
        replace: true,
        search: {
          ...search
        }
      })
    }
  }
  return [isModalOpen, setIsModalOpen]
}

export function useWorkflowStates({ workflowId }: { workflowId: string }) {
  const workflowStateResult = useKanbanDataQuery(
    { id: workflowId },
    {
      select: (data) => data?.workflow?.workflowStates.filter(notEmpty)
    }
  )
  return workflowStateResult
}

export function useStatesOptions({
  workflowId
}: {
  workflowId: string
}): [ReturnType<typeof useWorkflowStates>, { value: string; label: string }[]] {
  const workflowStateResult = useWorkflowStates({ workflowId })
  const cols =
    workflowStateResult?.data?.map((c) => ({
      value: c.id,
      label: c.name
    })) || []
  return [workflowStateResult, cols]
}

export function useWorkflowState(workflowId: string, wsId?: string) {
  const workflowStateResult = useKanbanDataQuery(
    { id: workflowId },
    {
      select: (data) =>
        data?.workflow?.workflowStates
          ?.filter(notEmpty)
          .find((s) => s.id === wsId)
    }
  )
  return workflowStateResult
}

export function useProjectOptions(workflowId: string) {
  const kanbanDataQuery = useKanbanDataQuery(
    { id: workflowId },
    {
      select: (data) => data?.allWorkflowProjects?.filter(notEmpty)
    }
  )
  return (
    kanbanDataQuery?.data?.map((p) => ({
      label: p.name,
      value: p.id
    })) ?? []
  )
}

export function useRejectionReasons() {
  const clients = useAllRejectionReasonsQuery(undefined, {
    select: (data) =>
      data?.allRejectionReasons?.filter(notEmpty).map((reason) => ({
        label: reason.name,
        value: reason.id
      })) ?? []
  })
  return clients
}

export function useClientOptions() {
  const clients = useAllClientsQuery(undefined, {
    select: (data) =>
      data?.allClients?.filter(notEmpty).map((client) => ({
        label: client.name,
        value: client.id
      })) ?? []
  })
  return clients
}

export function useCurrentProject(workflowId: string) {
  const workflowProjectIds = useSearch<LocationGenerics>().workflowProjectIds
  const projectQuery = useKanbanDataQuery(
    { id: workflowId },
    {
      select: (data) =>
        data?.allWorkflowProjects
          ?.filter(notEmpty)
          .find((p) => workflowProjectIds?.includes(p.id))
    }
  )
  return projectQuery
}

export function useCardById(
  cardId?: string,
  opts?: UseQueryOptions<CardByIdsQuery, Error, CardByIdsQuery>
) {
  const queryResult = useCardByIdsQuery(
    { ids: [cardId || ''] },
    {
      ...opts,
      select: (data) => ({
        ...data,
        cardsByIds: data?.cardsByIds?.filter(notEmpty)
      }),
      enabled: !!cardId,
      staleTime: 5000
    }
  )
  return { ...queryResult, card: queryResult.data?.cardsByIds?.[0] }
}

export function useCommentForEntity(
  { asOf, eId }: { asOf?: string; eId: string },
  opts: UseQueryOptions<CommentsForCardQuery, Error, CommentsForCardQuery> = {}
) {
  const query = useCommentsForCardQuery(
    { id: eId, asOf },
    {
      ...opts,
      select: (data) => ({
        ...data,
        commentsForEntity: data?.commentsForEntity
          ?.filter(notEmpty)
          .filter((c) => !c?.parentId)
      })
    }
  )
  return query
}

export function useCardHistory(
  cardId?: string,
  opts?: UseQueryOptions<CardHistoryQuery, Error, CardHistoryQuery>
) {
  const queryResult = useCardHistoryQuery(
    { id: cardId || '', historicalDb: true },
    {
      ...opts,
      select: (data) => ({
        ...data,
        cardHistory: data?.cardHistory?.filter((card) => card?._siteValidTime)
      }),
      enabled: !!cardId,
      staleTime: 5000
    }
  )
  return { ...queryResult, history: queryResult.data?.cardHistory }
}

export function useUser() {
  // should probably make a user query
  const { data } = useKanbanDataQuery(
    { id: 'WorkflowHiring' },
    {
      select: (data) => data?.myJuxtcode,
      staleTime: Infinity
    }
  )
  const userImg = userAvatar(data)

  return {
    id: data,
    avatar: userImg || ''
  }
}

export function useMoveCard({ handleSuccess }: { handleSuccess: () => void }) {
  const moveCardMutation = useMoveCardMutation({
    onSuccess: handleSuccess
  })
  const updateCardMutation = useUpdateHiringCardMutation()
  const updateServerCards = (
    state: TWorkflow,
    startCol: TWorkflowState,
    endCol: TWorkflowState,
    source: DraggableLocation,
    destination: DraggableLocation,
    draggableId: string,
    prevCardId?: string | false
  ) => {
    if (startCol === endCol) {
      moveCardMutation.mutate({
        workflowStateId: startCol.id,
        sameColMove: true,
        cardId: draggableId,
        previousCard: prevCardId || 'start'
      })
    } else if (endCol) {
      updateCardMutation.mutate({
        cardId: draggableId,
        card: {
          stateStr: endCol.id
        }
      })

      moveCardMutation.mutate({
        workflowStateId: endCol?.id,
        cardId: draggableId,
        previousCard: prevCardId || 'start'
      })
    }
  }
  return [updateServerCards]
}

export const asOfAtom = atom<string | undefined>(undefined)

export function useAsOf({
  validTime
}: {
  validTime?: string
}): [string | undefined, (v: string | undefined) => void] {
  const [asOf, setAsOf] = useAtom(asOfAtom)
  useEffect(() => {
    setAsOf(validTime)
  }, [validTime, setAsOf])
  return [asOf, setAsOf]
}

export async function purgeQueries(queries: string[]) {
  const isDev = process.env['NODE_ENV'] === 'development'
  if (!isDev) {
    const res = await fetch('https://admin.graphcdn.io/kanban', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'graphcdn-token':
          'b70c77f7c5eff9dd0ec598eb2043277499295c34989d459a862ef77ea3c843e4'
      },
      body: JSON.stringify({
        query: `mutation purgeCols { _purgeQuery(queries: [${queries}]) }`
      })
    })
  } else {
    console.log('not purging in dev')
  }
}

export async function purgeAllLists() {
  purgeQueries([
    'allComments',
    'allWorkflows',
    'allHiringCards',
    'allWorkflowStates',
    'allWorkflowProjects',
    'workflow',
    'cardsForProject',
    'commentsForEntity',
    'feedbackForCard'
  ])
}

export function useAddClientTags(): (tag: string) => void {
  const createTagMutation = useCreateClientMutation({
    onSuccess: () => {
      purgeQueries(['allClients'])
      toast.success('Tag created')
    }
  })
  const addTag = (tag: string) => {
    const tagStr = tag.toLocaleLowerCase().trim()
    const id = `Client-${tagStr}`
    if (tagStr) {
      createTagMutation.mutate({
        client: {
          id,
          name: tagStr
        }
      })
    }
    return {
      label: tagStr,
      value: id
    }
  }
  return addTag
}

export function useAddRejectionReason(): (tag: string) => void {
  const createTagMutation = useCreateRejectionReasonMutation({
    onSuccess: () => {
      purgeQueries(['allRejectionReasons'])
      toast.success('New rejection reason created')
    }
  })
  const addTag = (tag: string) => {
    const tagStr = tag.toLocaleLowerCase().trim()
    const id = `RejectionReason-${tagStr}`
    if (tagStr) {
      createTagMutation.mutate({
        reason: {
          id,
          name: tagStr
        }
      })
    }
    return {
      label: tagStr,
      value: id
    }
  }
  return addTag
}
