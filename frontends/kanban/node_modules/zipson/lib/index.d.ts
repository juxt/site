import { CompressOptions } from './compress';
import { ZipsonWriter } from './compressor/writer';
export * from './compressor/writer';
export * from './compressor/common';
export * from './decompressor/common';
/**
 * Parse a zipson data string
 */
export declare function parse(data: string): any;
/**
 * Incrementally parse a zipson data string in chunks
 */
export declare function parseIncremental(): (data: string | null) => any;
/**
 * Stringify any data to a zipson writer
 */
export declare function stringifyTo(data: any, writer: ZipsonWriter, options?: CompressOptions): void;
/**
 * Stringify any data to a string
 */
export declare function stringify(data: any, options?: CompressOptions): string;
