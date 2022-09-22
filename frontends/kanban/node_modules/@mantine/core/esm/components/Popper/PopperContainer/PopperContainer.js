import React from 'react';
import { getDefaultZIndex } from '@mantine/styles';
import { Portal } from '../../Portal/Portal.js';

function PopperContainer({
  children,
  zIndex = getDefaultZIndex("popover"),
  className,
  withinPortal = true
}) {
  if (withinPortal) {
    return /* @__PURE__ */ React.createElement(Portal, {
      className,
      zIndex
    }, children);
  }
  return /* @__PURE__ */ React.createElement("div", {
    className,
    style: { position: "relative", zIndex }
  }, children);
}
PopperContainer.displayName = "@mantine/core/PopperContainer";

export { PopperContainer };
//# sourceMappingURL=PopperContainer.js.map
