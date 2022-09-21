import { cardIVStage, FeedbackForCardQuery } from '../../site'
import { Button, TipTapContent, IconForScore } from '../../ui-common'
import { formatDate, notEmpty } from '../../utils'
import { useState, useRef, useEffect } from 'react'

export function InterviewFeedback({
  feedbackForCard
}: {
  feedbackForCard: FeedbackForCardQuery['feedbackForCard']
}) {
  const [questionNumber, setQuestionNumber] = useState(0)

  const feebackItemsCount = feedbackForCard?.length || 0

  const question = feedbackForCard?.[questionNumber]
  const questions = question?.questions || []
  const ref = useRef<HTMLDivElement>(null)
  const scrollToTop = () => {
    ref.current?.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
  }
  const handleNext = () => {
    setQuestionNumber((prev) => prev + 1)
    scrollToTop()
  }

  const handlePrev = () => {
    setQuestionNumber((prev) => prev - 1)
    scrollToTop()
  }
  useEffect(() => {
    setTimeout(() => ref.current?.scrollTo({ top: 0 }), 50)
  }, [questionNumber])

  const IVStage = question && cardIVStage(question)
  const feedbackDate =
    question?._siteValidTime && new Date(question._siteValidTime)
  const formattedFeedbackDate = feedbackDate && formatDate(feedbackDate)

  return (
    <>
      <div ref={ref} className="relative overflow-auto bg-white py-2 text-left">
        <div className="relative px-4 sm:px-6 lg:px-8">
          <div className="mx-auto flex max-w-prose flex-col text-lg">
            {' '}
            <h1>
              <span className="block text-center text-base font-semibold uppercase tracking-wide text-indigo-600">
                Feedback by {question?._siteSubject}
              </span>
              {formattedFeedbackDate && (
                <span className="block text-center text-sm font-semibold uppercase tracking-wide text-gray-400">
                  On {formattedFeedbackDate}
                </span>
              )}
              {IVStage && (
                <span className="block text-center text-sm font-semibold uppercase tracking-wide text-gray-600">
                  {IVStage.name}
                </span>
              )}
              <span className="mt-2 block text-center text-3xl font-extrabold leading-8 tracking-tight text-gray-900 sm:text-4xl">
                Summary
              </span>
            </h1>
            <strong className="my-2 inline-block self-center">
              Overall Suggestion -{' '}
              <IconForScore withLabel score={question?.overallScore} />
            </strong>
            <div className="rounded-lg bg-indigo-50">
              <div className="py-2 px-4 sm:py-3 sm:px-6 lg:px-8">
                <p className="prose-sm sm:prose lg:prose-xl leading-5 text-black">
                  {question?.summary ? (
                    <TipTapContent
                      fullHeight
                      className="m-0"
                      htmlString={question?.summary}
                    />
                  ) : (
                    'No summary'
                  )}
                </p>
              </div>
            </div>
            {questions.filter(notEmpty).map((q) => (
              <div key={q.question} className="mt-8">
                <h2 className="text-xl font-extrabold text-gray-900 sm:text-2xl">
                  {q.question}
                </h2>
                <div className=" py-2 px-4 sm:py-4 sm:px-6">
                  <p className="prose leading-5">
                    {q.response ? (
                      <TipTapContent
                        fullHeight
                        className="m-0"
                        htmlString={q.response}
                      />
                    ) : (
                      'No response'
                    )}
                  </p>
                </div>

                <strong>{q?.scoreCardsLabel}</strong>
                <div className="mt-4">
                  {q.scoreCards.map((sc) => (
                    <div
                      key={q.question + sc.text}
                      className="flex items-center justify-between border border-b-0 py-3 first:rounded-t last:rounded-b last:border-b"
                    >
                      <p className=" px-4 font-semibold text-gray-600">
                        {sc.text}
                      </p>
                      <div className="pr-3 font-bold">
                        <IconForScore score={sc.score} withLabel />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
      <nav
        className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 sm:px-6"
        aria-label="Pagination"
      >
        <div className="hidden sm:block">
          <p className="text-sm text-gray-700">
            Feedback <span className="font-medium">{questionNumber + 1}</span>{' '}
            of <span className="font-medium">{feebackItemsCount}</span>
          </p>
        </div>
        <div className="flex flex-1 justify-between sm:justify-end">
          <Button
            onClick={() => {
              ref.current?.scrollTo({
                top: 0,
                behavior: 'smooth'
              })
              handlePrev()
            }}
            disabled={questionNumber === 0}
            className="mr-2"
          >
            Previous
          </Button>

          <Button
            disabled={questionNumber === feebackItemsCount - 1}
            className="relative inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            onClick={handleNext}
          >
            Next
          </Button>
        </div>
      </nav>
    </>
  )
}
