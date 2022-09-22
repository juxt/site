import { useRef } from 'react';
import { useDidUpdate } from '@mantine/hooks';

function useAccordionFocus(itemsCount) {
  const controlsRefs = useRef([]);
  const handleItemKeydown = (index) => (event) => {
    var _a, _b;
    if (event.code === "ArrowDown") {
      event.preventDefault();
      const nextFocusElement = controlsRefs.current[index + 1];
      if (nextFocusElement) {
        nextFocusElement.focus();
      } else {
        (_a = controlsRefs.current[0]) == null ? void 0 : _a.focus();
      }
    }
    if (event.code === "ArrowUp") {
      event.preventDefault();
      const previousFocusElement = controlsRefs.current[index - 1];
      if (previousFocusElement) {
        previousFocusElement.focus();
      } else {
        (_b = controlsRefs.current[controlsRefs.current.length - 1]) == null ? void 0 : _b.focus();
      }
    }
  };
  const assignControlRef = (index) => (node) => {
    controlsRefs.current[index] = node;
  };
  useDidUpdate(() => {
    controlsRefs.current = controlsRefs.current.slice(0, itemsCount);
  }, [itemsCount]);
  return { handleItemKeydown, assignControlRef };
}

export { useAccordionFocus };
//# sourceMappingURL=use-accordion-focus.js.map
