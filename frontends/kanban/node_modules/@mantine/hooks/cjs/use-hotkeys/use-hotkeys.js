'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var parseHotkey = require('./parse-hotkey.js');

function shouldFireEvent(event) {
  if (event.target instanceof HTMLElement) {
    return !["INPUT", "TEXTAREA", "SELECT"].includes(event.target.tagName);
  }
  return true;
}
function useHotkeys(hotkeys) {
  react.useEffect(() => {
    const keydownListener = (event) => {
      hotkeys.forEach(([hotkey, handler]) => {
        if (parseHotkey.getHotkeyMatcher(hotkey)(event) && shouldFireEvent(event)) {
          event.preventDefault();
          handler(event);
        }
      });
    };
    document.documentElement.addEventListener("keydown", keydownListener);
    return () => document.documentElement.removeEventListener("keydown", keydownListener);
  }, [hotkeys]);
}

exports.getHotkeyHandler = parseHotkey.getHotkeyHandler;
exports.useHotkeys = useHotkeys;
//# sourceMappingURL=use-hotkeys.js.map
