import { Context, InvertedIndex, CompressOptions, Compressors } from "./common";
import { ZipsonWriter } from "./writer";
/**
 * Compress string to
 */
export declare function compressString(compressors: Compressors, context: Context, obj: string, invertedIndex: InvertedIndex, writer: ZipsonWriter, options: CompressOptions): void;
