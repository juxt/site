import { Editor as CoreEditor } from '@tiptap/core';
import React from 'react';
import { EditorContentProps, EditorContentState } from './EditorContent';
export declare class Editor extends CoreEditor {
    contentComponent: React.Component<EditorContentProps, EditorContentState> | null;
}
