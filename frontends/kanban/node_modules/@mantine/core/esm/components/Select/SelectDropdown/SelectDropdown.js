import React, { forwardRef, useRef } from 'react';
import { getDefaultZIndex } from '@mantine/styles';
import { SelectScrollArea } from '../SelectScrollArea/SelectScrollArea.js';
import useStyles from './SelectDropdown.styles.js';
import { Popper } from '../../Popper/Popper.js';
import { Paper } from '../../Paper/Paper.js';

const SelectDropdown = forwardRef(({
  mounted,
  transition,
  transitionDuration,
  transitionTimingFunction,
  uuid,
  shadow,
  maxDropdownHeight,
  withinPortal = true,
  children,
  classNames,
  styles,
  dropdownComponent,
  referenceElement,
  direction = "column",
  onDirectionChange,
  switchDirectionOnFlip = false,
  zIndex = getDefaultZIndex("popover"),
  dropdownPosition = "flip",
  __staticSelector
}, ref) => {
  const { classes } = useStyles({ native: dropdownComponent !== SelectScrollArea }, { classNames, styles, name: __staticSelector });
  const previousPlacement = useRef("bottom");
  return /* @__PURE__ */ React.createElement(Popper, {
    referenceElement,
    mounted,
    transition,
    transitionDuration,
    exitTransitionDuration: 0,
    transitionTimingFunction,
    position: dropdownPosition === "flip" ? "bottom" : dropdownPosition,
    withinPortal,
    zIndex,
    modifiers: [
      {
        name: "preventOverflow",
        enabled: false
      },
      {
        name: "flip",
        enabled: dropdownPosition === "flip"
      },
      {
        name: "sameWidth",
        enabled: true,
        phase: "beforeWrite",
        requires: ["computeStyles"],
        fn: ({ state }) => {
          state.styles.popper.width = `${state.rects.reference.width}px`;
        },
        effect: ({ state }) => {
          state.elements.popper.style.width = `${state.elements.reference.offsetWidth}px`;
        }
      },
      {
        name: "directionControl",
        enabled: true,
        phase: "main",
        fn: ({ state }) => {
          if (previousPlacement.current !== state.placement) {
            previousPlacement.current = state.placement;
            const nextDirection = state.placement === "top" ? "column-reverse" : "column";
            if (direction !== nextDirection && switchDirectionOnFlip) {
              onDirectionChange && onDirectionChange(nextDirection);
            }
          }
        }
      }
    ]
  }, /* @__PURE__ */ React.createElement("div", {
    style: { maxHeight: maxDropdownHeight, display: "flex" }
  }, /* @__PURE__ */ React.createElement(Paper, {
    component: dropdownComponent || "div",
    id: `${uuid}-items`,
    "aria-labelledby": `${uuid}-label`,
    role: "listbox",
    className: classes.dropdown,
    shadow,
    ref,
    onMouseDown: (event) => event.preventDefault()
  }, /* @__PURE__ */ React.createElement("div", {
    style: { display: "flex", flexDirection: direction, width: "100%" }
  }, children))));
});
SelectDropdown.displayName = "@mantine/core/SelectDropdown";

export { SelectDropdown };
//# sourceMappingURL=SelectDropdown.js.map
