'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var tabbable = require('./tabbable.js');
var scopeTab = require('./scope-tab.js');
var createAriaHider = require('./create-aria-hider.js');

function useFocusTrap(active = true) {
  const ref = react.useRef();
  const restoreAria = react.useRef(null);
  const setRef = react.useCallback((node) => {
    if (!active) {
      return;
    }
    if (restoreAria.current) {
      restoreAria.current();
    }
    if (node) {
      const processNode = (_node) => {
        restoreAria.current = createAriaHider.createAriaHider(_node);
        let focusElement = node.querySelector("[data-autofocus]");
        if (!focusElement) {
          const children = Array.from(node.querySelectorAll(tabbable.FOCUS_SELECTOR));
          focusElement = children.find(tabbable.tabbable) || children.find(tabbable.focusable) || null;
          if (!focusElement && tabbable.focusable(node))
            focusElement = node;
        }
        if (focusElement) {
          focusElement.focus();
        } else if (process.env.NODE_ENV === "development") {
          console.warn("[@mantine/hooks/use-focus-trap] Failed to find focusable element within provided node", node);
        }
      };
      setTimeout(() => {
        if (node.ownerDocument) {
          processNode(node);
        } else if (process.env.NODE_ENV === "development") {
          console.warn("[@mantine/hooks/use-focus-trap] Ref node is not part of the dom", node);
        }
      });
      ref.current = node;
    } else {
      ref.current = null;
    }
  }, [active]);
  react.useEffect(() => {
    if (!active) {
      return void 0;
    }
    const handleKeyDown = (event) => {
      if (event.key === "Tab" && ref.current) {
        scopeTab.scopeTab(ref.current, event);
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [active]);
  return setRef;
}

exports.useFocusTrap = useFocusTrap;
//# sourceMappingURL=use-focus-trap.js.map
