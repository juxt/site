import { useMediaQuery } from '../use-media-query/use-media-query.js';

function useColorScheme() {
  return useMediaQuery("(prefers-color-scheme: dark)") ? "dark" : "light";
}

export { useColorScheme };
//# sourceMappingURL=use-color-scheme.js.map
