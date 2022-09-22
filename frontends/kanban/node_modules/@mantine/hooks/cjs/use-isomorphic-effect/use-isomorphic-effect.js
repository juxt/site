'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

const useIsomorphicEffect = typeof document !== "undefined" ? react.useLayoutEffect : react.useEffect;

exports.useIsomorphicEffect = useIsomorphicEffect;
//# sourceMappingURL=use-isomorphic-effect.js.map
