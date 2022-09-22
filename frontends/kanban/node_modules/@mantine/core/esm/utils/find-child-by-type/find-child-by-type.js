import { Children } from 'react';

function findChildByType(children, type) {
  return Children.toArray(children).find((item) => item.type === type);
}

export { findChildByType };
//# sourceMappingURL=find-child-by-type.js.map
