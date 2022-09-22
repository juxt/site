import React from 'react';
import { CloseButton } from '../../ActionIcon/CloseButton/CloseButton.js';
import { ChevronIcon } from './ChevronIcon.js';

function SelectRightSection({
  shouldClear,
  clearButtonLabel,
  onClear,
  size,
  error
}) {
  return shouldClear ? /* @__PURE__ */ React.createElement(CloseButton, {
    variant: "transparent",
    "aria-label": clearButtonLabel,
    onClick: onClear,
    size
  }) : /* @__PURE__ */ React.createElement(ChevronIcon, {
    error,
    size
  });
}
SelectRightSection.displayName = "@mantine/core/SelectRightSection";

export { SelectRightSection };
//# sourceMappingURL=SelectRightSection.js.map
