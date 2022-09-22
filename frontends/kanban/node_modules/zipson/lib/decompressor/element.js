"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
var common_1 = require("./common");
var scalar_1 = require("./scalar");
var template_1 = require("./template");
function decompressElement(c, cursor, data, orderedIndex) {
    var targetValue;
    if (c === constants_1.ARRAY_END_TOKEN || c === constants_1.OBJECT_END_TOKEN) {
        targetValue = cursor.currentTarget.value;
        cursor.currentTarget = cursor.stack[cursor.pointer - 1];
        cursor.pointer--;
    }
    else {
        targetValue = scalar_1.decompressScalar(c, data, cursor, orderedIndex);
        if (targetValue === common_1.SKIP_SCALAR) {
            return false;
        }
    }
    if (cursor.currentTarget.type === common_1.TargetType.SCALAR) {
        cursor.currentTarget.value = targetValue;
    }
    else if (cursor.currentTarget.type === common_1.TargetType.ARRAY) {
        cursor.currentTarget.value[cursor.currentTarget.value.length] = targetValue;
    }
    else if (cursor.currentTarget.type === common_1.TargetType.OBJECT) {
        if (cursor.currentTarget.key != null) {
            cursor.currentTarget.value[cursor.currentTarget.key] = targetValue;
            cursor.currentTarget.key = void 0;
        }
        else {
            cursor.currentTarget.key = targetValue;
        }
    }
    else if (cursor.currentTarget.type === common_1.TargetType.TEMPLATE_OBJECT) {
        cursor.currentTarget.currentToken = targetValue;
        cursor.currentTarget.currentTokens.push(targetValue);
    }
    else if (cursor.currentTarget.type === common_1.TargetType.TEMPLATE_OBJECT_PROPERTIES) {
        template_1.appendTemplateObjectPropertiesValue(cursor.currentTarget, targetValue);
    }
    else if (cursor.currentTarget.type === common_1.TargetType.TEMPLATE_OBJECT_ELEMENTS) {
        template_1.appendTemplateObjectElementsValue(cursor.currentTarget, targetValue);
    }
    return true;
}
exports.decompressElement = decompressElement;
//# sourceMappingURL=element.js.map