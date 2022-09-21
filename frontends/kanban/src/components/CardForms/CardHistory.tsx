/* eslint-disable @typescript-eslint/no-non-null-assertion */
import {
  BookOpenIcon,
  DatabaseIcon,
  ChevronDoubleLeftIcon,
  ChevronDoubleRightIcon
} from '@heroicons/react/solid'
import {
  LocationGenerics,
  useCardHistory,
  useRollbackCardMutation,
  useCardByIdsQuery,
  useKanbanDataQuery,
  useCardHistoryQuery,
  CardHistoryQuery,
  useAsOf,
  purgeAllLists
} from '../../site'
import { Modal, Table } from '../../ui-common'
import { notEmpty } from '../../utils'
import { useMemo, useState } from 'react'
import { useSearch } from '@tanstack/react-location'
import { useQueryClient } from 'react-query'
import { CellProps } from 'react-table'
import { toast } from 'react-toastify'
import { workflowId } from '../constants'
import { CardView } from './CardView'

type TCardHistoryCard = NonNullable<
  NonNullable<CardHistoryQuery['cardHistory']>[0]
>

function TitleComponent({ value }: CellProps<TCardHistoryCard>) {
  return <div className="truncate text-sm">{value || 'Untitled'}</div>
}

export function CardHistory() {
  const [showPreviewModal, setShowPreviewModal] = useState<boolean | number>(
    false
  )

  const cardId = useSearch<LocationGenerics>().modalState?.cardId
  const { history, isLoading, isError, error } = useCardHistory(cardId)
  const previewIndex =
    typeof showPreviewModal === 'number' ? showPreviewModal : 0
  const previewCard = history?.[previewIndex]

  const [, setAsOf] = useAsOf({ validTime: previewCard?._siteValidTime })

  const handleClose = () => {
    setAsOf(undefined)
    setShowPreviewModal(false)
  }
  const queryClient = useQueryClient()
  const rollbackMutation = useRollbackCardMutation({
    onSettled: (data) => {
      const id = data?.rollbackCard?.id || ''
      queryClient.refetchQueries(useCardByIdsQuery.getKey({ ids: [id] }))
      queryClient.refetchQueries(useKanbanDataQuery.getKey({ id: workflowId }))
      queryClient.refetchQueries(useCardHistoryQuery.getKey({ id }))
      purgeAllLists()
    }
  })
  const handleRollback = async (card: TCardHistoryCard) => {
    toast.promise(
      rollbackMutation.mutateAsync({ id: card.id, asOf: card._siteValidTime }),
      {
        success: 'Card rolled back successfully',
        error: 'Card rollback failed',
        pending: 'Rolling back card...'
      }
    )
  }
  // eslint-disable-next-line react/no-unstable-nested-components
  function RollbackButton({ row }: CellProps<TCardHistoryCard>) {
    return (
      <div className="flex flex-row justify-between">
        <button
          type="button"
          title="Rollback"
          className="mt-3"
          onClick={() => handleRollback(row.original)}
        >
          <DatabaseIcon
            className="h-5 w-8 text-stone-700 hover:text-indigo-700"
            aria-hidden="true"
          />
        </button>

        <button
          type="button"
          title="Preview"
          className="mt-3"
          onClick={() => setShowPreviewModal(row.index)}
        >
          <BookOpenIcon
            className="h-6 w-8 text-stone-700 hover:text-indigo-700"
            aria-hidden="true"
          />
        </button>
      </div>
    )
  }

  const data = useMemo(
    () =>
      history
        ?.filter(notEmpty)
        .filter((h) => h._siteSubject)
        .map((card, i) => {
          const hasDescriptionChanged =
            history[i + 1] && history[i + 1]?.description !== card?.description
          const projectChanged =
            history[i + 1] &&
            history[i + 1]?.project?.name !== card?.project?.name
          const filesChanged =
            history[i + 1] &&
            history[i + 1]?.files?.map((f) => f?.name).toString() !==
              card?.files?.map((f) => f?.name).toString()
          const titleChanged =
            history[i + 1] && history[i + 1]?.title !== card?.title
          const locationChanged =
            history[i + 1] && history[i + 1]?.location !== card?.location
          const agentChanged =
            history[i + 1] && history[i + 1]?.agent !== card?.agent
          const stateChanged =
            history[i + 1] && history[i + 1]?.stateStr !== card?.stateStr
          const taskHtmlChanged =
            history[i + 1] && history[i + 1]?.taskHtml !== card?.taskHtml
          const nothingChanged =
            !titleChanged &&
            !hasDescriptionChanged &&
            !projectChanged &&
            !agentChanged &&
            !locationChanged &&
            !stateChanged &&
            !taskHtmlChanged &&
            !filesChanged

          return {
            ...card,
            nothingChanged,
            hasDescriptionChanged,
            projectChanged,
            filesChanged,
            titleChanged,
            diff: [
              titleChanged && 'Title changed',
              hasDescriptionChanged && 'description changed',
              projectChanged && 'project changed',
              stateChanged && `state changed to ${card.stateStr}`,
              filesChanged && 'files changed',
              agentChanged && 'agent changed',
              locationChanged && 'location changed',
              taskHtmlChanged && 'task changed'
            ]
              .filter((s) => s)
              .join(', ')
          }
        }),
    [history]
  )

  const cols = useMemo(
    () => [
      {
        Header: 'Diff',
        accessor: 'diff'
      },
      {
        Header: 'Title',
        accessor: 'title',
        Cell: TitleComponent
      },
      {
        Header: 'Project',
        accessor: 'project.name'
      },
      {
        Header: 'Edited By',
        accessor: '_siteSubject'
      },
      {
        Header: 'Updated at',
        accessor: '_siteValidTime'
      },
      {
        Header: 'Actions',
        Cell: RollbackButton
      }
    ],
    []
  )

  return (
    <div className="relative h-full">
      <div className="flex h-full flex-col items-center justify-around lg:flex-row lg:items-start">
        <div className="relative flex h-full w-full flex-col overflow-x-auto px-4 lg:w-fit lg:overflow-x-hidden">
          <div className="text-center">
            <h1 className="text-3xl font-extrabold text-gray-900">
              {isLoading
                ? 'Loading...'
                : isError
                ? `Error: ${error?.name} - ${error?.message}`
                : 'Card History'}
            </h1>
          </div>
          {history && (
            <>
              <Table columns={cols} data={data} />
              <Modal
                isOpen={showPreviewModal !== false}
                handleClose={handleClose}
              >
                {previewCard && typeof previewIndex === 'number' && (
                  <>
                    <CardView card={previewCard} />
                    <button
                      type="button"
                      title="Previous"
                      disabled={previewIndex === history.length - 1}
                      className="absolute top-1/2 left-1 cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
                      onClick={() => setShowPreviewModal(previewIndex + 1)}
                    >
                      <ChevronDoubleLeftIcon
                        className="h-8 w-8 rounded-full bg-slate-500 text-white hover:text-slate-200"
                        aria-hidden="true"
                      />
                    </button>
                    <button
                      type="button"
                      disabled={previewIndex === 0}
                      title="Previous"
                      className="absolute top-1/2 right-1 cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
                      onClick={() => setShowPreviewModal(previewIndex - 1)}
                    >
                      <ChevronDoubleRightIcon
                        className="h-8 w-8 rounded-full bg-slate-500 text-white hover:text-slate-200"
                        aria-hidden="true"
                      />
                    </button>
                    <h1 className="absolute inset-x-0 bottom-2 bg-black text-center text-lg text-white">
                      History Item - showing card as of{' '}
                      {previewCard._siteValidTime}
                    </h1>
                  </>
                )}
              </Modal>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
