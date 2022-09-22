import { useState } from 'react';

function useToggle(initialValue, options) {
  const [state, setState] = useState(initialValue);
  const toggle = (value) => {
    if (typeof value !== "undefined") {
      setState(value);
    } else {
      setState((current) => {
        if (current === options[0]) {
          return options[1];
        }
        return options[0];
      });
    }
  };
  return [state, toggle];
}
function useBooleanToggle(initialValue = false) {
  return useToggle(initialValue, [true, false]);
}

export { useBooleanToggle, useToggle };
//# sourceMappingURL=use-toggle.js.map
