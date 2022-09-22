import { useCallback } from 'react';
import { assignRef } from '../utils/assign-ref/assign-ref.js';

function useMergedRef(...refs) {
  return useCallback((node) => {
    refs.forEach((ref) => assignRef(ref, node));
  }, refs);
}
function mergeRefs(...refs) {
  return (node) => {
    refs.forEach((ref) => assignRef(ref, node));
  };
}

export { mergeRefs, useMergedRef };
//# sourceMappingURL=use-merged-ref.js.map
