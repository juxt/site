import { Fragment, useState } from 'react'
import {
  ChevronDownIcon,
  ChevronRightIcon,
  EyeIcon,
  FolderAddIcon,
  InformationCircleIcon,
  PencilIcon,
  PlusIcon,
  TableIcon,
  ViewBoardsIcon
} from '@heroicons/react/solid'
import { Menu, Transition } from '@headlessui/react'
import classNames from 'classnames'
import { useNavigate, useSearch } from '@tanstack/react-location'
import {
  TWorkflow,
  LocationGenerics,
  useCurrentProject,
  useModalForm,
  useWorkflowStates
} from '../site'
import { notEmpty, useMobileDetect } from '../utils'
import { MultiSelect } from 'react-multi-select-component'
import Tippy from '@tippyjs/react'

export function Heading({
  workflow,
  handleAddCard,
  initialHiddenCols
}: {
  workflow: TWorkflow
  handleAddCard: () => void
  initialHiddenCols: string[]
}) {
  const currentProject = useCurrentProject(workflow.id).data
  const isMobile = useMobileDetect().isMobile()
  const navigate = useNavigate<LocationGenerics>()
  const search = useSearch<LocationGenerics>()

  const cols =
    useWorkflowStates({ workflowId: workflow?.id || '' }).data?.map(
      (state) => ({
        label: state.name,
        value: state.id
      })
    ) || []
  const [colIds, setColIds] = useState<typeof cols>(
    initialHiddenCols.map((c) => ({ value: c, label: '' }))
  )

  const setToggleColumn = (colIds: typeof cols) => {
    setColIds(colIds)
    navigate({
      replace: true,
      search: (search) => ({
        ...search,
        filters: {
          colIds: colIds.map((c) => c.value)
        }
      })
    })
  }

  const isCardView = !search?.view || search.view === 'card'
  const ChangeViewIcon = isCardView ? (
    <ViewBoardsIcon
      className="-ml-1 mr-2 h-5 w-5 text-gray-500"
      aria-hidden="true"
    />
  ) : (
    <TableIcon
      className="-ml-1 mr-2 h-5 w-5 text-gray-500"
      aria-hidden="true"
    />
  )
  const changeViewText = isCardView ? 'Table View' : 'Card View'
  const handleChangeView = () => {
    navigate({
      replace: true,
      search: (search) => ({ ...search, view: isCardView ? 'table' : 'card' })
    })
  }

  const hasProject = currentProject?.name
  const editProjectText = `Edit "${currentProject?.name}"`
  const addProjectText = 'Add Project'
  const [, setProjectFormOpen] = useModalForm({
    formModalType: 'editProject',
    workflowProjectId: currentProject?.id
  })
  const [, setAddProject] = useModalForm({
    formModalType: 'addProject'
  })

  const resetProjectFilters = () => {
    navigate({
      replace: true,
      search: (search) => ({
        ...search,
        workflowProjectIds: undefined
      })
    })
  }

  const totalRolesForProject =
    (currentProject &&
      currentProject.openRoles?.reduce(
        (acc, role) => acc + (role?.count || 0),
        0
      )) ||
    0

  return (
    <div className="z-20 pb-4 lg:flex lg:items-center lg:justify-between">
      <div className="min-w-0 flex-1">
        <nav className="flex" aria-label="Breadcrumb">
          <ol className="flex items-center space-x-4">
            <li>
              <div className="flex">
                <button
                  onClick={resetProjectFilters}
                  className="text-sm font-medium text-gray-500 hover:text-gray-700"
                >
                  Projects
                </button>
              </div>
            </li>
            <li>
              <div className="flex items-center">
                <ChevronRightIcon
                  className="h-5 w-5 shrink-0 text-gray-400"
                  aria-hidden="true"
                />
                <span className="ml-4 text-sm font-medium text-gray-500 hover:text-gray-700">
                  {currentProject?.name || 'All projects'}
                </span>
              </div>
            </li>
          </ol>
        </nav>
        <div className="flex items-center">
          <h2 className="mt-2 text-2xl font-bold capitalize leading-7 text-gray-900 sm:truncate sm:text-3xl">
            {workflow?.name}
          </h2>
          {hasProject && (
            <Tippy
              disabled={totalRolesForProject === 0}
              content={currentProject?.openRoles
                ?.filter(notEmpty)
                .map((role) => (
                  <p key={role.name} className="text-sm font-medium text-white">
                    {`${role.name} (${role.count})`}
                  </p>
                ))}
            >
              <button
                onClick={() => setProjectFormOpen(true)}
                className="flex cursor-pointer"
              >
                <h3 className="mt-2 ml-4 text-2xl">
                  {`${totalRolesForProject} ${currentProject.name} positions to fill`}
                </h3>
                <InformationCircleIcon className="h-4 w-4" />
              </button>
            </Tippy>
          )}
        </div>
      </div>
      <div className="mt-5 flex lg:mt-0 lg:ml-4">
        <span className="hidden sm:block">
          <button
            type="button"
            onClick={() => setAddProject(true)}
            className="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
          >
            <FolderAddIcon
              className="-ml-1 mr-2 h-5 w-5 text-gray-500"
              aria-hidden="true"
            />
            {addProjectText}
          </button>
        </span>

        {hasProject && (
          <span className="ml-3 hidden sm:block">
            <button
              type="button"
              onClick={() => setProjectFormOpen(true)}
              className="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
            >
              <PencilIcon
                className="-ml-1 mr-2 h-5 w-5 text-gray-500"
                aria-hidden="true"
              />
              {editProjectText}
            </button>
          </span>
        )}
        <span className="ml-3 hidden sm:block">
          <button
            type="button"
            onClick={handleChangeView}
            className="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
          >
            {ChangeViewIcon}
            {changeViewText}
          </button>
        </span>
        {!isMobile && isCardView && (
          <MultiSelect
            valueRenderer={(value) => {
              const count = value.filter((v) => v.value).length
              return `${count}/${cols.length} cols hidden`
            }}
            className="tw-multiselect ml-3 inline-flex items-center rounded-md border border-gray-300 bg-white text-sm font-medium text-gray-700 shadow-sm focus-within:outline-none focus-within:ring-2 focus-within:ring-indigo-500 focus-within:ring-offset-2 hover:bg-gray-50"
            onChange={setToggleColumn}
            options={cols}
            labelledBy=""
            value={colIds}
          />
        )}

        <span className="sm:ml-3">
          <button
            type="button"
            className="inline-flex items-center rounded-md border border-transparent bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
            onClick={handleAddCard}
          >
            <PlusIcon className="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            Add Card
            {!isMobile && ' (c)'}
          </button>
        </span>

        {/* Dropdown */}
        <Menu as="span" className="relative ml-3 sm:hidden">
          <Menu.Button className="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2">
            More
            <ChevronDownIcon
              className="-mr-1 ml-2 h-5 w-5 text-gray-500"
              aria-hidden="true"
            />
          </Menu.Button>

          <Transition
            as={Fragment}
            enter="transition ease-out duration-200"
            enterFrom="transform opacity-0 scale-95"
            enterTo="transform opacity-100 scale-100"
            leave="transition ease-in duration-75"
            leaveFrom="transform opacity-100 scale-100"
            leaveTo="transform opacity-0 scale-95"
          >
            <Menu.Items className="absolute right-0 z-20 mt-2 -mr-1 w-48 origin-top-right cursor-pointer rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
              <Menu.Item>
                {({ active }) => (
                  <button
                    type="button"
                    onClick={() => setAddProject(true)}
                    className={classNames(
                      active ? 'bg-gray-100' : '',
                      'flex px-4 py-2 text-sm text-gray-700'
                    )}
                  >
                    <FolderAddIcon
                      className="-ml-1 mr-2 h-5 w-5 text-gray-500"
                      aria-hidden="true"
                    />
                    {addProjectText}
                  </button>
                )}
              </Menu.Item>

              {hasProject && (
                <Menu.Item>
                  {({ active }) => (
                    <button
                      type="button"
                      onClick={() => setProjectFormOpen(true)}
                      className={classNames(
                        active ? 'bg-gray-100' : '',
                        'flex px-4 py-2 text-sm text-gray-700'
                      )}
                    >
                      <PencilIcon
                        className="-ml-1 mr-2 h-5 w-5 text-gray-500"
                        aria-hidden="true"
                      />
                      {editProjectText}
                    </button>
                  )}
                </Menu.Item>
              )}

              <Menu.Item>
                {({ active }) => (
                  <button
                    type="button"
                    onClick={handleChangeView}
                    className={classNames(
                      active ? 'bg-gray-100' : '',
                      'flex px-4 py-2 text-sm text-gray-700'
                    )}
                  >
                    {ChangeViewIcon}
                    {changeViewText}
                  </button>
                )}
              </Menu.Item>
            </Menu.Items>
          </Transition>
        </Menu>
      </div>
    </div>
  )
}
