import { notEmpty } from '../utils'
import classNames from 'classnames'
import { Fragment } from 'react'

type MetadataItem = {
  label: string
  value?: string | null
  type?: 'date' | 'text' | 'number'
  hidden?: boolean
}

export function MetadataGrid({
  metadata,
  title,
  children
}: {
  metadata: MetadataItem[]
  title: string
  children?: React.ReactNode
}) {
  const metadataLabelClass = classNames(
    'text-sm font-medium text-gray-700 font-bold'
  )
  const metadataClass = classNames('text-sm font-medium text-gray-700')
  return (
    <div className="isolate flex min-w-min flex-col px-4 sm:overflow-y-auto">
      <h2 className="text-3xl font-extrabold text-gray-900 sm:text-4xl">
        {title}
      </h2>
      <div className="my-4 grid grid-cols-2 text-left">
        {metadata
          .filter((item) => item?.value && !item?.hidden)
          .filter(notEmpty)
          .map((item) => (
            <Fragment key={item?.label}>
              <p className={metadataLabelClass}>{item.label}</p>
              <p className={metadataClass}>
                {item?.value && item.type === 'date'
                  ? new Date(item.value).toLocaleDateString()
                  : item.type === 'number'
                  ? item.value
                  : item.value}
              </p>
            </Fragment>
          ))}
      </div>
      {children}
    </div>
  )
}
