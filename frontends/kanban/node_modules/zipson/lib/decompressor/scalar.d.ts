import { OrderedIndex, Cursor, Scalar, SkipScalar } from "./common";
export declare function decompressScalar(token: string, data: string, cursor: Cursor, orderedIndex: OrderedIndex): Scalar | SkipScalar;
