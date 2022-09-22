import React from 'react';
import { Checkbox } from '../../Checkbox/Checkbox.js';

const DefaultItem = React.memo(({ data, selected }) => /* @__PURE__ */ React.createElement(Checkbox, {
  checked: selected,
  onChange: () => {
  },
  label: data.label,
  tabIndex: -1,
  sx: { pointerEvents: "none" }
}));

export { DefaultItem };
//# sourceMappingURL=DefaultItem.js.map
