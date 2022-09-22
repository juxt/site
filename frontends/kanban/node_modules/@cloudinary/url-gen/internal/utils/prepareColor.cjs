'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

/**
 * Returns RGB or Color
 * @private
 * @param color
 */
function prepareColor(color) {
    if (color) {
        return color.match(/^#/) ? "rgb:".concat(color.substr(1)) : color;
    }
    else {
        return color;
    }
}

exports.prepareColor = prepareColor;
