"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
var common_1 = require("./common");
var scalar_1 = require("./scalar");
var element_1 = require("./element");
function decompressStages(cursor, data, orderedIndex) {
    for (; cursor.index < data.length; cursor.index++) {
        var c = data[cursor.index];
        if (c === constants_1.ARRAY_START_TOKEN) {
            cursor.currentTarget = { type: common_1.TargetType.ARRAY, value: [] };
            cursor.stack[++cursor.pointer] = cursor.currentTarget;
        }
        else if (c === constants_1.OBJECT_START_TOKEN) {
            cursor.currentTarget = { type: common_1.TargetType.OBJECT, value: {} };
            cursor.stack[++cursor.pointer] = cursor.currentTarget;
        }
        else if (c === constants_1.ARRAY_REPEAT_TOKEN && (cursor.currentTarget.type === common_1.TargetType.ARRAY || cursor.currentTarget.type === common_1.TargetType.TEMPLATE_OBJECT_ELEMENTS)) {
            var repeatedItem = cursor.currentTarget.value[cursor.currentTarget.value.length - 1];
            cursor.currentTarget.value.push(repeatedItem);
        }
        else if (c === constants_1.ARRAY_REPEAT_MANY_TOKEN && (cursor.currentTarget.type === common_1.TargetType.ARRAY || cursor.currentTarget.type === common_1.TargetType.TEMPLATE_OBJECT_ELEMENTS)) {
            var repeatCount = scalar_1.decompressScalar(data[cursor.index], data, cursor, orderedIndex);
            if (repeatCount === common_1.SKIP_SCALAR) {
                return;
            }
            var repeatedItem = cursor.currentTarget.value[cursor.currentTarget.value.length - 1];
            for (var i = 0; i < repeatCount; i++) {
                cursor.currentTarget.value.push(repeatedItem);
            }
        }
        else if (c === constants_1.TEMPLATE_OBJECT_START && (cursor.currentTarget.type === common_1.TargetType.TEMPLATE_OBJECT || cursor.currentTarget.type === common_1.TargetType.OBJECT || cursor.currentTarget.type === common_1.TargetType.ARRAY)) {
            if (cursor.currentTarget.type !== common_1.TargetType.TEMPLATE_OBJECT) {
                var parentTarget = cursor.currentTarget;
                cursor.currentTarget = { type: common_1.TargetType.TEMPLATE_OBJECT, value: void 0, currentTokens: [], currentRoute: [], paths: [], level: 0, parentTarget: parentTarget };
                cursor.stack[++cursor.pointer] = cursor.currentTarget;
            }
            else {
                // Add any found tokens prior to next nested as separate paths
                for (var i = 0; i < cursor.currentTarget.currentTokens.length - 1; i++) {
                    var currentToken = cursor.currentTarget.currentTokens[i];
                    cursor.currentTarget.paths[cursor.currentTarget.paths.length] = cursor.currentTarget.currentRoute.concat(currentToken);
                }
                // Add most recent token as part of next path
                if (cursor.currentTarget.currentToken != null) {
                    cursor.currentTarget.currentRoute.push(cursor.currentTarget.currentToken);
                }
                // Clear tokens for nested object
                cursor.currentTarget.currentTokens = [];
                cursor.currentTarget.level++;
            }
        }
        else if (c === constants_1.TEMPLATE_OBJECT_END && cursor.currentTarget.type === common_1.TargetType.TEMPLATE_OBJECT) {
            for (var i = 0; i < cursor.currentTarget.currentTokens.length; i++) {
                var currentToken = cursor.currentTarget.currentTokens[i];
                cursor.currentTarget.paths[cursor.currentTarget.paths.length] = cursor.currentTarget.currentRoute.concat(currentToken);
            }
            cursor.currentTarget.currentTokens = [];
            cursor.currentTarget.currentRoute = cursor.currentTarget.currentRoute.slice(0, -1);
            cursor.currentTarget.level--;
            if (cursor.currentTarget.level < 0) {
                var paths = cursor.currentTarget.paths;
                var parentTarget = cursor.currentTarget.parentTarget;
                cursor.pointer--;
                if (parentTarget.type === common_1.TargetType.ARRAY) {
                    cursor.currentTarget = { type: common_1.TargetType.TEMPLATE_OBJECT_ELEMENTS, value: parentTarget.value, paths: paths, currentPathIndex: 0, expectedPaths: paths.length, currentObject: {} };
                }
                else if (parentTarget.type === common_1.TargetType.OBJECT) {
                    cursor.currentTarget = { type: common_1.TargetType.TEMPLATE_OBJECT_PROPERTIES, value: parentTarget.value, paths: paths, currentPathIndex: -1, expectedPaths: paths.length, currentObject: {} };
                }
                cursor.stack[++cursor.pointer] = cursor.currentTarget;
            }
        }
        else if (c === constants_1.TEMPLATE_OBJECT_FINAL) {
            cursor.currentTarget = cursor.stack[--cursor.pointer];
        }
        else {
            if (!element_1.decompressElement(c, cursor, data, orderedIndex)) {
                return;
            }
        }
    }
}
exports.decompressStages = decompressStages;
//# sourceMappingURL=stages.js.map