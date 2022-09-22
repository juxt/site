import { Children } from 'react';

function filterChildrenByType(children, type) {
  return Children.toArray(children).filter((item) => Array.isArray(type) ? type.some((component) => component === item.type) : item.type === type);
}

export { filterChildrenByType };
//# sourceMappingURL=filter-children-by-type.js.map
