'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var createCache = require('@emotion/cache');
var MantineProvider = require('../theme/MantineProvider.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var createCache__default = /*#__PURE__*/_interopDefaultLegacy(createCache);

const defaultCacheOptions = {
  key: "mantine",
  prepend: true
};
const { getCache } = (() => {
  let cache;
  let _key = defaultCacheOptions.key;
  function _getCache(options) {
    if (cache === void 0 || _key !== (options == null ? void 0 : options.key)) {
      _key = (options == null ? void 0 : options.key) || "mantine";
      cache = createCache__default(options || defaultCacheOptions);
    }
    return cache;
  }
  return { getCache: _getCache };
})();
function useEmotionCache() {
  const options = MantineProvider.useMantineEmotionOptions();
  return getCache(options);
}

exports.getCache = getCache;
exports.useEmotionCache = useEmotionCache;
//# sourceMappingURL=use-emotion-cache.js.map
