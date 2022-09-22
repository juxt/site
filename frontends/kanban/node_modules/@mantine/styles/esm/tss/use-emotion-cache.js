import createCache from '@emotion/cache';
import { useMantineEmotionOptions } from '../theme/MantineProvider.js';

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
      cache = createCache(options || defaultCacheOptions);
    }
    return cache;
  }
  return { getCache: _getCache };
})();
function useEmotionCache() {
  const options = useMantineEmotionOptions();
  return getCache(options);
}

export { getCache, useEmotionCache };
//# sourceMappingURL=use-emotion-cache.js.map
