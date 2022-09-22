import { useState, useCallback, useEffect } from 'react';
import { useWindowEvent } from '../use-window-event/use-window-event.js';

function useLocalStorageValue({
  key,
  defaultValue = void 0
}) {
  const [value, setValue] = useState(typeof window !== "undefined" && "localStorage" in window ? window.localStorage.getItem(key) : defaultValue != null ? defaultValue : "");
  const setLocalStorageValue = useCallback((val) => {
    if (typeof val === "function") {
      setValue((current) => {
        const result = val(current);
        window.localStorage.setItem(key, result);
        return result;
      });
    } else {
      window.localStorage.setItem(key, val);
      setValue(val);
    }
  }, [key]);
  useWindowEvent("storage", (event) => {
    if (event.storageArea === window.localStorage && event.key === key) {
      setValue(event.newValue);
    }
  });
  useEffect(() => {
    if (defaultValue && !value) {
      setLocalStorageValue(defaultValue);
    }
  }, [defaultValue, value, setLocalStorageValue]);
  return [value || defaultValue, setLocalStorageValue];
}

export { useLocalStorageValue };
//# sourceMappingURL=use-local-storage-value.js.map
