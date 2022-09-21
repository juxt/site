import { DraggableLocation } from 'react-beautiful-dnd'
import { QueryClient } from 'react-query'
import {
  WorkflowStateFieldsFragment as TWorkflowState,
  useKanbanDataQuery,
  TWorkflow,
  TCard,
  purgeAllLists
} from '../site'
import { notEmpty } from '../utils'

export function filteredCols({
  filteredState,
  devMode
}: {
  filteredState: TWorkflow
  devMode: boolean
}) {
  const cols =
    filteredState?.workflowStates.filter(notEmpty).map((c) => ({
      ...c,
      cards:
        c.cards?.filter((card) => devMode || card?.project).filter(notEmpty) ||
        []
    })) || []
  return cols
}

export function filteredCards(
  cards: TCard[] | undefined,
  workflowProjectIds: string[] | undefined,
  searchTerm: string
) {
  return (
    cards
      ?.filter((c) => c?.title)
      .filter((card) => {
        const hasProject =
          (!workflowProjectIds && card?.project) ||
          (card?.project?.name &&
            workflowProjectIds?.includes(card.project?.id))
        return searchTerm.length < 2
          ? hasProject
          : card.title.toLowerCase().includes(searchTerm.toLowerCase())
      }) ?? []
  )
}

export function immutableMove<T>(arr: Array<T>, from: number, to: number) {
  return arr.reduce((prev, current, idx, self) => {
    if (from === to) {
      prev.push(current)
    }
    if (idx === from) {
      return prev
    }
    if (from < to) {
      prev.push(current)
    }
    if (idx === to) {
      prev.push(self[from])
    }
    if (from > to) {
      prev.push(current)
    }
    return prev
  }, [] as Array<T>)
}

function addInArrayAtPosition(
  array: TCard[],
  element: TCard,
  position: number
) {
  const arrayCopy = [...array]
  arrayCopy.splice(position, 0, element)
  return arrayCopy
}

function removeFromArrayAtPosition(array: TCard[], position: number) {
  return array.filter((_, index) => index !== position)
}

function reorderCardsOnWorkflowState(
  workflowState: TWorkflowState,
  reorderCards: (cards: TCard[]) => TCard[]
): TWorkflowState {
  if (!workflowState?.cards) return workflowState
  return {
    ...workflowState,
    cards: reorderCards(workflowState.cards.filter(notEmpty))
  }
}

function hasDuplicateCards(col: TWorkflowState) {
  const cards = col?.cards?.filter(notEmpty) ?? []
  if (cards.length > 0) {
    return cards.filter(notEmpty).some((card, index) => {
      return cards.findIndex((c) => c.id === card.id) !== index
    })
  }
  return null
}

function removeDuplicateCards(state: TWorkflow, cardList: string[]) {
  return cardList.filter((cardId) => {
    const col = state?.workflowStates.find((stateCol) =>
      stateCol?.cards?.find((c) => c?.id === cardId)
    )
    return col?.cards?.find((c) => c?.id === cardId) === undefined
  })
}

function moveCard(
  workflow: TWorkflow,
  { index: fromPosition, droppableId: fromWorkflowStateId }: DraggableLocation,
  { index: toPosition, droppableId: toWorkflowStateId }: DraggableLocation
) {
  if (!workflow) return null
  const cols = workflow.workflowStates.filter(notEmpty).map((col) => {
    return {
      ...col,
      cards: col.cards?.filter(notEmpty) ?? []
    }
  })
  const sourceWorkflowState = cols.find(
    (workflowState) => workflowState.id === fromWorkflowStateId
  )
  const destinationWorkflowState = cols.find(
    (workflowState) => workflowState.id === toWorkflowStateId
  )

  if (!sourceWorkflowState || !destinationWorkflowState) return workflow

  const reorderWorkflowStatesOnWorkflow = (
    reorderWorkflowStatesMapper: (col: TWorkflowState) => TWorkflowState
  ) => ({
    ...workflow,
    workflowStates: cols.map(reorderWorkflowStatesMapper)
  })

  if (sourceWorkflowState.id === destinationWorkflowState.id) {
    const reorderedCardsOnWorkflowState = reorderCardsOnWorkflowState(
      sourceWorkflowState,
      (cards: TCard[]) => {
        return immutableMove(cards, fromPosition, toPosition)
      }
    )
    return reorderWorkflowStatesOnWorkflow((workflowState: TWorkflowState) =>
      workflowState.id === sourceWorkflowState.id
        ? reorderedCardsOnWorkflowState
        : workflowState
    )
  }
  const reorderedCardsOnSourceWorkflowState = reorderCardsOnWorkflowState(
    sourceWorkflowState,
    (cards: TCard[]) => {
      return removeFromArrayAtPosition(cards, fromPosition)
    }
  )
  const reorderedCardsOnDestinationWorkflowState = reorderCardsOnWorkflowState(
    destinationWorkflowState,
    (cards: TCard[]) => {
      return addInArrayAtPosition(
        cards,
        sourceWorkflowState.cards[fromPosition],
        toPosition
      )
    }
  )
  return reorderWorkflowStatesOnWorkflow((workflowState: TWorkflowState) => {
    if (workflowState.id === sourceWorkflowState.id)
      return reorderedCardsOnSourceWorkflowState
    if (workflowState.id === destinationWorkflowState.id)
      return reorderedCardsOnDestinationWorkflowState
    return workflowState
  })
}

const defaultMutationProps = (
  queryClient: QueryClient,
  workflowId: string
) => ({
  onSettled: () => {
    queryClient.refetchQueries(useKanbanDataQuery.getKey({ id: workflowId }))
    purgeAllLists()
  }
})

export {
  defaultMutationProps,
  hasDuplicateCards,
  removeDuplicateCards,
  moveCard
}
