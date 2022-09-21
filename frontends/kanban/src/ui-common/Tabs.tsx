import {
  AnnotationIcon,
  EyeIcon,
  EyeOffIcon,
  SearchIcon
} from '@heroicons/react/solid'
import { LocationGenerics, useModalForm } from '../site'
import { notEmpty, useMobileDetect } from '../utils'
import Tippy, { useSingleton } from '@tippyjs/react'
import classNames from 'classnames'
import { useAtom } from 'jotai'
import { useCallback } from 'react'
import { useNavigate, useSearch } from '@tanstack/react-location'
import Select from 'react-select'
import { dirtyAtom, Option, useGlobalSearch } from './Forms'
import { OptionsMenu } from './Menus'

type Tab = {
  id: string
  name: string
  count?: number
  default?: boolean
  hidden?: boolean
}

type TabProps = {
  tabs: Tab[]
  navName: keyof LocationGenerics['Search']
}

export function NavTabs({ tabs }: TabProps) {
  const navigate = useNavigate<LocationGenerics>()
  const search = useSearch<LocationGenerics>()
  const currentIds = search['workflowProjectIds']
  const [, setViewComments] = useModalForm({
    formModalType: 'viewComments'
  })
  const handleFilterMyCards = () => {
    navigate({
      replace: true,
      search: (search) => ({
        ...search,
        showMyCards: !search?.showMyCards
      })
    })
  }
  const onTabClick = (id?: string, multi?: boolean) => {
    const newIds = multi ? [...(currentIds || []), id] : [id]
    navigate({
      search: {
        ...search,
        workflowProjectIds: id ? newIds.filter(notEmpty) : undefined
      }
    })
  }

  const hideEmptyStates = search.hideEmptyStates
  const handleToggleEmptyStates = () => {
    navigate({
      search: (search) => ({
        ...search,
        hideEmptyStates: !search?.hideEmptyStates
      })
    })
  }

  const showMyCards = search?.showMyCards

  const [searchVal, setSearchVal] = useGlobalSearch()
  const [source, target] = useSingleton()
  return (
    <div className="mb-2">
      <Tippy
        singleton={source}
        delay={[500, 100]}
        moveTransition="transform 0.4s cubic-bezier(0.22, 1, 0.36, 1)"
      />
      <div className="hidden sm:block">
        <div className="border-b border-gray-200">
          <nav
            className="-mb-px flex justify-between space-x-8"
            aria-label="Tabs"
          >
            <div className="flex items-center justify-center px-2  lg:justify-start">
              <div className="max-w-lg lg:max-w-xs">
                <label htmlFor="search" className="sr-only">
                  Search
                </label>
                <div className="relative ">
                  <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                    <SearchIcon
                      className="h-5 w-5 text-gray-400"
                      aria-hidden="true"
                    />
                  </div>
                  <input
                    id="search"
                    name="search"
                    className={classNames(
                      searchVal && searchVal.length > 0
                        ? 'border-indigo-600'
                        : 'border-gray-300',
                      'block pl-10 pr-3 py-2 border rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm'
                    )}
                    placeholder="Search"
                    type="search"
                    onChange={(e) => setSearchVal(e.target.value)}
                    value={searchVal}
                  />
                </div>
              </div>
            </div>
            <div className="flex flex-row items-center">
              {tabs.map((tab) => {
                const isCurrent = currentIds
                  ? currentIds?.includes(tab.id)
                  : !tab.id
                return (
                  <Tippy
                    singleton={target}
                    key={tab.id + tab.name}
                    delay={[100, 500]}
                    className=" relative whitespace-normal rounded bg-slate-800 p-2 text-center text-sm text-white outline-none transition-all"
                    content={
                      <div className="text-sm">
                        <p>
                          Click to show only {tab.name} cards, or shift+click to
                          select multiple filters
                        </p>
                      </div>
                    }
                  >
                    <button
                      type="button"
                      onClick={(e) => {
                        onTabClick(tab.id, e.shiftKey)
                      }}
                      className={classNames(
                        isCurrent
                          ? 'border-indigo-500 text-indigo-600'
                          : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-200',
                        'whitespace-nowrap cursor-pointer flex py-4 px-1 border-b-2 font-medium text-sm'
                      )}
                      aria-current={isCurrent ? 'page' : undefined}
                    >
                      {tab.name}
                      {typeof tab.count === 'number' ? (
                        <span
                          className={classNames(
                            isCurrent
                              ? 'bg-indigo-100 text-indigo-600'
                              : 'bg-gray-100 text-gray-900',
                            'hidden ml-3 py-0.5 px-2.5 rounded-full text-xs font-medium md:inline-block'
                          )}
                        >
                          {tab.count}
                        </span>
                      ) : null}
                    </button>
                  </Tippy>
                )
              })}
              <div className="border-b-2 border-transparent">
                <OptionsMenu
                  options={[
                    {
                      label: 'View comments',
                      id: 'viewComments',
                      Icon: AnnotationIcon,
                      props: {
                        onClick: () => setViewComments(true)
                      }
                    },
                    {
                      label: hideEmptyStates
                        ? 'Show empty columns'
                        : 'Hide empty columns',
                      id: 'toggleEmptyStates',
                      Icon: hideEmptyStates ? EyeIcon : EyeOffIcon,
                      openOnSelect: true,
                      props: {
                        onClick: handleToggleEmptyStates
                      }
                    },
                    {
                      label: showMyCards
                        ? "Don't highlight cards owned by me"
                        : 'Highlight cards owned by me',
                      id: 'myCards',
                      Icon: showMyCards ? EyeOffIcon : EyeIcon,
                      openOnSelect: true,
                      props: {
                        title:
                          'Click to filter the board to only show cards where you are an owner',
                        onClick: handleFilterMyCards
                      }
                    }
                  ]}
                />
              </div>
            </div>

            <div className="hidden lg:block lg:w-60" />
          </nav>
        </div>
      </div>
    </div>
  )
}

export function ModalTabs({ tabs, navName }: TabProps) {
  const navigate = useNavigate<LocationGenerics>()
  const search = useSearch<LocationGenerics>()
  const currentId = search[navName]
  const [isDirty, setDirty] = useAtom(dirtyAtom)

  const onTabClick = (id?: string) => {
    const confirmed =
      !isDirty ||
      confirm(
        'You have unsaved changes. Are you sure you want to navigate away?'
      )

    if (confirmed) {
      setDirty(false)
      navigate({
        replace: true,
        search: (search) => ({
          ...search,
          [navName]: id
        })
      })
    }
  }

  return (
    <div className="overflow-x-auto border-b  border-gray-200 sm:overflow-hidden">
      <nav
        className="-mb-px flex justify-center space-x-4 sm:space-x-8"
        aria-label="Tabs"
      >
        {tabs
          .filter((t) => !t.hidden)
          .map((tab) => {
            const isCurrent = tab.id === currentId || tab.default
            return (
              <button
                type="button"
                key={tab.id + tab.name}
                onClick={() => onTabClick(tab.id)}
                className={classNames(
                  isCurrent
                    ? 'border-indigo-500 text-indigo-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-200',
                  'sm:whitespace-nowrap cursor-pointer flex py-3 px-1 border-b-2 font-medium text-sm'
                )}
                aria-current={isCurrent ? 'page' : undefined}
              >
                {tab.name}
              </button>
            )
          })}
      </nav>
    </div>
  )
}

type ToggleTab = Tab & { selectedName: string }

export function useHasFilter() {
  const { filters, view } = useSearch<LocationGenerics>()
  const isMobile = useMobileDetect().isMobile()
  const hasFilter = useCallback(
    (filter) =>
      isMobile ? view === filter : filters?.['tabs']?.includes(filter),
    [filters, isMobile, view]
  )
  return (filter: string) => hasFilter(filter)
}

export function ToggleTabs({ tabs }: { tabs: ToggleTab[] }) {
  const navigate = useNavigate<LocationGenerics>()
  const search = useSearch<LocationGenerics>()
  const selected = search['filters']?.['tabs'] ?? []

  const onTabClick = (id: string) => {
    navigate({
      replace: true,
      search: (search) => ({
        ...search,
        filters: {
          ...search?.['filters'],
          tabs: selected.includes(id)
            ? selected.filter((i) => i !== id)
            : [...selected, id]
        }
      })
    })
  }

  return (
    <div className="border-b border-gray-200">
      <nav className="-mb-px flex justify-center space-x-8" aria-label="Tabs">
        {tabs
          .filter((t) => !t.hidden)
          .map((tab) => {
            const isSelected = selected.includes(tab.id)
            return (
              <button
                type="button"
                key={tab.id + tab.name}
                onClick={() => onTabClick(tab.id)}
                className={classNames(
                  'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-200',
                  'whitespace-nowrap cursor-pointer flex py-4 px-1 border-b-2 font-medium text-sm'
                )}
                aria-current={isSelected ? 'page' : undefined}
              >
                {isSelected ? tab.selectedName : tab.name}
              </button>
            )
          })}
      </nav>
    </div>
  )
}
