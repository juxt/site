/**
 * Convert number to base62 string
 */
export declare function compressInteger(number: number): string;
/**
 * Convert base62 string to number
 */
export declare function decompressInteger(compressedInteger: string): number;
/**
 * Convert float to base62 string for integer and fraction
 */
export declare function compressFloat(float: number, fullPrecision?: boolean): string;
/**
 * Convert base62 integer and fraction to float
 */
export declare function decompressFloat(compressedFloat: string): number;
