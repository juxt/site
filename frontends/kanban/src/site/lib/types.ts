import { MakeGenerics } from '@tanstack/react-location'
import {
  KanbanDataQuery,
  CardFieldsFragment,
  WorkflowStateFieldsFragment,
  CardByIdsQuery,
  RecentCommentsQuery
} from '../generated/graphql'

export type TWorkflow = NonNullable<KanbanDataQuery['workflow']>
export type TCard = CardFieldsFragment
export type TDetailedCard = NonNullable<
  NonNullable<CardByIdsQuery['cardsByIds']>[0]
>
export type TWorkflowState = WorkflowStateFieldsFragment
export type TComment = NonNullable<
  NonNullable<RecentCommentsQuery['allComments']>[0]
>
export type TProject = NonNullable<
  NonNullable<KanbanDataQuery['allWorkflowProjects']>[0]
>

export type WorkflowFormModalTypes =
  | 'addCard'
  | 'addProject'
  | 'addWorkflowState'
  | 'addWorkflow'
  | 'editProject'
  | 'editWorkflowState'
  | 'editCard'
  | 'viewComments'
  | null
  | false
  | undefined

export type LocationGenerics = MakeGenerics<{
  Search: {
    modalState: {
      formModalType: WorkflowFormModalTypes
      workflowProjectId?: string
      workflowStateId?: string
      cardId?: string
      isEditing?: boolean
    }
    interviewCardId?: string
    hideEmptyStates?: boolean
    workflowProjectIds?: Array<string>
    showMyCards?: boolean
    cardModalView?: string
    view?: 'card' | 'table'
    filters?: {
      [key: string]: string[]
    }
    showDetails?: boolean
    devMode?: boolean
  }
}>
