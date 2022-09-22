'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var useIsomorphicEffect = require('../use-isomorphic-effect/use-isomorphic-effect.js');
var randomId = require('../utils/random-id/random-id.js');

function useUuid(staticId) {
  const [uuid, setUuid] = react.useState("");
  useIsomorphicEffect.useIsomorphicEffect(() => {
    setUuid(randomId.randomId());
  }, []);
  return staticId || uuid;
}

exports.useUuid = useUuid;
//# sourceMappingURL=use-uuid.js.map
