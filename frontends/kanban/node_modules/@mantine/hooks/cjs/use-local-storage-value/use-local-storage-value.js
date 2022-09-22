'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var useWindowEvent = require('../use-window-event/use-window-event.js');

function useLocalStorageValue({
  key,
  defaultValue = void 0
}) {
  const [value, setValue] = react.useState(typeof window !== "undefined" && "localStorage" in window ? window.localStorage.getItem(key) : defaultValue != null ? defaultValue : "");
  const setLocalStorageValue = react.useCallback((val) => {
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
  useWindowEvent.useWindowEvent("storage", (event) => {
    if (event.storageArea === window.localStorage && event.key === key) {
      setValue(event.newValue);
    }
  });
  react.useEffect(() => {
    if (defaultValue && !value) {
      setLocalStorageValue(defaultValue);
    }
  }, [defaultValue, value, setLocalStorageValue]);
  return [value || defaultValue, setLocalStorageValue];
}

exports.useLocalStorageValue = useLocalStorageValue;
//# sourceMappingURL=use-local-storage-value.js.map
