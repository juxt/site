'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var styles = require('@mantine/styles');
var SelectScrollArea = require('../SelectScrollArea/SelectScrollArea.js');
var SelectDropdown_styles = require('./SelectDropdown.styles.js');
var Popper = require('../../Popper/Popper.js');
var Paper = require('../../Paper/Paper.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

const SelectDropdown = React.forwardRef(({
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
  styles: styles$1,
  dropdownComponent,
  referenceElement,
  direction = "column",
  onDirectionChange,
  switchDirectionOnFlip = false,
  zIndex = styles.getDefaultZIndex("popover"),
  dropdownPosition = "flip",
  __staticSelector
}, ref) => {
  const { classes } = SelectDropdown_styles['default']({ native: dropdownComponent !== SelectScrollArea.SelectScrollArea }, { classNames, styles: styles$1, name: __staticSelector });
  const previousPlacement = React.useRef("bottom");
  return /* @__PURE__ */ React__default.createElement(Popper.Popper, {
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
  }, /* @__PURE__ */ React__default.createElement("div", {
    style: { maxHeight: maxDropdownHeight, display: "flex" }
  }, /* @__PURE__ */ React__default.createElement(Paper.Paper, {
    component: dropdownComponent || "div",
    id: `${uuid}-items`,
    "aria-labelledby": `${uuid}-label`,
    role: "listbox",
    className: classes.dropdown,
    shadow,
    ref,
    onMouseDown: (event) => event.preventDefault()
  }, /* @__PURE__ */ React__default.createElement("div", {
    style: { display: "flex", flexDirection: direction, width: "100%" }
  }, children))));
});
SelectDropdown.displayName = "@mantine/core/SelectDropdown";

exports.SelectDropdown = SelectDropdown;
//# sourceMappingURL=SelectDropdown.js.map
