import { NavTabs } from '../ui-common'
import { notEmpty } from '../utils'
import { useSearch } from '@tanstack/react-location'
import {
  LocationGenerics,
  useCardByIdsQuery,
  useKanbanDataQuery,
  useModalForm,
  useUser
} from '../site'
import {
  AddProjectModal,
  UpdateWorkflowProjectModalWrapper,
  Workflow,
  AddWorkflowStateModal,
  UpdateWorkflowStateModalWrapper,
  ViewCommentsModal
} from '../ui-kanban'
import {
  AddHiringCardModalWrapper as AddHiringCardModal,
  EditHiringCardModal as HiringCardModal
} from './CardForms'
import { workflowId } from './constants'
import { useQueryClient } from 'react-query'
import { useCallback, useEffect } from 'react'

export function App() {
  const search = useSearch<LocationGenerics>()
  const isDev = process.env.NODE_ENV === 'development'
  const refetch = search.modalState?.formModalType ? false : isDev ? 5000 : 1000
  const queryClient = useQueryClient()
  const kanbanQueryResult = useKanbanDataQuery(
    { id: workflowId },
    {
      refetchInterval: refetch
    }
  )
  const cardIds = kanbanQueryResult.data?.workflow?.workflowStates
    ?.flatMap((ws) => ws?.cards?.map((card) => card?.id))
    .filter(notEmpty)
  const { id: username } = useUser()

  const prefetchCardDetails = useCallback(() => {
    if (username === 'alx') {
      cardIds?.forEach((cardId, idx) => {
        if (cardId && !isDev) {
          setTimeout(() => {
            queryClient.prefetchQuery(
              ['cardById', { ids: [cardId] }],
              useCardByIdsQuery.fetcher({ ids: [cardId] })
            )
          }, idx * 20)
        }
      })
    }
  }, [cardIds, queryClient, isDev])

  useEffect(() => prefetchCardDetails(), [])

  const workflow = kanbanQueryResult.data?.workflow
  const [isModalOpen, setIsModalOpen] = useModalForm({
    formModalType: 'addWorkflowState'
  })
  const [isCardModalOpen, setIsCardModalOpen] = useModalForm({
    formModalType: 'editCard'
  })
  const [isWorkflowStateModalOpen, setIsWorkflowStateModalOpen] = useModalForm({
    formModalType: 'editWorkflowState'
  })

  const [isAddCard, setIsAddCard] = useModalForm({
    formModalType: 'addCard'
  })
  const [isAddProject, setIsAddProject] = useModalForm({
    formModalType: 'addProject'
  })
  const [isViewComments, setIsViewComments] = useModalForm({
    formModalType: 'viewComments'
  })
  const [isEditProject, setIsEditProject] = useModalForm({
    formModalType: 'editProject'
  })
  const projects = kanbanQueryResult.data?.allWorkflowProjects || []
  return (
    <>
      {kanbanQueryResult.isLoading && <div>Loading cards...</div>}
      <AddWorkflowStateModal
        isOpen={!!isModalOpen}
        workflowId={workflowId}
        handleClose={() => setIsModalOpen(false)}
      />
      <UpdateWorkflowStateModalWrapper
        isOpen={!!isWorkflowStateModalOpen}
        workflowId={workflowId}
        handleClose={() => setIsWorkflowStateModalOpen(false)}
      />
      <HiringCardModal
        isOpen={isCardModalOpen}
        handleClose={() => setIsCardModalOpen(false)}
      />
      <AddHiringCardModal
        isOpen={isAddCard}
        handleClose={() => setIsAddCard(false)}
      />
      <AddProjectModal
        isOpen={isAddProject}
        handleClose={() => setIsAddProject(false)}
      />
      <ViewCommentsModal
        isOpen={isViewComments}
        handleClose={() => setIsViewComments(false)}
      />
      <UpdateWorkflowProjectModalWrapper
        isOpen={isEditProject}
        workflowId={workflowId}
        handleClose={() => setIsEditProject(false)}
      />
      <NavTabs
        navName="workflowProjectIds"
        tabs={[...projects, { id: '', name: 'All' }]
          .filter(notEmpty)
          .map((project) => ({
            id: project.id,
            name: project.name,
            count:
              workflow?.workflowStates.reduce(
                (acc, ws) =>
                  acc +
                  (ws?.cards?.filter((c) => {
                    if (project.id === '') {
                      return c?.project?.id
                    }
                    return project?.id && c?.project?.id === project.id
                  })?.length || 0),
                0
              ) || 0
          }))
          .filter((p) => p.count > 0)}
      />

      {workflow && <Workflow key={workflow.id} workflow={workflow} />}
    </>
  )
}
