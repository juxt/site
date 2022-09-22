'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

function useDebouncedValue(value, wait, options = { leading: false }) {
  const [_value, setValue] = react.useState(value);
  const mountedRef = react.useRef(false);
  const timeoutRef = react.useRef(null);
  const cooldownRef = react.useRef(false);
  const cancel = () => window.clearTimeout(timeoutRef.current);
  react.useEffect(() => {
    if (mountedRef.current) {
      if (!cooldownRef.current && options.leading) {
        cooldownRef.current = true;
        setValue(value);
      } else {
        cancel();
        timeoutRef.current = window.setTimeout(() => {
          cooldownRef.current = false;
          setValue(value);
        }, wait);
      }
    }
  }, [value, options.leading]);
  react.useEffect(() => {
    mountedRef.current = true;
    return cancel;
  }, []);
  return [_value, cancel];
}

exports.useDebouncedValue = useDebouncedValue;
//# sourceMappingURL=use-debounced-value.js.map
