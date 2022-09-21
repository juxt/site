import { Dialog } from '@headlessui/react'
import Dropzone, { FileRejection } from 'react-dropzone'
import { Controller, FieldError, FieldValues } from 'react-hook-form'
import { MultiSelect } from 'react-multi-select-component'
import Select, { GroupBase, Props as SelectProps } from 'react-select'
import { DownloadIcon } from '@heroicons/react/solid'
import classNames from 'classnames'
import { notEmpty, fileToString, useMobileDetect } from '../../utils'
import { toast } from 'react-toastify'
import { CardByIdsQuery } from '@juxt-home/site'
import { FormInputField, FormProps, Option } from './types'
import {
  DeleteInactiveIcon,
  DeleteActiveIcon,
  OptionsMenu,
  Tiptap,
  Button,
  useDirty
} from '../index'
import get from 'lodash-es/get'
import { useEffect } from 'react'

export const inputClass =
  'relative inline-flex w-full rounded leading-none transition-colors ease-in-out placeholder-contrast-light text-contrast bg-white dark:bg-gray-600 border border-gray-300 dark:border-gray-500 hover:border-blue-400 focus:outline-none focus:border-blue-400 focus:ring-blue-400 focus:ring-4 focus:ring-opacity-30 p-3 text-base'

function CustomSelect<
  SelectOption,
  IsMulti extends boolean = false,
  Group extends GroupBase<SelectOption> = GroupBase<SelectOption>
>(props: SelectProps<SelectOption, IsMulti, Group>) {
  return <Select {...props} />
}

function ImagePreview({ image, title }: { image: string; title: string }) {
  return (
    <li className={classNames('block p-1 h-24 isolate', ' w-full pb-5')}>
      <article className="group hasImage focus:shadow-outline relative h-full w-full rounded-md bg-gray-100 shadow-sm  focus:outline-none">
        <img
          alt="upload preview"
          className={classNames(
            'w-full h-full rounded-md bg-fixed',
            'object-contain'
          )}
          src={image}
        />

        <section className="flex w-full flex-col justify-between break-words rounded-md py-2 px-3 text-xs">
          <h1 className="truncate">{title}</h1>
        </section>
      </article>
    </li>
  )
}

type TFile = NonNullable<
  NonNullable<NonNullable<CardByIdsQuery['cardsByIds']>[0]>['files']
>[0]

export function FilePreview({
  file,
  handleDelete
}: {
  file: NonNullable<TFile>
  handleDelete?: () => void
}) {
  if (!file) return <div>No file</div>

  const options = [
    {
      label: 'Delete',
      id: 'delete',
      Icon: DeleteInactiveIcon,
      ActiveIcon: DeleteActiveIcon,
      props: {
        onClick: handleDelete
      }
    },
    {
      label: 'Download',
      ActiveIcon: DownloadIcon,
      Icon: DownloadIcon,

      id: 'download',
      props: {
        href: file.base64,
        download: file.name
      }
    }
  ]

  return (
    <div className="flex flex-row justify-between">
      {file.type.startsWith('image') ? (
        <ImagePreview title={file.name} image={file.base64} />
      ) : (
        <div className="w-5/6">
          <p>
            Type:
            {file.type}
          </p>
          <h1 className="max-w-fit truncate">{file.name}</h1>
        </div>
      )}
      {handleDelete && <OptionsMenu options={options} />}
    </div>
  )
}

export function ErrorMessage({ error }: { error: FieldError | undefined }) {
  if (!error) return null
  return <p className="text-red-600">{error.message}</p>
}

export function RenderField<T extends FieldValues>({
  field,
  props
}: {
  field: FormInputField<T>
  props: FormProps<T>
}) {
  const { control, register } = props.formHooks
  const registerProps = register(field.path, field.rules)
  const defaultProps = {
    ...registerProps,
    className: classNames(inputClass, props?.className),
    type: field.type
  }

  switch (field.type) {
    case 'text':
      return <input {...defaultProps} placeholder={field?.placeholder} />
    case 'number':
      return <input {...defaultProps} placeholder={field?.placeholder} />
    case 'checkbox':
      return <input {...defaultProps} className="" />
    case 'file':
      return (
        <Controller
          render={(controlProps) => {
            const { onChange, value } = controlProps.field

            const handleDrop = async (
              acceptedFiles: File[],
              fileRejections: FileRejection[]
            ) => {
              const f = acceptedFiles[0]
              fileRejections.forEach((rejection) => {
                toast.error(
                  `Couldn't upload file ${
                    rejection.file.name
                  }. ${rejection.errors.map((e) => e.message).join(', ')}`
                )
              })
              const base64 = await fileToString(f)
              const newFile = {
                name: f.name,
                type: f.type,
                base64
              }

              onChange(newFile)
            }
            const file = value as TFile

            return (
              <Dropzone
                onDrop={handleDrop}
                accept={field.accept}
                maxSize={5000000}
              >
                {({
                  getRootProps,
                  getInputProps,
                  isDragAccept,
                  isDragActive,
                  isDragReject
                }) => (
                  <section>
                    {!file ? (
                      <div {...getRootProps()}>
                        <div
                          className={classNames(
                            'max-w-lg flex justify-center cursor-pointer px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md',
                            isDragActive && 'border-blue-500',
                            isDragAccept && 'border-green-500 cursor-copy',
                            isDragReject && 'border-red-500 cursor-no-drop'
                          )}
                        >
                          <div className="space-y-1 text-center">
                            <div className="flex text-sm text-gray-600">
                              <label
                                htmlFor="file-upload"
                                className="relative cursor-pointer rounded-md bg-white font-medium text-indigo-600 focus-within:outline-none focus-within:ring-2 focus-within:ring-indigo-500 focus-within:ring-offset-2 hover:text-indigo-500"
                              >
                                <input {...getInputProps()} />
                                <p>
                                  Drag a file here, or click to select a file
                                </p>
                              </label>
                            </div>
                            <p className="text-contrast-LIGHT text-xs">
                              {field.accept
                                ?.toString()
                                ?.replace(/image\/|application\//g, '')
                                .toLocaleUpperCase()}{' '}
                              up to 5MB
                            </p>
                          </div>
                        </div>
                      </div>
                    ) : (
                      <div className="pt-2 text-sm text-gray-800">
                        <div className="mt-2 pt-2">
                          <FilePreview
                            file={file}
                            handleDelete={() => onChange(null)}
                          />
                        </div>
                      </div>
                    )}
                  </section>
                )}
              </Dropzone>
            )
          }}
          name={field.path}
          control={control}
          rules={field.rules}
        />
      )
    case 'multifile':
      return (
        <Controller
          render={(controlProps) => {
            const { onChange, value } = controlProps.field
            const files: TFile[] | undefined =
              Array.isArray(value) && value.filter(notEmpty).length > 0
                ? value.filter(notEmpty)
                : undefined

            const handleDelete = (file: TFile) => {
              const newValue = files?.filter((f) => f !== file)
              onChange(newValue)
            }

            const handleDrop = async (
              acceptedFiles: File[],
              fileRejections: FileRejection[]
            ) => {
              fileRejections.forEach((rejection) => {
                toast.error(
                  `Couldn't upload file ${
                    rejection.file.name
                  }. ${rejection.errors.map((e) => e.message).join(', ')}`
                )
              })
              acceptedFiles.filter(notEmpty).map(async (f) => {
                const base64 = await fileToString(f)
                const newFile = {
                  name: f.name,
                  type: f.type,
                  base64
                }

                const newValue = [...(files || []), newFile]
                onChange(newValue)
              })
            }

            return (
              <Dropzone
                onDrop={handleDrop}
                accept={field.accept}
                multiple
                maxSize={5000000}
              >
                {({
                  getRootProps,
                  getInputProps,
                  isDragAccept,
                  isDragActive,
                  isDragReject
                }) => (
                  <section>
                    <div {...getRootProps()}>
                      <div
                        className={classNames(
                          'max-w-lg flex justify-center cursor-pointer px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md',
                          isDragActive && 'border-blue-500',
                          isDragAccept && 'border-green-500 cursor-copy',
                          isDragReject && 'border-red-500 cursor-no-drop'
                        )}
                      >
                        <div className="space-y-1 text-center">
                          <div className="flex text-sm text-gray-600">
                            <label
                              htmlFor="file-upload"
                              className="relative cursor-pointer rounded-md bg-white font-medium text-indigo-600 focus-within:outline-none focus-within:ring-2 focus-within:ring-indigo-500 focus-within:ring-offset-2 hover:text-indigo-500"
                            >
                              <input {...getInputProps()} />
                              <p>
                                Drag some files here, or click to select files
                              </p>
                            </label>
                          </div>
                          <p className="text-contrast-LIGHT text-xs">
                            {field.accept
                              ?.toString()
                              ?.replace(/image\/|application\//g, '')
                              .toLocaleUpperCase()}{' '}
                            up to 5MB
                          </p>
                        </div>
                      </div>
                    </div>
                    {files && (
                      <div className="pt-2 text-sm text-gray-800">
                        {files.length} files selected
                        {files.filter(notEmpty).map((file) => (
                          <div key={file.name} className="mt-2 pt-2">
                            <FilePreview
                              file={file}
                              handleDelete={() => handleDelete(file)}
                            />
                          </div>
                        ))}
                      </div>
                    )}
                  </section>
                )}
              </Dropzone>
            )
          }}
          name={field.path}
          control={control}
          rules={field.rules}
        />
      )
    case 'select':
      return (
        <Controller
          name={field.path}
          control={control}
          render={(controlProps) => {
            const { value, onChange, name } = controlProps.field

            return (
              <CustomSelect<Option>
                {...controlProps}
                options={field.options}
                onChange={(selected) => {
                  onChange(selected)
                }}
                value={value as Option}
                name={name}
              />
            )
          }}
        />
      )
    case 'multiselect':
      return (
        <Controller
          name={field.path}
          control={control}
          render={(controlProps) => {
            const { onChange, name } = controlProps.field
            const value = controlProps.field.value as Option[]
            const { isCreatable, onCreateOption, options } = field
            return (
              <MultiSelect
                onChange={onChange}
                isCreatable={isCreatable}
                onCreateOption={onCreateOption}
                options={options}
                value={value?.[0]?.label ? value : []}
                valueRenderer={(selected) =>
                  selected
                    .filter((o) => o?.label)
                    .map((option) => option.label)
                    .join(', ')
                }
                labelledBy={name}
              />
            )
          }}
        />
      )
    case 'textarea':
      return <textarea {...defaultProps} {...field} className={inputClass} />
    case 'tiptap':
      return (
        <Controller
          name={field.path}
          control={control}
          render={(controlProps) => {
            const { value, onChange } = controlProps.field
            return (
              <Tiptap
                onChange={onChange}
                content={value as string}
                withTaskListExtension
                withLinkExtension
                withTypographyExtension
                withPlaceholderExtension
                withMentionSuggestion
                {...field}
              />
            )
          }}
        />
      )
    case 'hidden':
      return <input {...defaultProps} type="hidden" />
    default:
      return null
  }
}

export function Label(text: string) {
  return (
    <label
      htmlFor="about"
      className="text-contrast block text-sm font-medium sm:mt-px sm:pt-2"
    >
      {text}
    </label>
  )
}

export function Error({ text }: { text: string }) {
  return (
    <div className="mt-2 sm:col-span-2 sm:mt-0">
      <p className="text-sm text-red-600 sm:text-xs">{text}</p>
    </div>
  )
}

export function Form<T extends FieldValues = FieldValues>(props: FormProps<T>) {
  const {
    fields,
    options,
    className,
    formHooks,
    title,
    id,
    description,
    onSubmit
  } = props
  const {
    formState: { errors }
  } = formHooks
  return (
    <form className={className} id={id || title} onSubmit={onSubmit}>
      <div className="relative bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
        {options && (
          <div className="absolute top-7 right-4 sm:top-5">
            <OptionsMenu options={options} />
          </div>
        )}
        <div className="sm:flex sm:items-start">
          <div className="mt-3 text-center sm:mt-0 sm:text-left">
            <Dialog.Title
              as="h3"
              className="text-lg font-medium leading-6 text-gray-900"
            >
              {title}
            </Dialog.Title>
            {description && (
              <div className="mt-2">
                <p className="text-contrast-LIGHT text-sm">{description}</p>
              </div>
            )}
          </div>
        </div>
        <div className="space-y-8 divide-y divide-gray-200 sm:space-y-5">
          <div>
            <div>
              {description && (
                <p className="text-contrast-LIGHT mt-1 max-w-2xl text-sm">
                  {description}
                </p>
              )}
            </div>

            <div className="mt-6 space-y-6 sm:mt-5 sm:space-y-5">
              {fields
                ?.filter((field) => field.type === 'hidden')
                .map((field) => (
                  <RenderField key={field.path} field={field} props={props} />
                ))}
              {fields
                ?.filter((field) => field.type !== 'hidden')
                .map((field) => {
                  const label = field?.label || field.path
                  const error = get(errors, field.path)
                  return (
                    <div
                      key={field.id}
                      className="sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:border-t sm:border-gray-200 sm:pt-5"
                    >
                      <label
                        htmlFor={label}
                        className="text-contrast block text-sm font-medium capitalize sm:mt-px"
                      >
                        {label}
                      </label>
                      <div className="mt-1 sm:col-span-2 sm:mt-0">
                        {field.type === 'custom' ? (
                          field.component
                        ) : (
                          <RenderField field={field} props={props} />
                        )}
                      </div>
                      {error && (
                        <p className="text-red-500">
                          {error.message || 'This field is required'}
                        </p>
                      )}
                    </div>
                  )
                })}
            </div>
          </div>
        </div>
      </div>
    </form>
  )
}

export function StandaloneForm<T extends FieldValues = FieldValues>(
  props: FormProps<T> & {
    handleSubmit: () => void
    preForm?: React.ReactNode
  }
) {
  const {
    fields,
    className,
    preForm,
    formHooks,
    title,
    id,
    description,
    onSubmit,
    handleSubmit
  } = props
  const {
    reset,
    formState: { errors, isDirty }
  } = formHooks

  useEffect(() => {
    const listener = (event: KeyboardEvent) => {
      if (!isDirty || event.isComposing) {
        return
      }

      if (event.code === 'KeyS' && (event.ctrlKey || event.metaKey)) {
        event.preventDefault()
        event.stopImmediatePropagation()
        handleSubmit()
      }
      if (event.code === 'Esc' || event.code === 'Escape') {
        event.preventDefault()
        reset()
      }
    }
    document.addEventListener('keydown', listener)
    return () => {
      document.removeEventListener('keydown', listener)
    }
  }, [handleSubmit, isDirty, reset])

  useDirty({ isDirty })

  return (
    <div className="space-y-6 py-2">
      <div className="rounded-md bg-slate-50 px-4 py-5 shadow dark:bg-slate-600 sm:rounded-lg sm:p-6">
        {isDirty && (
          <div className="fixed inset-x-0 top-0 z-20 bg-red-50 p-4">
            <div className="mx-1 flex w-full items-center justify-around sm:hidden">
              <strong className="prose">Editing Card</strong>
              <div className="flex w-2/5 flex-nowrap space-x-4">
                <Button primary onClick={handleSubmit}>
                  Save
                </Button>
                <Button
                  onClick={() => {
                    reset()
                  }}
                >
                  Cancel
                </Button>
              </div>
            </div>
            <p className="hidden text-center sm:block">
              Editing... Press ESC to cancel or Ctrl/Cmd+S to save
            </p>
          </div>
        )}
        <div className="xl:grid xl:grid-cols-4 xl:gap-6">
          <div className="min-w-min xl:col-span-1">
            <h3 className="text-contrast text-xl font-medium leading-6">
              {title}
            </h3>
            <div className="text-contrast-light mt-1 text-sm">
              {description}
            </div>
          </div>
          <div className="mt-5 xl:col-span-3 xl:mt-0">
            {preForm}
            <form
              className={classNames('space-y-6', className)}
              id={id || title}
              onSubmit={onSubmit}
            >
              {fields
                ?.filter((field) => field.type !== 'hidden')
                .map((field) => {
                  const label = field?.label || field.path
                  const error = get(errors, field.path)
                  return (
                    <div key={field.id || field.label}>
                      <label
                        htmlFor={label}
                        className="text-contrast block text-sm font-medium"
                      >
                        {label}
                      </label>
                      <div className="mt-1">
                        {field.type === 'custom' ? (
                          field.component
                        ) : (
                          <RenderField field={field} props={props} />
                        )}
                      </div>
                      {field.description && (
                        <p className="text-contrast-LIGHT mt-2 text-sm">
                          {field.description}
                        </p>
                      )}
                      {error && (
                        <p className="text-red-500">
                          {error.message || 'This field is required'}
                        </p>
                      )}
                    </div>
                  )
                })}
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
