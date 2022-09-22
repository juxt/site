import { Cursor, OrderedIndex } from "./decompressor/common";
/**
 * Create an ordered index for decompression
 */
export declare function makeOrderedIndex(): OrderedIndex;
/**
 * Decompress data string with provided ordered index
 */
export declare function decompress(data: string, orderedIndex: OrderedIndex): any;
/**
 * Decompress zipson data incrementally by providing each chunk of data in sequence
 */
export declare function decompressIncremental(orderedIndex: any): {
    increment: (data: string | null) => void;
    cursor: Cursor;
};
