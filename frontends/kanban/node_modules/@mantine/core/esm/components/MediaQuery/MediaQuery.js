import React, { Children } from 'react';
import useStyles from './MediaQuery.styles.js';

function MediaQuery({
  children,
  smallerThan,
  largerThan,
  query,
  styles,
  className
}) {
  var _a;
  const { classes, cx } = useStyles({ smallerThan, largerThan, query, styles }, { name: "MediaQuery" });
  const child = Children.only(children);
  return React.cloneElement(child, {
    className: cx(classes.media, (_a = child.props) == null ? void 0 : _a.className, className)
  });
}
MediaQuery.displayName = "@mantine/core/MediaQuery";

export { MediaQuery };
//# sourceMappingURL=MediaQuery.js.map
