import {
  FieldArrayWithId,
  useFieldArray,
  useForm,
  UseFormReturn
} from 'react-hook-form'
import { toast } from 'react-toastify'
import { useEffect, useMemo } from 'react'
import { useSearch } from '@tanstack/react-location'
import { useQueryClient } from 'react-query'
import {
  CreateWorkflowProjectMutationVariables,
  LocationGenerics,
  useCreateWorkflowProjectMutation,
  UpdateWorkflowProjectMutationVariables,
  useUpdateWorkflowProjectMutation,
  useCurrentProject,
  purgeAllLists,
  TProject,
  Exact,
  WorkflowProjectInput
} from '../site'
import {
  ModalStateProps,
  ModalForm,
  Form,
  Modal,
  Button,
  DeleteActiveIcon,
  RenderField
} from '../ui-common'
import { defaultMutationProps } from './utils'
import { notEmpty } from '../utils'

type AddProjectInput = CreateWorkflowProjectMutationVariables

type AddProjectModalProps = ModalStateProps

export function AddProjectModal({ isOpen, handleClose }: AddProjectModalProps) {
  const addProjectMutation = useCreateWorkflowProjectMutation({
    ...defaultMutationProps
  })

  const addProject = (input: AddProjectInput) => {
    handleClose()
    purgeAllLists()
    toast.promise(addProjectMutation.mutateAsync(input), {
      pending: 'Creating project...',
      success: 'Project created!',
      error: 'Error creating project'
    })
  }

  const formHooks = useForm<AddProjectInput>()

  return (
    <ModalForm<AddProjectInput>
      title="Add Project"
      formHooks={formHooks}
      fields={[
        {
          id: 'ProjectName',
          placeholder: 'Project Name',
          label: 'Project Name',
          type: 'text',
          rules: {
            required: true
          },
          path: 'workflowProject.name'
        },
        {
          id: 'ProjectDescription',
          label: 'Project Description',
          placeholder: 'Project Description',
          type: 'text',
          path: 'workflowProject.description'
        }
      ]}
      onSubmit={formHooks.handleSubmit(addProject, console.warn)}
      isOpen={isOpen}
      handleClose={handleClose}
    />
  )
}

function OpenRolesForm({
  formHooks,
  openRoleIndex,
  onRemove
}: {
  formHooks: UseFormReturn<UpdateWorkflowProjectMutationVariables, object>
  openRoleIndex: number
  onRemove: () => void
}) {
  return (
    <>
      <div className="my-2 flex items-center first:mt-0">
        <p>Role {openRoleIndex + 1}</p>
        <button
          onClick={onRemove}
          type="button"
          title="Remove Question"
          className="cursor-pointer"
        >
          <DeleteActiveIcon
            fill="pink"
            stroke="red"
            className="ml-2 h-5 w-5 opacity-80 hover:opacity-100"
          />
        </button>
      </div>
      <RenderField
        field={{
          id: 'RoleCount',
          path: `workflowProject.openRoles.${openRoleIndex}.count`,
          placeholder: 'Number of currently open positions',
          type: 'number'
        }}
        props={{
          className: 'mt-2',
          formHooks
        }}
      />
      <RenderField
        field={{
          id: 'RoleName',
          placeholder: 'Role Name',
          path: `workflowProject.openRoles.${openRoleIndex}.name`,
          type: 'text'
        }}
        props={{
          className: 'mt-2',
          formHooks
        }}
      />
    </>
  )
}

function OpenRolesArray({
  formHooks
}: {
  formHooks: UseFormReturn<UpdateWorkflowProjectMutationVariables, object>
}) {
  const { control } = formHooks
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'workflowProject.openRoles'
  })
  return (
    <div className="py-2">
      {fields.map((field, i) => (
        <OpenRolesForm
          key={field.id}
          formHooks={formHooks}
          openRoleIndex={i}
          onRemove={() => remove(i)}
        />
      ))}
      <Button primary noMargin className="mt-2 p-0" onClick={() => append({})}>
        Add Open Role
      </Button>
    </div>
  )
}

type UpdateWorkflowProjectInput = UpdateWorkflowProjectMutationVariables

type UpdateWorkflowProjectModalProps = ModalStateProps & {
  workflowId: string
  project: TProject
}

function UpdateWorkflowProjectModal({
  isOpen,
  handleClose,
  project,
  workflowId
}: UpdateWorkflowProjectModalProps) {
  const { modalState } = useSearch<LocationGenerics>()
  const workflowProjectId = modalState?.workflowProjectId
  const queryClient = useQueryClient()
  const UpdateWorkflowProjectMutation = useUpdateWorkflowProjectMutation({
    ...defaultMutationProps(queryClient, workflowId),
    onSuccess: (data) => {
      splitbee.track('Update Project', {
        data: JSON.stringify(data)
      })
    }
  })

  const UpdateWorkflowProject = (input: UpdateWorkflowProjectInput) => {
    handleClose()
    UpdateWorkflowProjectMutation.mutate({ ...input })
  }
  const defaultValues = useMemo(
    () => ({
      workflowProjectId,
      workflowProject: {
        ...project,
        openRoles: project.openRoles?.filter(notEmpty) ?? []
      }
    }),
    [workflowProjectId, project]
  )

  const formHooks = useForm<UpdateWorkflowProjectInput>({
    defaultValues
  })

  useEffect(() => {
    formHooks.reset({ ...defaultValues })
  }, [defaultValues, formHooks, project])

  const title = 'Update Project'
  return (
    <Modal key={project.name} isOpen={isOpen} handleClose={handleClose}>
      <Form
        title={title}
        formHooks={formHooks}
        fields={[
          {
            id: 'ProjectName',
            placeholder: 'Project Name',
            type: 'text',
            rules: {
              required: true
            },
            path: 'workflowProject.name',
            label: 'Name'
          },
          {
            id: 'ProjectDescription',
            label: 'Project Description',
            placeholder: 'Project Description',
            type: 'text',
            path: 'workflowProject.description'
          },
          {
            type: 'custom',
            component: <OpenRolesArray formHooks={formHooks} />,
            label: 'Open Roles',
            path: 'workflowProject.openRoles'
          }
        ]}
        onSubmit={formHooks.handleSubmit(UpdateWorkflowProject, console.warn)}
      />
      <div className="bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
        <button
          type="submit"
          form={title}
          className="inline-flex w-full justify-center rounded-md border border-transparent bg-blue-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm"
        >
          Submit
        </button>
        <button
          type="button"
          className="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm"
          onClick={() => handleClose()}
        >
          Cancel
        </button>
      </div>
    </Modal>
  )
}

export function UpdateWorkflowProjectModalWrapper({
  isOpen,
  handleClose,
  workflowId
}: Omit<UpdateWorkflowProjectModalProps, 'project'>) {
  const { data: project, isLoading } = useCurrentProject(workflowId)
  return (
    <>
      {isLoading && <div>Loading...</div>}
      {project && (
        <UpdateWorkflowProjectModal
          {...{ isOpen, handleClose, project, workflowId }}
        />
      )}
    </>
  )
}
