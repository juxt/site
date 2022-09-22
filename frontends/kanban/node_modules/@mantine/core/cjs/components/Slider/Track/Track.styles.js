'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');
var SliderRoot_styles = require('../SliderRoot/SliderRoot.styles.js');

var useStyles = styles.createStyles((theme, { radius, size, color }) => ({
  track: {
    position: "relative",
    height: theme.fn.size({ sizes: SliderRoot_styles.sizes, size }),
    width: "100%",
    marginRight: theme.fn.size({ size, sizes: SliderRoot_styles.sizes }),
    marginLeft: theme.fn.size({ size, sizes: SliderRoot_styles.sizes }),
    "&::before": {
      content: '""',
      position: "absolute",
      top: 0,
      bottom: 0,
      borderRadius: theme.fn.size({ size: radius, sizes: theme.radius }),
      right: -theme.fn.size({ size, sizes: SliderRoot_styles.sizes }),
      left: -theme.fn.size({ size, sizes: SliderRoot_styles.sizes }),
      backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[4] : theme.colors.gray[2],
      zIndex: 0
    }
  },
  bar: {
    position: "absolute",
    zIndex: 1,
    top: 0,
    bottom: 0,
    backgroundColor: theme.fn.themeColor(color, 6),
    borderRadius: theme.fn.size({ size: radius, sizes: theme.radius })
  }
}));

exports.default = useStyles;
//# sourceMappingURL=Track.styles.js.map
