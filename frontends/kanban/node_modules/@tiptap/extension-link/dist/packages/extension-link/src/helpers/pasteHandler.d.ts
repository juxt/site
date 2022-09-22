import { Editor } from '@tiptap/core';
import { MarkType } from 'prosemirror-model';
import { Plugin } from 'prosemirror-state';
declare type PasteHandlerOptions = {
    editor: Editor;
    type: MarkType;
};
export declare function pasteHandler(options: PasteHandlerOptions): Plugin;
export {};
