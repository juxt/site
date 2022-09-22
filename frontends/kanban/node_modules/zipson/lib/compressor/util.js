"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * Determine if obj is an object according to serialization
 */
function isObject(obj) {
    var type = typeof obj;
    if (type === 'number') {
        return false;
    }
    else if (type === 'string') {
        return false;
    }
    else if (type === 'boolean') {
        return false;
    }
    else if (obj === null) {
        return false;
    }
    else if (Array.isArray(obj)) {
        return false;
    }
    else if (obj instanceof Date) {
        return false;
    }
    else if (obj === void 0) {
        return false;
    }
    else {
        return true;
    }
}
exports.isObject = isObject;
//# sourceMappingURL=util.js.map