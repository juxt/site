"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var constants_1 = require("../constants");
var writer_1 = require("./writer");
var util_1 = require("../util");
/**
 * Compress array to writer
 */
function compressArray(compressors, context, array, invertedIndex, writer, options) {
    // Increase context array level and create a new element writer if needed
    context.arrayLevel++;
    if (context.arrayLevel > context.arrayItemWriters.length) {
        context.arrayItemWriters.push(new writer_1.ZipsonStringWriter());
    }
    // Get the element and parent writer
    var arrayItemWriter = context.arrayItemWriters[context.arrayLevel - 1];
    var parentWriter = context.arrayItemWriters[context.arrayLevel - 2] || writer;
    parentWriter.write(constants_1.ARRAY_START_TOKEN);
    var previousItem = '';
    var repeatedTimes = 0;
    var repeatManyCount = 0;
    // Create a template object for first two keys in object
    var templateObject = new compressors.template.Object(array[0], array[1]);
    // Compress template is templating
    if (templateObject.isTemplating) {
        templateObject.compressTemplate(compressors, context, invertedIndex, parentWriter, options);
    }
    for (var i = 0; i < array.length; i++) {
        var item = array[i];
        arrayItemWriter.value = '';
        // Make undefined elements into null values
        if (item === undefined) {
            item = null;
        }
        // Determine if still templating after the two first elements
        if (i > 1 && templateObject.isTemplating) {
            templateObject.isNextTemplateable(array[i], parentWriter);
        }
        if (templateObject.isTemplating) {
            // Compress template values if templating
            templateObject.compressTemplateValues(compressors, context, invertedIndex, arrayItemWriter, options, array[i]);
        }
        else {
            // Compress any element otherwise
            compressors.any(compressors, context, item, invertedIndex, arrayItemWriter, options);
        }
        // Check if we wrote an identical elements
        if (arrayItemWriter.value === previousItem) {
            // Count repetitions and see if we repeated enough to use a many token
            repeatedTimes++;
            if (repeatedTimes >= constants_1.ARRAY_REPEAT_COUNT_THRESHOLD) {
                // Write a many token if needed and count how many "many"-times we repeated
                if (repeatManyCount === 0) {
                    parentWriter.write(constants_1.ARRAY_REPEAT_MANY_TOKEN);
                }
                repeatManyCount++;
            }
            else {
                // Default to standard repeat token
                parentWriter.write(constants_1.ARRAY_REPEAT_TOKEN);
            }
        }
        else {
            repeatedTimes = 0;
            if (repeatManyCount > 0) {
                // If we repeated many times, write the count before the next element
                parentWriter.write(util_1.compressInteger(repeatManyCount));
                repeatManyCount = 0;
            }
            parentWriter.write(arrayItemWriter.value);
            previousItem = arrayItemWriter.value;
        }
    }
    // If still repeating may, write the final repeat count
    if (repeatManyCount > 0) {
        parentWriter.write(util_1.compressInteger(repeatManyCount));
    }
    // Finalize template object if still templating
    if (templateObject.isTemplating) {
        templateObject.end(parentWriter);
    }
    parentWriter.write(constants_1.ARRAY_END_TOKEN);
    context.arrayLevel--;
}
exports.compressArray = compressArray;
//# sourceMappingURL=array.js.map