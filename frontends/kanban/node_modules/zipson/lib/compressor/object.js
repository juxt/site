"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
/**
 * Compress object to writer
 */
function compressObject(compressors, context, obj, invertedIndex, writer, options) {
    writer.write(constants_1.OBJECT_START_TOKEN);
    var keys = Object.keys(obj);
    // Create a template object for first two keys in object
    var templateObject = new compressors.template.Object(obj[keys[0]], obj[keys[1]]);
    // Compress template is templating
    if (templateObject.isTemplating) {
        templateObject.compressTemplate(compressors, context, invertedIndex, writer, options);
    }
    for (var i = 0; i < keys.length; i++) {
        // Determine if still templating after the two first keys
        if (i > 1 && templateObject.isTemplating) {
            templateObject.isNextTemplateable(obj[keys[i]], writer);
        }
        if (templateObject.isTemplating) {
            // Compress id and template values if templating
            compressors.string(compressors, context, keys[i], invertedIndex, writer, options);
            templateObject.compressTemplateValues(compressors, context, invertedIndex, writer, options, obj[keys[i]]);
        }
        else {
            // Compress object key and value if not templating
            var key = keys[i];
            var val = obj[key];
            if (val !== undefined) {
                compressors.string(compressors, context, key, invertedIndex, writer, options);
                compressors.any(compressors, context, val, invertedIndex, writer, options);
            }
        }
    }
    ;
    // Finalize template object if still templating
    if (templateObject.isTemplating) {
        templateObject.end(writer);
    }
    writer.write(constants_1.OBJECT_END_TOKEN);
}
exports.compressObject = compressObject;
//# sourceMappingURL=object.js.map