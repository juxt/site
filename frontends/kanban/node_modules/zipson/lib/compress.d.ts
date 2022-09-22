import { Context, InvertedIndex, CompressOptions } from "./compressor/common";
import { ZipsonWriter } from "./compressor/writer";
export * from "./compressor/common";
/**
 * Create a new compression context
 */
export declare function makeCompressContext(): Context;
/**
 * Create an inverted index for compression
 */
export declare function makeInvertedIndex(): InvertedIndex;
/**
 * Compress all data onto a provided writer
 */
export declare function compress(context: Context, obj: any, invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions): void;
