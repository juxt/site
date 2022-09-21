import { userAvatar, useRecentCommentsQuery, useUser } from '../site'
import {
  CommentLoading,
  Modal,
  ModalStateProps,
  RenderComment
} from '../ui-common'
import { notEmpty } from '../utils'

export function ViewCommentsModal({ isOpen, handleClose }: ModalStateProps) {
  const { data, isLoading } = useRecentCommentsQuery()
  const comments = data?.allComments || []
  const { id: userId } = useUser()

  return (
    <Modal isOpen={isOpen} handleClose={handleClose}>
      <section aria-labelledby="activity-title" className="m-4 sm:h-full">
        <div className="divide-y divide-gray-200 pr-2">
          <div className="pb-4">
            <h2 className="text-lg font-medium text-gray-900">
              Most Recent Comments
            </h2>
          </div>
          <div className="pt-6">
            {comments && (
              <div className="flow-root h-full">
                <ul className="-mb-8 flex flex-col">
                  {comments &&
                    comments
                      .filter(notEmpty)
                      .map((item, itemIdx) => (
                        <RenderComment
                          key={itemIdx}
                          comment={item}
                          isNotLast={itemIdx !== comments.length - 1}
                          userId={userId}
                          linkToCard={true}
                          userProfileImg={userAvatar(item._siteSubject)}
                        />
                      ))}
                  {isLoading && <CommentLoading count={5} />}
                </ul>
              </div>
            )}
          </div>
        </div>
      </section>
    </Modal>
  )
}
