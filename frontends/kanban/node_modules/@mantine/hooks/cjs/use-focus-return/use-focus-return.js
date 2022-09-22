'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var useDidUpdate = require('../use-did-update/use-did-update.js');

function useFocusReturn({
  opened,
  transitionDuration,
  shouldReturnFocus = true
}) {
  const lastActiveElement = react.useRef();
  const returnFocus = () => {
    var _a;
    if (lastActiveElement.current && "focus" in lastActiveElement.current && typeof lastActiveElement.current.focus === "function") {
      (_a = lastActiveElement.current) == null ? void 0 : _a.focus();
    }
  };
  useDidUpdate.useDidUpdate(() => {
    let timeout = -1;
    const clearFocusTimeout = (event) => {
      if (event.code === "Tab") {
        window.clearTimeout(timeout);
      }
    };
    document.addEventListener("keydown", clearFocusTimeout);
    if (opened) {
      lastActiveElement.current = document.activeElement;
    } else if (shouldReturnFocus) {
      timeout = window.setTimeout(returnFocus, transitionDuration + 10);
    }
    return () => {
      window.clearTimeout(timeout);
      document.removeEventListener("keydown", clearFocusTimeout);
    };
  }, [opened]);
  return returnFocus;
}

exports.useFocusReturn = useFocusReturn;
//# sourceMappingURL=use-focus-return.js.map
