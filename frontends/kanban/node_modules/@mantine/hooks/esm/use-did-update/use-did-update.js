import { useRef, useEffect } from 'react';

function useDidUpdate(fn, dependencies) {
  const mounted = useRef(false);
  useEffect(() => {
    if (mounted.current) {
      fn();
    } else {
      mounted.current = true;
    }
  }, dependencies);
}

export { useDidUpdate };
//# sourceMappingURL=use-did-update.js.map
