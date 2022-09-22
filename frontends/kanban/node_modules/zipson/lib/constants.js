"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * Precision constants
 */
exports.FLOAT_COMPRESSION_PRECISION = 1000;
exports.DATE_LOW_PRECISION = 100000;
/**
 * Floating point delimiters
 */
exports.FLOAT_FULL_PRECISION_DELIMITER = ',';
exports.FLOAT_REDUCED_PRECISION_DELIMITER = '.';
/**
 * Data type tokens
 */
exports.INTEGER_TOKEN = '¢';
exports.FLOAT_TOKEN = '£';
exports.STRING_TOKEN = '¨';
exports.DATE_TOKEN = 'ø';
exports.LP_DATE_TOKEN = '±';
exports.UNREFERENCED_INTEGER_TOKEN = '¤';
exports.UNREFERENCED_FLOAT_TOKEN = '¥';
exports.UNREFERENCED_STRING_TOKEN = '´';
exports.UNREFERENCED_DATE_TOKEN = '¿';
exports.UNREFERENCED_LP_DATE_TOKEN = 'ÿ';
exports.REF_INTEGER_TOKEN = 'º';
exports.REF_FLOAT_TOKEN = 'Ý';
exports.REF_STRING_TOKEN = 'ß';
exports.REF_DATE_TOKEN = '×';
exports.REF_LP_DATE_TOKEN = 'ü';
exports.NULL_TOKEN = '§';
exports.UNDEFINED_TOKEN = 'µ';
exports.BOOLEAN_TRUE_TOKEN = '»';
exports.BOOLEAN_FALSE_TOKEN = '«';
/**
 * String escape tokens
 */
exports.ESCAPE_CHARACTER = '\\';
exports.ESCAPED_STRING_TOKEN = "" + exports.ESCAPE_CHARACTER + exports.STRING_TOKEN;
exports.ESCAPED_UNREFERENCED_STRING_TOKEN = "" + exports.ESCAPE_CHARACTER + exports.UNREFERENCED_STRING_TOKEN;
/**
 * Regex lookups
 */
exports.REGEX_ESCAPE_CHARACTER = new RegExp(exports.ESCAPE_CHARACTER.replace("\\", "\\\\"), 'g');
exports.REGEX_ESCAPED_ESCAPE_CHARACTER = new RegExp(exports.ESCAPE_CHARACTER.replace("\\", "\\\\") + exports.ESCAPE_CHARACTER.replace("\\", "\\\\"), 'g');
exports.REGEX_STRING_TOKEN = new RegExp(exports.STRING_TOKEN, 'g');
exports.REGEX_ESCAPED_STRING_TOKEN = new RegExp(exports.ESCAPE_CHARACTER + exports.ESCAPED_STRING_TOKEN, 'g');
exports.REGEX_UNREFERENCED_STRING_TOKEN = new RegExp(exports.UNREFERENCED_STRING_TOKEN, 'g');
exports.REGEX_UNREFERENCED_ESCAPED_STRING_TOKEN = new RegExp(exports.ESCAPE_CHARACTER + exports.ESCAPED_UNREFERENCED_STRING_TOKEN, 'g');
exports.DATE_REGEX = /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z/;
/**
 * Structural tokens
 */
exports.OBJECT_START_TOKEN = '{';
exports.OBJECT_END_TOKEN = '}';
exports.TEMPLATE_OBJECT_START = '¦';
exports.TEMPLATE_OBJECT_END = '‡';
exports.TEMPLATE_OBJECT_FINAL = '—';
exports.ARRAY_START_TOKEN = '|';
exports.ARRAY_END_TOKEN = '÷';
exports.ARRAY_REPEAT_TOKEN = 'þ';
exports.ARRAY_REPEAT_MANY_TOKEN = '^';
exports.ARRAY_REPEAT_COUNT_THRESHOLD = 4;
/**
 * General tokenization constants
 */
exports.REFERENCE_HEADER_LENGTH = 1;
exports.DELIMITING_TOKENS_THRESHOLD = 122;
exports.STRING_IDENT_PREFIX = '$';
/**
 * Small integer tokens
 */
exports.INTEGER_SMALL_EXCLUSIVE_BOUND_LOWER = -10;
exports.INTEGER_SMALL_EXCLUSIVE_BOUND_UPPER = 10;
exports.INTEGER_SMALL_TOKEN_EXCLUSIVE_BOUND_LOWER = 191;
exports.INTEGER_SMALL_TOKEN_EXCLUSIVE_BOUND_UPPER = 211;
exports.INTEGER_SMALL_TOKEN_OFFSET = -201;
exports.INTEGER_SMALL_TOKEN_ELEMENT_OFFSET = 9;
exports.INTEGER_SMALL_TOKENS = ['À', 'Á', 'Â', 'Ã', 'Ä', 'Å', 'Æ', 'Ç', 'È', 'É', 'Ê', 'Ë', 'Ì', 'Í', 'Î', 'Ï', 'Ð', 'Ñ', 'Ò'];
//# sourceMappingURL=constants.js.map