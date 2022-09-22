import { useRef } from 'react';
import { randomId } from '../utils/random-id/random-id.js';

function useId(id, generateId = randomId) {
  const generatedId = useRef(generateId());
  return id || generatedId.current;
}

export { useId };
//# sourceMappingURL=use-id.js.map
