import { ChatAltIcon } from '@heroicons/react/solid'
import * as yup from 'yup'
import { yupResolver } from '@hookform/resolvers/yup'
import takeRight from 'lodash-es/takeRight'
import {
  useCommentForEntity,
  useCommentsForCardQuery,
  useCreateCommentMutation,
  useDeleteCommentMutation,
  CreateCommentMutationVariables,
  CommentInputSchema,
  TComment,
  useUser,
  useModalForm,
  userAvatar,
  asOfAtom,
  purgeQueries
} from '../../site'
import { useCallback, BaseSyntheticEvent, useEffect, useState } from 'react'
import { useForm, useFormState } from 'react-hook-form'
import { useQueryClient } from 'react-query'
import { toast } from 'react-toastify'
import { DeleteInactiveIcon, DeleteActiveIcon } from '../Icons'
import { OptionsMenu } from '../Menus'
import { RenderField, ErrorMessage } from './Components'
import { useDirty } from './hooks'
import { TipTapContent } from '../Tiptap/Tiptap'
import { Button } from '../Buttons'
import { useAtom } from 'jotai'

function CommentForm({
  userProfileImg,
  eId,
  commentMutationProps
}: {
  userProfileImg: string
  eId: string
  commentMutationProps: {
    onSettled: () => void
  }
}) {
  const addCommentMutation = useCreateCommentMutation(commentMutationProps)
  const addComment = useCallback(
    (input: CreateCommentMutationVariables) => {
      if (input.Comment.text !== '' && input.Comment.text !== '<p></p>') {
        toast.promise(
          addCommentMutation.mutateAsync({
            Comment: {
              ...input.Comment,
              cardId: eId
            }
          }),
          {
            pending: 'Adding comment...',
            error: 'Error adding comment'
          },
          {
            autoClose: 500
          }
        )
      }
    },
    [addCommentMutation, eId]
  )

  const schema = yup.object({ Comment: CommentInputSchema() })
  const formHooks = useForm<CreateCommentMutationVariables>({
    resolver: yupResolver(schema),
    defaultValues: {
      Comment: {
        cardId: eId,
        text: '<p></p>'
      }
    }
  })
  const { handleSubmit, reset, control } = formHooks
  const { isDirty } = useFormState({ control })

  useDirty({ isDirty })

  const submitComment = useCallback(
    (e?: BaseSyntheticEvent) => {
      if (!isDirty) {
        e?.preventDefault()
        return
      }
      handleSubmit(addComment, console.warn)(e)
      reset()
    },
    [isDirty, handleSubmit, addComment, reset]
  )

  const commentFormProps = {
    formHooks,
    cardId: eId,
    onSubmit: submitComment
  }
  const error = formHooks.formState.errors.Comment?.text

  useEffect(() => {
    const listener = (event: KeyboardEvent) => {
      if (
        (event.code === 'Enter' || event.code === 'NumpadEnter') &&
        (event.ctrlKey || event.metaKey)
      ) {
        submitComment()
      }
    }
    document.addEventListener('keydown', listener)
    return () => {
      document.removeEventListener('keydown', listener)
    }
  }, [submitComment])

  return (
    <div className="mt-10">
      <div className="flex space-x-3">
        <div className="shrink-0">
          <div className="relative">
            <img
              className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-400 ring-8 ring-white"
              src={userProfileImg}
              alt={`Profile avatar of ${userProfileImg}`}
            />

            <span className="absolute -bottom-0.5 -right-1 rounded-tl bg-white px-0.5 py-px">
              <ChatAltIcon
                className="h-5 w-5 text-gray-400"
                aria-hidden="true"
              />
            </span>
          </div>
        </div>
        <div className="min-w-0 flex-1 text-left">
          <form onSubmit={commentFormProps.onSubmit}>
            <div>
              <RenderField
                field={{
                  id: 'commentText',
                  path: 'Comment.text',
                  placeholder:
                    'Type a comment (ctrl+enter to send, drag+drop or paste images)',
                  type: 'tiptap'
                }}
                props={commentFormProps}
              />
              <ErrorMessage error={error} />
            </div>
            <div className="my-6 flex items-center justify-end space-x-4">
              <button
                type="submit"
                disabled={!isDirty}
                className="inline-flex items-center justify-center rounded-lg border border-transparent bg-gray-900 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-black focus:outline-none focus:ring-2 focus:ring-gray-900 focus:ring-offset-2 disabled:opacity-20"
              >
                Comment
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

function CommentOptionsMenu({
  comment,
  deleteComment,
  userId
}: {
  comment: TComment
  deleteComment: () => void
  userId: string
}) {
  return (
    <OptionsMenu
      options={[
        {
          label: 'Delete',
          id: 'delete',
          hidden: !userId || userId !== comment?._siteSubject,
          Icon: DeleteInactiveIcon,
          ActiveIcon: DeleteActiveIcon,
          props: {
            onClick: deleteComment
          }
        }
      ]}
    />
  )
}

export function RenderComment({
  comment,
  isNotLast,
  userId,
  userProfileImg,
  linkToCard = false,
  deleteComment
}: {
  comment: TComment
  isNotLast: boolean
  userId?: string | null
  userProfileImg?: string
  linkToCard?: boolean
  deleteComment?: () => void
}) {
  const [, setIsCard] = useModalForm({
    formModalType: 'editCard',
    cardId: comment.card?.id
  })
  return (
    <li className="text-left">
      <div className="relative pb-8">
        {isNotLast ? (
          <span
            className="absolute top-5 left-5 -ml-px h-full w-0.5 bg-gray-200"
            aria-hidden="true"
          />
        ) : null}
        <div className="relative flex items-start space-x-3">
          <div className="relative">
            <img
              className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-400 ring-8 ring-white"
              src={userProfileImg}
              title={comment._siteSubject || 'Unknown user'}
              alt="Avatar"
            />

            <span className="absolute -bottom-0.5 -right-1 rounded-tl bg-white px-0.5 py-px">
              <ChatAltIcon
                className="h-5 w-5 text-gray-400"
                aria-hidden="true"
              />
            </span>
          </div>
          <div className="min-w-0 flex-1">
            <div>
              <div className="flex flex-row justify-between text-sm">
                {userId && deleteComment && userId === comment._siteSubject ? (
                  <CommentOptionsMenu
                    comment={comment}
                    deleteComment={deleteComment}
                    userId={userId}
                  />
                ) : null}
              </div>
              {linkToCard ? (
                <p className="mt-0.5 text-sm text-gray-500">
                  Commented on{' '}
                  <button
                    onClick={() => setIsCard(true)}
                    type="button"
                    className="text-blue-500 underline"
                  >
                    {comment.card?.title}
                  </button>{' '}
                  at {new Date(comment._siteCreatedAt).toLocaleString()}
                </p>
              ) : (
                <p className="mt-0.5 text-sm text-gray-500">
                  Commented {new Date(comment._siteCreatedAt).toLocaleString()}
                </p>
              )}
            </div>
            <TipTapContent
              fullHeight
              className="prose mt-2 max-h-max text-sm text-gray-700"
              htmlString={comment.text}
            />
          </div>
        </div>
      </div>
    </li>
  )
}

export function CommentLoading({ count }: { count: number }) {
  return (
    <>
      {Array(count)
        .fill(0)
        .map((_, idx) => (
          <RenderComment
            key={idx}
            comment={{
              id: `${idx}`,
              text: `loading...`,
              _siteCreatedAt: new Date().toISOString(),
              _siteValidTime: new Date().toISOString(),
              _siteSubject: 'Loading...'
            }}
            isNotLast={idx !== 4}
          />
        ))}
    </>
  )
}

export function CommentSection({ eId }: { eId: string }) {
  const [asOf] = useAtom(asOfAtom)

  const { data, isLoading } = useCommentForEntity(
    { eId, asOf },
    {
      refetchInterval: 5000
    }
  )
  const [commentLimit, setCommentLimit] = useState(5)
  const allComments = data?.commentsForEntity || []
  const comments = takeRight(allComments, commentLimit)
  const { id: userId, avatar } = useUser()

  const queryClient = useQueryClient()
  const commentMutationProps = {
    onSettled: async () => {
      await purgeQueries(['commentsForEntity'])
      setTimeout(() => {
        queryClient.refetchQueries(useCommentsForCardQuery.getKey({ id: eId }))
      }, 100)
    }
  }
  const deleteCommentMutation = useDeleteCommentMutation(commentMutationProps)
  const deleteComment = (commentId: string) => {
    toast.promise(
      deleteCommentMutation.mutateAsync({
        commentId
      }),
      {
        pending: 'Deleting comment...',
        error: 'Error deleting comment'
      },
      {
        autoClose: 1000
      }
    )
  }

  return (
    <section aria-labelledby="activity-title" className="sm:h-full">
      <div className="divide-y divide-gray-200 pr-2">
        <div className="pb-4">
          <h2 className="text-lg font-medium text-gray-900">
            General Comments
          </h2>
        </div>
        <div className="pt-6">
          {/* Activity feed */}
          {comments && (
            <div className="flow-root h-full">
              <ul className="-mb-8">
                {comments.length !== allComments.length && (
                  <Button onClick={() => setCommentLimit(allComments.length)}>
                    Load more
                  </Button>
                )}
                {comments &&
                  comments.map((item, itemIdx) => (
                    <RenderComment
                      key={itemIdx}
                      comment={item}
                      isNotLast={itemIdx !== comments.length - 1}
                      userId={userId}
                      userProfileImg={userAvatar(item._siteSubject)}
                      deleteComment={() => deleteComment(item.id)}
                    />
                  ))}
              </ul>
            </div>
          )}
          {isLoading && <div className="flex justify-center">Loading...</div>}
          <CommentForm
            userProfileImg={avatar}
            eId={eId}
            commentMutationProps={commentMutationProps}
          />
        </div>
      </div>
    </section>
  )
}
