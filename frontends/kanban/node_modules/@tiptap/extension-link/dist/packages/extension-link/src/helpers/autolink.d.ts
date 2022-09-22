import { MarkType } from 'prosemirror-model';
import { Plugin } from 'prosemirror-state';
declare type AutolinkOptions = {
    type: MarkType;
    validate?: (url: string) => boolean;
};
export declare function autolink(options: AutolinkOptions): Plugin;
export {};
