import { useRef, useCallback, useEffect } from 'react';
import { FOCUS_SELECTOR, tabbable, focusable } from './tabbable.js';
import { scopeTab } from './scope-tab.js';
import { createAriaHider } from './create-aria-hider.js';

function useFocusTrap(active = true) {
  const ref = useRef();
  const restoreAria = useRef(null);
  const setRef = useCallback((node) => {
    if (!active) {
      return;
    }
    if (restoreAria.current) {
      restoreAria.current();
    }
    if (node) {
      const processNode = (_node) => {
        restoreAria.current = createAriaHider(_node);
        let focusElement = node.querySelector("[data-autofocus]");
        if (!focusElement) {
          const children = Array.from(node.querySelectorAll(FOCUS_SELECTOR));
          focusElement = children.find(tabbable) || children.find(focusable) || null;
          if (!focusElement && focusable(node))
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
  useEffect(() => {
    if (!active) {
      return void 0;
    }
    const handleKeyDown = (event) => {
      if (event.key === "Tab" && ref.current) {
        scopeTab(ref.current, event);
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [active]);
  return setRef;
}

export { useFocusTrap };
//# sourceMappingURL=use-focus-trap.js.map
