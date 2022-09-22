"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
Object.defineProperty(exports, "MOUSE_ACTIVATION", {
  enumerable: true,
  get: function get() {
    return _constants.MOUSE_ACTIVATION;
  }
});
Object.defineProperty(exports, "TOUCH_ACTIVATION", {
  enumerable: true,
  get: function get() {
    return _constants.TOUCH_ACTIVATION;
  }
});
exports["default"] = exports.defaultState = void 0;

var _react = _interopRequireWildcard(require("react"));

var _propTypes = _interopRequireDefault(require("prop-types"));

var _mouseActivation = _interopRequireDefault(require("./mouseActivation"));

var _touchActivation = _interopRequireDefault(require("./touchActivation"));

var _constants = require("./constants");

var _utils = _interopRequireDefault(require("./utils"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } if (obj === null || _typeof(obj) !== "object" && typeof obj !== "function") { return { "default": obj }; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(Object(source), true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(Object(source)).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _createSuper(Derived) { var hasNativeReflectConstruct = _isNativeReflectConstruct(); return function _createSuperInternal() { var Super = _getPrototypeOf(Derived), result; if (hasNativeReflectConstruct) { var NewTarget = _getPrototypeOf(this).constructor; result = Reflect.construct(Super, arguments, NewTarget); } else { result = Super.apply(this, arguments); } return _possibleConstructorReturn(this, result); }; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _isNativeReflectConstruct() { if (typeof Reflect === "undefined" || !Reflect.construct) return false; if (Reflect.construct.sham) return false; if (typeof Proxy === "function") return true; try { Date.prototype.toString.call(Reflect.construct(Date, [], function () {})); return true; } catch (e) { return false; } }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var defaultState = {
  active: false,
  activePosition: {
    x: 0,
    y: 0
  },
  prevActivePosition: {
    x: 0,
    y: 0
  },
  passivePosition: {
    x: 0,
    y: 0
  },
  elementDimensions: {
    width: 0,
    height: 0
  },
  elementOffset: {
    left: 0,
    top: 0
  },
  itemPosition: {
    x: 0,
    y: 0
  },
  itemDimensions: {
    width: 0,
    height: 0
  }
};
exports.defaultState = defaultState;

var ReactInputPosition = /*#__PURE__*/function (_Component) {
  _inherits(ReactInputPosition, _Component);

  var _super = _createSuper(ReactInputPosition);

  function ReactInputPosition() {
    var _this;

    _classCallCheck(this, ReactInputPosition);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _super.call.apply(_super, [this].concat(args));

    _defineProperty(_assertThisInitialized(_this), "state", defaultState);

    _defineProperty(_assertThisInitialized(_this), "containerRef", /*#__PURE__*/_react["default"].createRef());

    _defineProperty(_assertThisInitialized(_this), "itemRef", /*#__PURE__*/_react["default"].createRef());

    _defineProperty(_assertThisInitialized(_this), "mouseDown", false);

    _defineProperty(_assertThisInitialized(_this), "touched", false);

    _defineProperty(_assertThisInitialized(_this), "justTouched", false);

    _defineProperty(_assertThisInitialized(_this), "tapped", false);

    _defineProperty(_assertThisInitialized(_this), "tapTimer", null);

    _defineProperty(_assertThisInitialized(_this), "tapTimedOut", false);

    _defineProperty(_assertThisInitialized(_this), "doubleTapTimer", null);

    _defineProperty(_assertThisInitialized(_this), "doubleTapTimedOut", false);

    _defineProperty(_assertThisInitialized(_this), "longTouchTimer", null);

    _defineProperty(_assertThisInitialized(_this), "longTouchTimedOut", false);

    _defineProperty(_assertThisInitialized(_this), "refresh", true);

    _defineProperty(_assertThisInitialized(_this), "onLoadRefresh", function () {
      _this.refreshPosition();
    });

    _defineProperty(_assertThisInitialized(_this), "handleResize", function () {
      _this.refreshPosition();
    });

    return _this;
  }

  _createClass(ReactInputPosition, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      this.init();
      this.refreshPosition();
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      this.removeMouseEventListeners();
      this.removeTouchEventListeners();
      this.removeOtherEventListeners();
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate(prevProps) {
      if (prevProps.mouseActivationMethod !== this.props.mouseActivationMethod) {
        this.removeMouseEventListeners();
        this.setMouseInteractionMethods();
        this.addMouseEventListeners();
      }

      if (prevProps.touchActivationMethod !== this.props.touchActivationMethod) {
        this.removeTouchEventListeners();
        this.setTouchInteractionMethods();
        this.addTouchEventListeners();
      }
    }
  }, {
    key: "init",
    value: function init() {
      this.checkPassiveEventSupport();
      this.setInputInteractionMethods();
      this.addMouseEventListeners();
      this.addTouchEventListeners();
      this.addOtherEventListeners();
    }
  }, {
    key: "checkPassiveEventSupport",
    value: function checkPassiveEventSupport() {
      var _this2 = this;

      this.supportsPassive = false;

      try {
        var options = Object.defineProperty({}, "passive", {
          get: function get() {
            return _this2.supportsPassive = true;
          }
        });
        window.addEventListener("testPassive", null, options);
        window.removeEventListener("testPassive", null, options);
      } catch (e) {}
    }
  }, {
    key: "updateState",
    value: function updateState(changes, cb) {
      var _this3 = this;

      var onUpdate = this.props.onUpdate;
      var activationCallback;

      if (changes.hasOwnProperty("active")) {
        if (changes.active) {
          activationCallback = this.props.onActivate;
        } else {
          activationCallback = this.props.onDeactivate;
        }
      }

      if (this.props.overrideState) {
        onUpdate && onUpdate(changes);
        activationCallback && activationCallback();
        cb && cb.call(this);
        return;
      }

      this.setState(function () {
        return changes;
      }, function () {
        cb && cb.call(_this3);
        activationCallback && activationCallback();
        onUpdate && onUpdate(_this3.getStateClone());
      });
    }
  }, {
    key: "getState",
    value: function getState() {
      if (this.props.overrideState) {
        return this.props.overrideState;
      } else {
        return this.state;
      }
    }
  }, {
    key: "getStateClone",
    value: function getStateClone() {
      var state = this.getState();
      var clonedState = {};

      for (var key in state) {
        if (_typeof(state[key]) === "object") {
          clonedState[key] = _objectSpread({}, state[key]);
        } else {
          clonedState[key] = state[key];
        }
      }

      return clonedState;
    }
  }, {
    key: "refreshPosition",
    value: function refreshPosition() {
      var _this$props = this.props,
          trackItemPosition = _this$props.trackItemPosition,
          centerItemOnLoad = _this$props.centerItemOnLoad;
      this.setPosition({
        x: 0,
        y: 0
      }, trackItemPosition, false, centerItemOnLoad);
    }
  }, {
    key: "setInputInteractionMethods",
    value: function setInputInteractionMethods() {
      this.setMouseInteractionMethods();
      this.setTouchInteractionMethods();
    }
  }, {
    key: "setMouseInteractionMethods",
    value: function setMouseInteractionMethods() {
      var mouseInteractionMethods = _mouseActivation["default"][this.props.mouseActivationMethod];
      this.mouseHandlers = [];

      for (var key in mouseInteractionMethods) {
        this.mouseHandlers.push({
          event: key.toLowerCase(),
          handler: mouseInteractionMethods[key].bind(this)
        });
      }
    }
  }, {
    key: "setTouchInteractionMethods",
    value: function setTouchInteractionMethods() {
      var touchInteractionMethods = _touchActivation["default"][this.props.touchActivationMethod];
      this.touchHandlers = [];

      for (var key in touchInteractionMethods) {
        this.touchHandlers.push({
          event: key.toLowerCase(),
          handler: touchInteractionMethods[key].bind(this)
        });
      }
    }
  }, {
    key: "setPosition",
    value: function setPosition(position, updateItemPosition, activate, centerItem) {
      if (this.props.minUpdateSpeedInMs && !this.refresh) return;
      this.refresh = false;

      var _this$containerRef$cu = this.containerRef.current.getBoundingClientRect(),
          left = _this$containerRef$cu.left,
          top = _this$containerRef$cu.top,
          width = _this$containerRef$cu.width,
          height = _this$containerRef$cu.height;

      var _this$props2 = this.props,
          trackItemPosition = _this$props2.trackItemPosition,
          trackPassivePosition = _this$props2.trackPassivePosition,
          trackPreviousPosition = _this$props2.trackPreviousPosition,
          centerItemOnActivate = _this$props2.centerItemOnActivate,
          centerItemOnActivatePos = _this$props2.centerItemOnActivatePos,
          linkItemToActive = _this$props2.linkItemToActive,
          itemMovementMultiplier = _this$props2.itemMovementMultiplier,
          alignItemOnActivePos = _this$props2.alignItemOnActivePos,
          itemPositionMinX = _this$props2.itemPositionMinX,
          itemPositionMaxX = _this$props2.itemPositionMaxX,
          itemPositionMinY = _this$props2.itemPositionMinY,
          itemPositionMaxY = _this$props2.itemPositionMaxY,
          itemPositionLimitBySize = _this$props2.itemPositionLimitBySize,
          itemPositionLimitInternal = _this$props2.itemPositionLimitInternal;

      var _this$getState = this.getState(),
          activePosition = _this$getState.activePosition,
          itemPosition = _this$getState.itemPosition; // Set container div info and active position


      var stateUpdate = {
        elementDimensions: {
          width: width,
          height: height
        },
        elementOffset: {
          left: left,
          top: top
        },
        activePosition: {
          x: Math.min(Math.max(0, position.x - left), width),
          y: Math.min(Math.max(0, position.y - top), height)
        }
      }; // Activate if necessary

      if (activate) stateUpdate.active = true; // Set item dimensions

      if (this.itemRef.current) {
        var itemSize = this.itemRef.current.getBoundingClientRect();
        stateUpdate.itemDimensions = {
          width: itemSize.width,
          height: itemSize.height
        };
      } // Set previous active position


      if (trackPreviousPosition || trackItemPosition) {
        stateUpdate.prevActivePosition = {
          x: activePosition.x,
          y: activePosition.y
        };
      } // Set passive position


      if (trackPassivePosition) {
        stateUpdate.passivePosition = {
          x: position.x - left,
          y: position.y - top
        };
      } // Create adjusted limits


      var limits = _utils["default"].createAdjustedLimits(itemPositionMinX, itemPositionMaxX, itemPositionMinY, itemPositionMaxY, stateUpdate.elementDimensions, stateUpdate.itemDimensions, itemPositionLimitBySize, itemPositionLimitInternal); // Center item


      if (centerItem || activate && centerItemOnActivate) {
        var centerX = (limits.maxX + limits.minX) / 2;
        var centerY = (limits.maxY + limits.minY) / 2;
        stateUpdate.itemPosition = {
          x: centerX || 0,
          y: centerY || 0
        };
        return this.updateState(stateUpdate, this.startRefreshTimer);
      }

      var shouldLimitItem = true; // Set item position

      if (trackItemPosition && linkItemToActive) {
        stateUpdate.itemPosition = _objectSpread({}, stateUpdate.activePosition);
      } else if (trackItemPosition && alignItemOnActivePos) {
        stateUpdate.itemPosition = _utils["default"].alignItemOnPosition(stateUpdate.elementDimensions, stateUpdate.itemDimensions, stateUpdate.activePosition);
      } else if (trackItemPosition && activate && centerItemOnActivatePos) {
        stateUpdate.itemPosition = _utils["default"].centerItemOnPosition(stateUpdate.elementDimensions, stateUpdate.itemDimensions, stateUpdate.activePosition);
      } else if (trackItemPosition && updateItemPosition) {
        stateUpdate.itemPosition = _utils["default"].calculateItemPosition(itemPosition, stateUpdate.prevActivePosition, stateUpdate.activePosition, itemMovementMultiplier);
      } else {
        shouldLimitItem = false;
      } // Apply position limits


      if (shouldLimitItem) {
        stateUpdate.itemPosition = _utils["default"].limitPosition(limits.minX, limits.maxX, limits.minY, limits.maxY, stateUpdate.itemPosition);
      }

      this.updateState(stateUpdate, this.startRefreshTimer);
    }
  }, {
    key: "setPassivePosition",
    value: function setPassivePosition(position) {
      if (!this.props.trackPassivePosition) return;

      var _this$containerRef$cu2 = this.containerRef.current.getBoundingClientRect(),
          left = _this$containerRef$cu2.left,
          top = _this$containerRef$cu2.top;

      this.updateState({
        passivePosition: {
          x: position.x - left,
          y: position.y - top
        }
      });
    }
  }, {
    key: "toggleActive",
    value: function toggleActive() {
      var position = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {
        x: 0,
        y: 0
      };

      if (!this.getState().active) {
        this.activate(position);
      } else {
        this.deactivate();
      }
    }
  }, {
    key: "activate",
    value: function activate() {
      var position = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {
        x: 0,
        y: 0
      };
      this.setPosition(position, false, true);
    }
  }, {
    key: "deactivate",
    value: function deactivate() {
      this.updateState({
        active: false
      });
    }
  }, {
    key: "startRefreshTimer",
    value: function startRefreshTimer() {
      var _this4 = this;

      if (!this.props.minUpdateSpeedInMs) return;
      setTimeout(function () {
        _this4.refresh = true;
      }, this.props.minUpdateSpeedInMs);
    }
  }, {
    key: "startTapTimer",
    value: function startTapTimer() {
      var _this5 = this;

      this.tapTimer = setTimeout(function () {
        _this5.tapTimedOut = true;
      }, this.props.tapDurationInMs);
    }
  }, {
    key: "startDoubleTapTimer",
    value: function startDoubleTapTimer() {
      var _this6 = this;

      this.doubleTapTimer = setTimeout(function () {
        _this6.doubleTapTimedOut = true;
      }, this.props.doubleTapDurationInMs);
    }
  }, {
    key: "startLongTouchTimer",
    value: function startLongTouchTimer(e) {
      var _this7 = this;

      this.longTouchTimer = setTimeout(function () {
        if (_this7.touched) {
          _this7.toggleActive(e);
        }
      }, this.props.longTouchDurationInMs);
    }
  }, {
    key: "addMouseEventListeners",
    value: function addMouseEventListeners() {
      var _this8 = this;

      this.mouseHandlers.forEach(function (mouse) {
        _this8.containerRef.current.addEventListener(mouse.event, mouse.handler);
      });
    }
  }, {
    key: "addTouchEventListeners",
    value: function addTouchEventListeners() {
      var _this9 = this;

      this.touchHandlers.forEach(function (touch) {
        _this9.containerRef.current.addEventListener(touch.event, touch.handler, _this9.supportsPassive ? {
          passive: false
        } : false);
      });
    }
  }, {
    key: "removeMouseEventListeners",
    value: function removeMouseEventListeners() {
      var _this10 = this;

      this.mouseHandlers.forEach(function (mouse) {
        _this10.containerRef.current.removeEventListener(mouse.event, mouse.handler);
      });
    }
  }, {
    key: "removeTouchEventListeners",
    value: function removeTouchEventListeners() {
      var _this11 = this;

      this.touchHandlers.forEach(function (touch) {
        _this11.containerRef.current.removeEventListener(touch.event, touch.handler, _this11.supportsPassive ? {
          passive: false
        } : false);
      });
    }
  }, {
    key: "addOtherEventListeners",
    value: function addOtherEventListeners() {
      window.addEventListener("resize", this.handleResize);
      window.addEventListener("load", this.onLoadRefresh);
    }
  }, {
    key: "removeOtherEventListeners",
    value: function removeOtherEventListeners() {
      window.removeEventListener("resize", this.handleResize);
      window.removeEventListener("load", this.onLoadRefresh);
    }
  }, {
    key: "render",
    value: function render() {
      var _this$props3 = this.props,
          style = _this$props3.style,
          className = _this$props3.className,
          children = _this$props3.children,
          cursorStyle = _this$props3.cursorStyle,
          cursorStyleActive = _this$props3.cursorStyleActive;

      var _this$getState2 = this.getState(),
          active = _this$getState2.active;

      var combinedStyle = _objectSpread(_objectSpread({}, style), {}, {
        WebkitUserSelect: "none",
        MozUserSelect: "none",
        msUserSelect: "none",
        userSelect: "none",
        cursor: active ? cursorStyleActive || cursorStyle : cursorStyle
      });

      return /*#__PURE__*/_react["default"].createElement("div", {
        style: combinedStyle,
        className: className,
        ref: this.containerRef
      }, _utils["default"].decorateChildren(children, _objectSpread(_objectSpread({}, this.getState()), {}, {
        itemRef: this.itemRef,
        onLoadRefresh: this.onLoadRefresh
      })));
    }
  }]);

  return ReactInputPosition;
}(_react.Component);

_defineProperty(ReactInputPosition, "propTypes", {
  mouseActivationMethod: _propTypes["default"].oneOf([_constants.MOUSE_ACTIVATION.CLICK, _constants.MOUSE_ACTIVATION.DOUBLE_CLICK, _constants.MOUSE_ACTIVATION.HOVER, _constants.MOUSE_ACTIVATION.MOUSE_DOWN]).isRequired,
  touchActivationMethod: _propTypes["default"].oneOf([_constants.TOUCH_ACTIVATION.DOUBLE_TAP, _constants.TOUCH_ACTIVATION.LONG_TOUCH, _constants.TOUCH_ACTIVATION.TAP, _constants.TOUCH_ACTIVATION.TOUCH]).isRequired,
  tapDurationInMs: _propTypes["default"].number,
  doubleTapDurationInMs: _propTypes["default"].number,
  longTouchDurationInMs: _propTypes["default"].number,
  longTouchMoveLimit: _propTypes["default"].number,
  clickMoveLimit: _propTypes["default"].number,
  itemPositionMinX: _propTypes["default"].number,
  itemPositionMaxX: _propTypes["default"].number,
  itemPositionMinY: _propTypes["default"].number,
  itemPositionMaxY: _propTypes["default"].number,
  itemPositionLimitBySize: _propTypes["default"].bool,
  itemPositionLimitInternal: _propTypes["default"].bool,
  linkItemToActive: _propTypes["default"].bool,
  className: _propTypes["default"].string,
  style: _propTypes["default"].object,
  minUpdateSpeedInMs: _propTypes["default"].number,
  trackPassivePosition: _propTypes["default"].bool,
  trackItemPosition: _propTypes["default"].bool,
  trackPreviousPosition: _propTypes["default"].bool,
  centerItemOnActivate: _propTypes["default"].bool,
  centerItemOnActivatePos: _propTypes["default"].bool,
  centerItemOnLoad: _propTypes["default"].bool,
  alignItemOnActivePos: _propTypes["default"].bool,
  itemMovementMultiplier: _propTypes["default"].number,
  cursorStyle: _propTypes["default"].string,
  cursorStyleActive: _propTypes["default"].string,
  onUpdate: _propTypes["default"].func,
  overrideState: _propTypes["default"].object,
  mouseDownAllowOutside: _propTypes["default"].bool,
  onActivate: _propTypes["default"].func,
  onDeactivate: _propTypes["default"].func
});

_defineProperty(ReactInputPosition, "defaultProps", {
  tapDurationInMs: 180,
  doubleTapDurationInMs: 400,
  longTouchDurationInMs: 500,
  longTouchMoveLimit: 5,
  clickMoveLimit: 5,
  style: {},
  minUpdateSpeedInMs: 1,
  itemMovementMultiplier: 1,
  cursorStyle: "crosshair",
  mouseActivationMethod: _constants.MOUSE_ACTIVATION.CLICK,
  touchActivationMethod: _constants.TOUCH_ACTIVATION.TAP,
  mouseDownAllowOutside: false
});

var _default = ReactInputPosition;
exports["default"] = _default;