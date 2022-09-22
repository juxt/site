import { MarkType } from 'prosemirror-model';
import { Plugin } from 'prosemirror-state';
declare type ClickHandlerOptions = {
    type: MarkType;
};
export declare function clickHandler(options: ClickHandlerOptions): Plugin;
export {};
