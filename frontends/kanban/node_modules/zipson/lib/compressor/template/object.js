"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../../constants");
var util_1 = require("../util");
var TemplateObject = /** @class */ (function () {
    /**
     * Create a new template object starting with two initial object that might have a shared structure
     */
    function TemplateObject(a, b) {
        this.isTemplating = false;
        this.struct = [];
        if (a != null && b != null) {
            this.isTemplating = buildTemplate(a, b, this.struct);
        }
    }
    /**
     * Compress template to writer
     */
    TemplateObject.prototype.compressTemplate = function (compressors, context, invertedIndex, writer, options) {
        compresObjectTemplate(compressors, context, invertedIndex, writer, options, this.struct);
    };
    /**
     * Compress object values according to structure to writer
     */
    TemplateObject.prototype.compressTemplateValues = function (compressors, context, invertedIndex, writer, options, obj) {
        compressObjectValues(compressors, context, invertedIndex, writer, options, this.struct, obj);
    };
    /**
     * Determine if object is templateable according to existing structure
     * If not the an ending token will be written to writer
     */
    TemplateObject.prototype.isNextTemplateable = function (obj, writer) {
        this.isTemplating = conformsToStructure(this.struct, obj);
        if (!this.isTemplating) {
            writer.write(constants_1.TEMPLATE_OBJECT_FINAL);
        }
    };
    /**
     * Finalize template object and write ending token
     */
    TemplateObject.prototype.end = function (writer) {
        writer.write(constants_1.TEMPLATE_OBJECT_FINAL);
    };
    return TemplateObject;
}());
exports.TemplateObject = TemplateObject;
/**
 * Build a shared template structure for two objects, returns true if they strictly share a structre
 * or false if not and a shared template structure could not be built
 */
function buildTemplate(a, b, struct, level) {
    if (level === void 0) { level = 0; }
    // Do not check deeper than 6 levels
    if (level > 6) {
        return false;
    }
    var keysA = Object.keys(a);
    var keysB = Object.keys(b);
    // If they do not have the same amount of keys, it is not a shared structure
    if (keysA.length !== keysB.length) {
        return false;
    }
    // Do not try to find a shared structure if there is more than 10 keys for one level
    if (keysA.length > 10) {
        return false;
    }
    // Sort keys to assert structural equality
    keysA.sort(function (a, b) { return a.localeCompare(b); });
    keysB.sort(function (a, b) { return a.localeCompare(b); });
    // Check each key for structural equality
    for (var i = 0; i < keysA.length; i++) {
        var keyA = keysA[i];
        var keyB = keysB[i];
        // If the keys do not share the same identifier, they are not structurally equal
        if (keyA !== keyB) {
            return false;
        }
        var valueA = a[keyA];
        var valueB = b[keyB];
        // Check if the key is an object
        if (util_1.isObject(valueA)) {
            if (!util_1.isObject(valueB)) {
                // If a is an object a b is not, they are not structurally equal
                return false;
            }
            // Create a substructure for nested object
            var nextStruct = [];
            // Add key and substructure to parent structure
            struct.push([keyA, nextStruct]);
            // Check nested objects for structural equality
            if (!buildTemplate(valueA, valueB, nextStruct, level + 1)) {
                return false;
            }
        }
        else if (util_1.isObject(valueB)) {
            // If a is not an object and b is, they are not structurally equal
            return false;
        }
        else {
            struct.push([keyA]);
        }
    }
    // If not on root level or root level is structurally equal objects they are considered structurally equal
    return level > 0 || util_1.isObject(a);
}
/**
 * Check if an object conforms to an existing structure
 */
function conformsToStructure(struct, obj) {
    if (!obj) {
        return false;
    }
    if (Object.keys(obj).length !== struct.length) {
        return false;
    }
    for (var i = 0; i < struct.length; i++) {
        var key = struct[i][0];
        var isNested = struct[i].length > 1;
        if (obj[key] === void 0) {
            return false;
        }
        if (isNested) {
            var x = struct[i];
            var y = x[1];
            if (!conformsToStructure(struct[i][1], obj[key])) {
                return false;
            }
        }
        else {
            if (util_1.isObject(obj[key])) {
                return false;
            }
        }
    }
    return true;
}
/**
 * Compress an object template to writer
 */
function compresObjectTemplate(compressors, context, invertedIndex, writer, options, struct) {
    writer.write(constants_1.TEMPLATE_OBJECT_START);
    for (var i = 0; i < struct.length; i++) {
        var key = struct[i][0];
        var isNested = struct[i].length > 1;
        compressors.string(compressors, context, key, invertedIndex, writer, options);
        if (isNested) {
            compresObjectTemplate(compressors, context, invertedIndex, writer, options, struct[i][1]);
        }
    }
    ;
    writer.write(constants_1.TEMPLATE_OBJECT_END);
}
/**
 * Compress object values according to provided structure to writer
 */
function compressObjectValues(compressors, context, invertedIndex, writer, options, struct, obj) {
    for (var i = 0; i < struct.length; i++) {
        var key = struct[i][0];
        var value = obj[key];
        var isNested = struct[i].length > 1;
        if (isNested) {
            compressObjectValues(compressors, context, invertedIndex, writer, options, struct[i][1], value);
        }
        else {
            compressors.any(compressors, context, value, invertedIndex, writer, options);
        }
    }
    ;
}
//# sourceMappingURL=object.js.map