import { XIcon } from '@heroicons/react/solid'
import {
  LocationGenerics,
  useProjectOptions,
  useCardById,
  useStatesOptions,
  juxters,
  useClientOptions
} from '../../site'
import { ModalTabs, Modal, PdfViewer, dirtyAtom } from '../../ui-common'
import { notEmpty } from '../../utils'
import { useAtom } from 'jotai'
import * as _ from 'lodash'
import { useNavigate, useSearch } from '@tanstack/react-location'
import { workflowId } from '../constants'
import { AddHiringCardModal } from './AddHiringCardForm'
import { CardHistory } from './CardHistory'
import { CardViewWrapper } from './CardView'
import {
  AddHiringCardModalProps,
  AddHiringCardInput,
  EditCardModalProps
} from './types'
import { UpdateHiringCardForm } from './UpdateHiringCardForm'

export function AddHiringCardModalWrapper({
  isOpen,
  handleClose
}: AddHiringCardModalProps) {
  const { workflowProjectIds } = useSearch<LocationGenerics>()
  const [{ data: cols }, stateOptions] = useStatesOptions({ workflowId })
  const projectOptions = useProjectOptions(workflowId)
  const usernameOptions = juxters.map((user) => ({
    label: user.name,
    value: user.staffRecord.juxtcode
  }))
  const { data: clientOptions } = useClientOptions()

  const defaultValues: Partial<AddHiringCardInput> = {
    project:
      projectOptions.find((p) => workflowProjectIds?.includes(p.value)) ||
      projectOptions[0],
    workflowStateId: stateOptions?.[0]?.value,
    workflowState: stateOptions?.[0]
  }

  return (
    <>
      {_.isEmpty(stateOptions) || !cols || !clientOptions ? (
        <div>Loading workflow states...</div>
      ) : (
        <AddHiringCardModal
          isOpen={isOpen}
          clientOptions={clientOptions}
          usernameOptions={usernameOptions}
          handleClose={handleClose}
          defaultValues={defaultValues}
          projectOptions={projectOptions}
          cols={cols}
          stateOptions={stateOptions}
        />
      )}
    </>
  )
}

export function EditHiringCardModal({
  isOpen,
  handleClose
}: EditCardModalProps) {
  const { cardModalView } = useSearch<LocationGenerics>()
  const cardId = useSearch<LocationGenerics>().modalState?.cardId
  const { data, error } = useCardById(cardId)
  const navigate = useNavigate<LocationGenerics>()
  const card = data?.cardsByIds?.[0]
  const pdfBase64 = card?.cvPdf?.base64

  const [hasUnsaved] = useAtom(dirtyAtom)
  const onClose = () => {
    const confirmation =
      hasUnsaved &&
      // eslint-disable-next-line no-restricted-globals
      confirm('You have unsaved changes. Are you sure you want to close?')
    if (!hasUnsaved || confirmation) {
      handleClose()
      if (cardModalView !== 'view') {
        navigate({
          search: (search) => ({
            ...search,
            modalState: undefined,
            cardModalView: undefined
          })
        })
      }
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      handleClose={onClose}
      fullWidth={cardModalView !== 'update'}
      noScroll
    >
      <div className="fixed top-0 z-10 w-full bg-white">
        <ModalTabs
          tabs={[
            { id: 'view', name: 'View', default: !cardModalView },
            { id: 'cv', name: 'CV', hidden: !pdfBase64 },
            { id: 'update', name: 'Edit' },
            { id: 'history', name: 'History' }
          ]}
          navName="cardModalView"
        />
        <div className="absolute top-3 right-3 h-5 w-5 cursor-pointer">
          <XIcon onClick={onClose} />
        </div>
      </div>
      <div className="h-full" style={{ paddingTop: '54px' }}>
        {error && (
          <div className="flex h-full flex-col items-center justify-center">
            <div className="text-center">
              <h1 className="text-3xl font-extrabold text-gray-900">
                Error Loading Card
              </h1>
            </div>
            <div className="text-center">
              <p className="text-gray-700">{error.message}</p>
            </div>
          </div>
        )}
        {(!cardModalView || cardModalView === 'view') && <CardViewWrapper />}
        {cardModalView === 'update' && (
          <UpdateHiringCardForm handleClose={onClose} />
        )}
        {cardModalView === 'history' && <CardHistory />}
        {cardModalView === 'cv' && (
          <div className="mx-auto block h-full min-h-full ">
            <PdfViewer pdfString={pdfBase64} />
          </div>
        )}
      </div>
    </Modal>
  )
}
