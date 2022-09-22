'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var useDidUpdate = require('../use-did-update/use-did-update.js');

function useLogger(componentName, props) {
  react.useEffect(() => {
    console.log(`${componentName} mounted`, ...props);
    return () => console.log(`${componentName} unmounted`);
  }, []);
  useDidUpdate.useDidUpdate(() => {
    console.log(`${componentName} updated`, ...props);
  }, props);
  return null;
}

exports.useLogger = useLogger;
//# sourceMappingURL=use-logger.js.map
