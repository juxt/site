import { useState } from 'react';
import { useIsomorphicEffect } from '../use-isomorphic-effect/use-isomorphic-effect.js';
import { randomId } from '../utils/random-id/random-id.js';

function useUuid(staticId) {
  const [uuid, setUuid] = useState("");
  useIsomorphicEffect(() => {
    setUuid(randomId());
  }, []);
  return staticId || uuid;
}

export { useUuid };
//# sourceMappingURL=use-uuid.js.map
