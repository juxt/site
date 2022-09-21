export function cardIVStage({ stateStr }: { stateStr?: string | null }) {
  switch (stateStr) {
    case 'WorkflowStateAwaitStage1IV':
      return {
        id: 'WorkflowStateAwaitStage1IV',
        name: 'Introduction Interview'
      }
    case 'WorkflowStateTakeHomeReview':
      return {
        id: 'WorkflowStateTakeHomeReview',
        name: 'Take Home Interview'
      }
    case 'WorkflowStatePP':
      return {
        id: 'WorkflowStatePP',
        name: 'Pair Programming Interview'
      }
    default:
      return null
  }
}
