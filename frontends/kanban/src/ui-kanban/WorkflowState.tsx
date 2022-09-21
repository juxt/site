import { notEmpty, take } from '../utils'
import { Draggable, Droppable, DroppableProvided } from 'react-beautiful-dnd'
import Tippy, { useSingleton, TippyProps } from '@tippyjs/react'
import 'tippy.js/dist/tippy.css' // optional
import classNames from 'classnames'
import { useSearch } from '@tanstack/react-location'
import NaturalDragAnimation from './lib/react-dnd-animation'
import {
  TCard,
  TWorkflow,
  TWorkflowState,
  LocationGenerics,
  useModalForm,
  useUser,
  useDeleteCardFromColumnMutation,
  juxters
} from '../site'
import { memo } from 'react'
import { ArchiveActiveIcon, IconForScore, searchAtom } from '../ui-common'
import { useAtom } from 'jotai'
import { FastForwardIcon } from '@heroicons/react/solid'

type CardProps = {
  card: TCard
  workflow: TWorkflow
  index: number
}

const DraggableCard = memo(({ card, index, workflow }: CardProps) => {
  const workflowState = workflow?.workflowStates.find((state) =>
    state?.cards?.find((c) => c?.id === card.id)
  )
  const [, setIsOpen] = useModalForm({
    formModalType: 'editCard',
    cardId: card.id,
    workflowStateId: workflowState?.id
  })
  const search = useSearch<LocationGenerics>()
  const showMyCards = search?.showMyCards
  const { id: username } = useUser()
  const isMyCard =
    showMyCards && username && card?.currentOwnerUsernames?.includes(username)
  const updateState = useDeleteCardFromColumnMutation()
  const handleDeleteCard = (cardId: string) => {
    const id = workflowState?.id
    if (id && workflowState.cards) {
      updateState.mutate({
        cardId,
        workflowStateId: id
      })
    }
  }

  return (
    <Draggable draggableId={card.id} index={index}>
      {(provided, snapshot) => {
        const isDragging = snapshot.isDragging && !snapshot.isDropAnimating
        const cardStyles = classNames(
          'text-left relative bg-white card-width rounded border-2 mb-2 p-2 border-gray-500 hover:border-blue-400',
          isDragging && 'bg-blue-50 border-blue-400 shadow-lg',
          isMyCard && 'border-green-400',
          !card?.project && 'border-red-500 bg-red-50'
        )
        const owners = juxters.filter((j) =>
          card?.currentOwnerUsernames?.includes(j.staffRecord.juxtcode)
        )

        const totalFeedbacks = card.interviewFeedback?.length || 0
        const totalScores = card.interviewFeedback
          ?.filter(notEmpty)
          ?.map((f) => f.overallScore)
          ?.reduce((acc, curr) => acc + curr || 0, 0)
        const averageScore =
          totalScores && totalFeedbacks
            ? Math.floor(totalScores / totalFeedbacks)
            : undefined

        return (
          <NaturalDragAnimation
            style={provided.draggableProps.style}
            snapshot={snapshot}
          >
            {(style: object) => (
              <div
                {...provided.draggableProps}
                {...provided.dragHandleProps}
                style={style}
                className={cardStyles}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    setIsOpen(true)
                  }
                }}
                onClick={() => workflow?.id && setIsOpen(true)}
                ref={provided.innerRef}
              >
                {search?.devMode && <pre className="truncate">{card.id}</pre>}
                {search?.devMode && (
                  <ArchiveActiveIcon
                    onClick={(e) => {
                      e.preventDefault()
                      e.stopPropagation()
                      handleDeleteCard(card.id)
                    }}
                    className="absolute top-0 right-0 h-5 w-5"
                  />
                )}
                <div className="flex justify-between">
                  <div className="flex space-x-2">
                    <p className="text-sm font-extralight uppercase text-gray-800">
                      {card.project?.name}
                    </p>
                    {card.hasRemoteFee && (
                      <p className="text-sm font-extralight text-red-800">
                        Remote
                      </p>
                    )}
                    {averageScore && (
                      <IconForScore size="sm" score={averageScore} />
                    )}
                  </div>

                  {owners.length > 0 && (
                    <div className="flex">
                      {take(owners, 3).map((o) => (
                        <Tippy
                          key={o.avatar}
                          theme="light"
                          content={o.name}
                          placement="top"
                        >
                          <img
                            className={classNames('w-6 h-6 rounded-full mr-2')}
                            src={o.avatar}
                            alt="card owner"
                          />
                        </Tippy>
                      ))}
                    </div>
                  )}
                </div>
                {card?.takeHomeLanguage && (
                  <p className="text-sm font-extralight text-gray-800">
                    Take Home language - {card.takeHomeLanguage}
                  </p>
                )}

                <p className="prose lg:prose-xl">{card.title}</p>
                {card.isFastTrack && (
                  <p className="flex items-center text-sm font-extralight text-blue-800">
                    <FastForwardIcon className="mr-2 h-4 w-4" /> Fast Track
                  </p>
                )}
              </div>
            )}
          </NaturalDragAnimation>
        )
      }}
    </Draggable>
  )
})

type WorkflowStateProps = {
  workflowState: TWorkflowState
  isDragging: boolean
  cards: TCard[]
  workflow: NonNullable<TWorkflow>
  tooltipTarget: TippyProps['singleton']
}

const WorkflowState = memo(
  ({ workflowState, cards, workflow, tooltipTarget }: WorkflowStateProps) => {
    const isFirst = workflow.workflowStates?.[0]?.id === workflowState.id
    const [, setIsOpen] = useModalForm({
      formModalType: 'editWorkflowState',
      workflowStateId: workflowState.id
    })
    return (
      <Droppable droppableId={workflowState.id} type="card">
        {(provided, snapshot) => (
          <div
            style={{
              borderColor: snapshot.isDraggingOver ? 'gray' : 'transparent'
            }}
            className={classNames(
              'transition sm:mx-1 border-4 h-full',
              isFirst && 'sm:ml-0',
              snapshot.isDraggingOver && 'bg-blue-50 shadow-sm  border-dashed ',
              ' flex flex-col ',
              cards.length === 0 && 'relative h-36'
            )}
          >
            <button
              type="button"
              onClick={() => workflow?.id && setIsOpen(true)}
              className={classNames(
                'prose cursor-pointer isolate default-tippy-tooltip',
                cards.length === 0 &&
                  !snapshot.isDraggingOver &&
                  'sm:transition sm:rotate-90 sm:relative sm:top-2 sm:left-10 sm:origin-top-left sm:whitespace-nowrap'
              )}
            >
              {cards.length === 0 && !snapshot.isDraggingOver ? (
                <>
                  <h3 className="text-white">col</h3>
                  <h3 className="absolute top-2">{workflowState.name}</h3>
                </>
              ) : (
                <Tippy
                  singleton={tooltipTarget}
                  delay={[100, 500]}
                  className=" relative whitespace-normal rounded bg-slate-800 p-2 text-center text-sm text-white outline-none transition-all"
                  content={
                    <div className="text-sm">
                      <strong>{workflowState.name}</strong>
                      {workflowState?.description && (
                        <>
                          <p>{workflowState?.description}</p>
                          <br />
                        </>
                      )}
                      <br />
                      <p>
                        Click to edit column name, description and default
                        roles/tasks
                      </p>
                    </div>
                  }
                >
                  <div className="card-width my-2 flex items-center justify-between">
                    <h3 className="m-0 truncate text-left capitalize">
                      {workflowState.name}
                    </h3>
                    <span className="rounded-md bg-blue-50 px-2 font-extralight text-gray-500 ">
                      {cards.length}
                    </span>
                  </div>
                </Tippy>
              )}
            </button>

            <div
              className="juxt-kanban-cols-container no-scrollbar h-full overflow-scroll"
              ref={provided.innerRef}
              {...provided.droppableProps}
            >
              {cards.map((t, i) => (
                <DraggableCard
                  key={t.id}
                  card={t}
                  workflow={workflow}
                  index={i}
                />
              ))}
              {provided.placeholder}
            </div>
          </div>
        )}
      </Droppable>
    )
  }
)

export function WorkflowStateContainer({
  provided,
  isDragging,
  cols,
  workflow
}: {
  provided: DroppableProvided
  isDragging: boolean
  cols: NonNullable<TWorkflowState[]>
  workflow: NonNullable<TWorkflow>
}) {
  const [source, target] = useSingleton()
  const search = useSearch<LocationGenerics>()
  const [globalSearch] = useAtom(searchAtom)
  const columnFilters =
    search?.filters?.colIds ?? search?.filters?.roleFilters ?? []
  const colIds = globalSearch ? [] : columnFilters
  const hideEmptyStates = search?.hideEmptyStates
  const hiddenColumnIds = new Set(colIds)
  return (
    <div
      className="h-column-height-sm lg:h-column-height-lg flex max-w-full flex-col sm:flex-row"
      {...provided.droppableProps}
      ref={provided.innerRef}
    >
      <Tippy
        singleton={source}
        delay={[500, 100]}
        moveTransition="transform 0.4s cubic-bezier(0.22, 1, 0.36, 1)"
      />
      {cols
        .filter((col) => !hiddenColumnIds.has(col.id))
        .filter((col) =>
          hideEmptyStates ? col.cards && col.cards.length > 0 : true
        )
        .map((col) => {
          return (
            <WorkflowState
              key={col.id}
              tooltipTarget={target}
              isDragging={isDragging}
              workflowState={col}
              cards={col?.cards?.filter(notEmpty) || []}
              workflow={workflow}
            />
          )
        })}
      {provided.placeholder}
    </div>
  )
}
