"use strict";

Object.defineProperty(exports, "__esModule", {
  value: !0
});

var _extends = require("@babel/runtime/helpers/extends"), React = require("react"), base_dist_reactSelect = require("../../dist/Select-0478e6f3.cjs.prod.js"), useAsync = require("../../dist/useAsync-5f1d80ed.cjs.prod.js"), useStateManager = require("../../dist/useStateManager-e7ac419f.cjs.prod.js"), useCreatable = require("../../dist/useCreatable-b5b17a51.cjs.prod.js");

function _interopDefault(e) {
  return e && e.__esModule ? e : {
    default: e
  };
}

function _interopNamespace(e) {
  if (e && e.__esModule) return e;
  var n = Object.create(null);
  return e && Object.keys(e).forEach((function(k) {
    if ("default" !== k) {
      var d = Object.getOwnPropertyDescriptor(e, k);
      Object.defineProperty(n, k, d.get ? d : {
        enumerable: !0,
        get: function() {
          return e[k];
        }
      });
    }
  })), n.default = e, Object.freeze(n);
}

require("../../dist/index-3df9f8fb.cjs.prod.js"), require("@emotion/react"), require("@babel/runtime/helpers/taggedTemplateLiteral"), 
require("@babel/runtime/helpers/objectWithoutProperties"), require("@babel/runtime/helpers/slicedToArray"), 
require("@babel/runtime/helpers/typeof"), require("@babel/runtime/helpers/classCallCheck"), 
require("@babel/runtime/helpers/createClass"), require("@babel/runtime/helpers/inherits"), 
require("@babel/runtime/helpers/defineProperty"), require("react-dom"), require("@babel/runtime/helpers/toConsumableArray"), 
require("memoize-one");

var _extends__default = _interopDefault(_extends), React__namespace = _interopNamespace(React), AsyncCreatableSelect = React.forwardRef((function(props, ref) {
  var stateManagerProps = useAsync.useAsync(props), creatableProps = useStateManager.useStateManager(stateManagerProps), selectProps = useCreatable.useCreatable(creatableProps);
  return React__namespace.createElement(base_dist_reactSelect.Select, _extends__default.default({
    ref: ref
  }, selectProps));
}));

exports.default = AsyncCreatableSelect;
