import { useMediaQuery } from '../use-media-query/use-media-query.js';

function useReducedMotion() {
  return useMediaQuery("(prefers-reduced-motion: reduce)");
}

export { useReducedMotion };
//# sourceMappingURL=use-reduced-motion.js.map
