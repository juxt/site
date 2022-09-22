"use strict";

Object.defineProperty(exports, "__esModule", {
  value: !0
});

var index$1 = require("../../dist/index-3df9f8fb.cjs.prod.js"), _objectWithoutProperties = require("@babel/runtime/helpers/objectWithoutProperties"), memoizeOne = require("memoize-one"), React = require("react"), _extends = require("@babel/runtime/helpers/extends"), _classCallCheck = require("@babel/runtime/helpers/classCallCheck"), _createClass = require("@babel/runtime/helpers/createClass"), _inherits = require("@babel/runtime/helpers/inherits"), reactTransitionGroup = require("react-transition-group"), _slicedToArray = require("@babel/runtime/helpers/slicedToArray");

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

require("@emotion/react"), require("@babel/runtime/helpers/taggedTemplateLiteral"), 
require("@babel/runtime/helpers/typeof"), require("@babel/runtime/helpers/defineProperty"), 
require("react-dom");

var _objectWithoutProperties__default = _interopDefault(_objectWithoutProperties), memoizeOne__default = _interopDefault(memoizeOne), React__namespace = _interopNamespace(React), _extends__default = _interopDefault(_extends), _classCallCheck__default = _interopDefault(_classCallCheck), _createClass__default = _interopDefault(_createClass), _inherits__default = _interopDefault(_inherits), _slicedToArray__default = _interopDefault(_slicedToArray), _excluded$4 = [ "in", "onExited", "appear", "enter", "exit" ], AnimatedInput = function(WrappedComponent) {
  return function(_ref) {
    _ref.in, _ref.onExited, _ref.appear, _ref.enter, _ref.exit;
    var props = _objectWithoutProperties__default.default(_ref, _excluded$4);
    return React__namespace.createElement(WrappedComponent, props);
  };
}, _excluded$3 = [ "component", "duration", "in", "onExited" ], Fade = function(_ref) {
  var Tag = _ref.component, _ref$duration = _ref.duration, duration = void 0 === _ref$duration ? 1 : _ref$duration, inProp = _ref.in;
  _ref.onExited;
  var props = _objectWithoutProperties__default.default(_ref, _excluded$3), nodeRef = React.useRef(null), transition = {
    entering: {
      opacity: 0
    },
    entered: {
      opacity: 1,
      transition: "opacity ".concat(duration, "ms")
    },
    exiting: {
      opacity: 0
    },
    exited: {
      opacity: 0
    }
  };
  return React__namespace.createElement(reactTransitionGroup.Transition, {
    mountOnEnter: !0,
    unmountOnExit: !0,
    in: inProp,
    timeout: duration,
    nodeRef: nodeRef
  }, (function(state) {
    var innerProps = {
      style: index$1._objectSpread2({}, transition[state]),
      ref: nodeRef
    };
    return React__namespace.createElement(Tag, _extends__default.default({
      innerProps: innerProps
    }, props));
  }));
}, collapseDuration = 260, Collapse = function(_Component) {
  _inherits__default.default(Collapse, _Component);
  var _super = index$1._createSuper(Collapse);
  function Collapse() {
    var _this;
    _classCallCheck__default.default(this, Collapse);
    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) args[_key] = arguments[_key];
    return (_this = _super.call.apply(_super, [ this ].concat(args))).duration = collapseDuration, 
    _this.rafID = void 0, _this.state = {
      width: "auto"
    }, _this.transition = {
      exiting: {
        width: 0,
        transition: "width ".concat(_this.duration, "ms ease-out")
      },
      exited: {
        width: 0
      }
    }, _this.nodeRef = React.createRef(), _this.getStyle = function(width) {
      return {
        overflow: "hidden",
        whiteSpace: "nowrap",
        width: width
      };
    }, _this.getTransition = function(state) {
      return _this.transition[state];
    }, _this;
  }
  return _createClass__default.default(Collapse, [ {
    key: "componentDidMount",
    value: function() {
      var _this2 = this, ref = this.nodeRef.current;
      ref && (this.rafID = window.requestAnimationFrame((function() {
        var width = ref.getBoundingClientRect().width;
        _this2.setState({
          width: width
        });
      })));
    }
  }, {
    key: "componentWillUnmount",
    value: function() {
      this.rafID && window.cancelAnimationFrame(this.rafID);
    }
  }, {
    key: "render",
    value: function() {
      var _this3 = this, _this$props = this.props, children = _this$props.children, inProp = _this$props.in, onExited = _this$props.onExited, width = this.state.width;
      return React__namespace.createElement(reactTransitionGroup.Transition, {
        enter: !1,
        mountOnEnter: !0,
        unmountOnExit: !0,
        in: inProp,
        onExited: function() {
          _this3.nodeRef.current && onExited && onExited(_this3.nodeRef.current);
        },
        timeout: this.duration,
        nodeRef: this.nodeRef
      }, (function(state) {
        var style = index$1._objectSpread2(index$1._objectSpread2({}, _this3.getStyle(width)), _this3.getTransition(state));
        return React__namespace.createElement("div", {
          ref: _this3.nodeRef,
          style: style
        }, children);
      }));
    }
  } ]), Collapse;
}(React.Component), _excluded$2 = [ "in", "onExited" ], AnimatedMultiValue = function(WrappedComponent) {
  return function(_ref) {
    var inProp = _ref.in, onExited = _ref.onExited, props = _objectWithoutProperties__default.default(_ref, _excluded$2);
    return React__namespace.createElement(Collapse, {
      in: inProp,
      onExited: onExited
    }, React__namespace.createElement(WrappedComponent, _extends__default.default({
      cropWithEllipsis: inProp
    }, props)));
  };
}, AnimatedPlaceholder = function(WrappedComponent) {
  return function(props) {
    return React__namespace.createElement(Fade, _extends__default.default({
      component: WrappedComponent,
      duration: props.isMulti ? collapseDuration : 1
    }, props));
  };
}, AnimatedSingleValue = function(WrappedComponent) {
  return function(props) {
    return React__namespace.createElement(Fade, _extends__default.default({
      component: WrappedComponent
    }, props));
  };
}, _excluded$1 = [ "component" ], _excluded2 = [ "children" ], AnimatedValueContainer = function(WrappedComponent) {
  return function(props) {
    return props.isMulti ? React__namespace.createElement(IsMultiValueContainer, _extends__default.default({
      component: WrappedComponent
    }, props)) : React__namespace.createElement(reactTransitionGroup.TransitionGroup, _extends__default.default({
      component: WrappedComponent
    }, props));
  };
}, IsMultiValueContainer = function(_ref) {
  var component = _ref.component, restProps = _objectWithoutProperties__default.default(_ref, _excluded$1), multiProps = useIsMultiValueContainer(restProps);
  return React__namespace.createElement(reactTransitionGroup.TransitionGroup, _extends__default.default({
    component: component
  }, multiProps));
}, useIsMultiValueContainer = function(_ref2) {
  var children = _ref2.children, props = _objectWithoutProperties__default.default(_ref2, _excluded2), isMulti = props.isMulti, hasValue = props.hasValue, innerProps = props.innerProps, _props$selectProps = props.selectProps, components = _props$selectProps.components, controlShouldRenderValue = _props$selectProps.controlShouldRenderValue, _useState = React.useState(isMulti && controlShouldRenderValue && hasValue), _useState2 = _slicedToArray__default.default(_useState, 2), cssDisplayFlex = _useState2[0], setCssDisplayFlex = _useState2[1], _useState3 = React.useState(!1), _useState4 = _slicedToArray__default.default(_useState3, 2), removingValue = _useState4[0], setRemovingValue = _useState4[1];
  React.useEffect((function() {
    hasValue && !cssDisplayFlex && setCssDisplayFlex(!0);
  }), [ hasValue, cssDisplayFlex ]), React.useEffect((function() {
    removingValue && !hasValue && cssDisplayFlex && setCssDisplayFlex(!1), setRemovingValue(!1);
  }), [ removingValue, hasValue, cssDisplayFlex ]);
  var onExited = function() {
    return setRemovingValue(!0);
  }, newInnerProps = index$1._objectSpread2(index$1._objectSpread2({}, innerProps), {}, {
    style: index$1._objectSpread2(index$1._objectSpread2({}, null == innerProps ? void 0 : innerProps.style), {}, {
      display: cssDisplayFlex ? "flex" : "grid"
    })
  });
  return index$1._objectSpread2(index$1._objectSpread2({}, props), {}, {
    innerProps: newInnerProps,
    children: React__namespace.Children.toArray(children).map((function(child) {
      if (isMulti && React__namespace.isValidElement(child)) {
        if (child.type === components.MultiValue) return React__namespace.cloneElement(child, {
          onExited: onExited
        });
        if (child.type === components.Placeholder && cssDisplayFlex) return null;
      }
      return child;
    }))
  });
}, _excluded = [ "Input", "MultiValue", "Placeholder", "SingleValue", "ValueContainer" ], makeAnimated = function() {
  var externalComponents = arguments.length > 0 && void 0 !== arguments[0] ? arguments[0] : {}, components = index$1.defaultComponents({
    components: externalComponents
  }), Input = components.Input, MultiValue = components.MultiValue, Placeholder = components.Placeholder, SingleValue = components.SingleValue, ValueContainer = components.ValueContainer, rest = _objectWithoutProperties__default.default(components, _excluded);
  return index$1._objectSpread2({
    Input: AnimatedInput(Input),
    MultiValue: AnimatedMultiValue(MultiValue),
    Placeholder: AnimatedPlaceholder(Placeholder),
    SingleValue: AnimatedSingleValue(SingleValue),
    ValueContainer: AnimatedValueContainer(ValueContainer)
  }, rest);
}, AnimatedComponents = makeAnimated(), Input = AnimatedComponents.Input, MultiValue = AnimatedComponents.MultiValue, Placeholder = AnimatedComponents.Placeholder, SingleValue = AnimatedComponents.SingleValue, ValueContainer = AnimatedComponents.ValueContainer, index = memoizeOne__default.default(makeAnimated);

exports.Input = Input, exports.MultiValue = MultiValue, exports.Placeholder = Placeholder, 
exports.SingleValue = SingleValue, exports.ValueContainer = ValueContainer, exports.default = index;
