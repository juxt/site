import { IconProps, ModalStateProps, TiptapProps } from '../index'
import { ReactNode, BaseSyntheticEvent } from 'react'
import { DropzoneProps } from 'react-dropzone'
import {
  FieldArrayPath,
  FieldPath,
  FieldValues,
  RegisterOptions,
  UseFormReturn
} from 'react-hook-form'

export type Option = {
  value: string
  label: string
  key?: string
  disabled?: boolean
}

export type ISelectProps = {
  options: Option[]
  value: Option[]
  onChange?: (value: Option[]) => void
  valueRenderer?: (selected: Option[], options: Option[]) => ReactNode
  ItemRenderer?: (option: Option) => ReactNode
  isLoading?: boolean
  disabled?: boolean
  disableSearch?: boolean
  shouldToggleOnHover?: boolean
  hasSelectAll?: boolean
  filterOptions?: (
    options: Option[],
    filter: string
  ) => Promise<Option[]> | Option[]
  overrideStrings?: { [key: string]: string }
  labelledBy: string
  className?: string
  onMenuToggle?: () => void
  ClearIcon?: ReactNode
  debounceDuration?: number
  ClearSelectedIcon?: ReactNode
  defaultIsOpen?: boolean
  isOpen?: boolean
  isCreatable?: boolean
  onCreateOption?: (option: string) => void
}

type WithIdAndType<
  T extends
    | 'text'
    | 'number'
    | 'checkbox'
    | 'textarea'
    | 'submit'
    | 'file'
    | 'multifile'
    | 'select'
    | 'multiselect'
    | 'tiptap'
    | 'custom'
    | 'hidden'
> = {
  id?: string
  type: T
}

type InputProps = React.InputHTMLAttributes<HTMLInputElement>
type TextInputProps = InputProps & {
  type: 'text'
  value?: string
}
type HiddenInputProps = InputProps & {
  type: 'hidden'
  value: string
}
type FileInputProps = InputProps &
  DropzoneProps & {
    type: 'file'
  }
type MultiFileInputProps = InputProps &
  DropzoneProps & {
    type: 'multifile'
  }
type NumberInputProps = InputProps & {
  type: 'number'
  value?: number
}
type CheckboxProps = InputProps & {
  type: 'checkbox'
  value?: boolean
}
type SubmitProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  type: 'submit'
}
type TextAreaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement> & {
  type: 'textarea'
  value?: string
}
type SelectProps = {
  type: 'select'
  options: Option[]
  default?: string
}

export type TextInputDefinition = TextInputProps & WithIdAndType<'text'>

export type HiddenInputDefinition = HiddenInputProps & WithIdAndType<'hidden'>

export type FileInputDefinition = FileInputProps & WithIdAndType<'file'>

export type MultiFileInputDefinition = MultiFileInputProps &
  WithIdAndType<'multifile'>

export type NumberInputDefinition = NumberInputProps & WithIdAndType<'number'>

export type CheckboxInputDefinition = CheckboxProps & WithIdAndType<'checkbox'>

export type TextAreaInputDefinition = TextAreaProps & WithIdAndType<'textarea'>

export type TiptapDefinition = Omit<TiptapProps, 'onChange'> &
  WithIdAndType<'tiptap'>

export type SelectInputDefinition = SelectProps & WithIdAndType<'select'>

export type MultiSelectDefinition = {
  options: Option[]
} & Omit<ISelectProps, 'labelledBy' | 'value'> &
  WithIdAndType<'multiselect'>

export type SubmitButtonProps = SubmitProps & WithIdAndType<'submit'>

export type CustomInputProps = {
  component: JSX.Element
} & WithIdAndType<'custom'>

export type FormInputField<T extends FieldValues> = {
  path: FieldPath<T>
  id?: string
  label?: string
  description?: string
  required?: boolean
  rules?: RegisterOptions
} & (
  | TextInputDefinition
  | HiddenInputDefinition
  | NumberInputDefinition
  | CheckboxInputDefinition
  | FileInputDefinition
  | MultiFileInputDefinition
  | TextAreaInputDefinition
  | SelectInputDefinition
  | MultiSelectDefinition
  | TiptapDefinition
  | CustomInputProps
)

export type FormProps<T extends FieldValues> = {
  fields?: FormInputField<T>[]
  options?: {
    label: string
    id: string
    Icon: (props: IconProps) => JSX.Element
    ActiveIcon: (props: IconProps) => JSX.Element
    props: {
      onClick: () => void
    }
  }[]
  className?: string
  id?: string
  handleDelete?: () => void
  formHooks: UseFormReturn<T, object>
  title?: string
  description?: string | ReactNode
  onSubmit?: (e: BaseSyntheticEvent) => void
}

export type ModalFormProps<T extends FieldValues> = FormProps<T> &
  ModalStateProps
