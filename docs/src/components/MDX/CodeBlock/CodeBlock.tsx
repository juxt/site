/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 */

import * as React from 'react';
import cn from 'classnames';
import {CopyBlock, ocean} from 'react-code-blocks';
import {
  ClasserProvider,
  SandpackCodeViewer,
  SandpackProvider,
  SandpackThemeProvider,
} from '@codesandbox/sandpack-react';
import rangeParser from 'parse-numeric-range';
import {CustomTheme} from '../Sandpack/Themes';
import styles from './CodeBlock.module.scss';
import {CodeMirrorRef} from '@codesandbox/sandpack-react/dist/types/components/CodeEditor/CodeMirror';
import {IconTerminal} from 'components/Icon/IconTerminal';
import {useRouter} from 'next/router';

type InlineHiglight = {
  step: number;
  line: number;
  startColumn: number;
  endColumn: number;
};

/**
 *
 * @param metastring string provided after the language in a markdown block
 * @returns array of lines to highlight
 * @example
 * ```js {1-3,7} [[1, 1, 20, 33], [2, 4, 4, 8]] App.js active
 * ...
 * ```
 *
 * -> The metastring is `{1-3,7} [[1, 1, 20, 33], [2, 4, 4, 8]] App.js active`
 */
function getHighlights(metastring: string): number[] {
  const HIGHLIGHT_REGEX = /{([\d,-]+)}/;
  const parsedMetastring = HIGHLIGHT_REGEX.exec(metastring);
  if (!parsedMetastring) {
    return [];
  }
  return rangeParser(parsedMetastring[1]);
}

export const CodeBox = React.forwardRef(function CodeBlock(
  {
    children,
    className = 'language-js',
    metastring,
    noMargin,
    noMarkers,
  }: {
    children: string;
    className?: string;
    metastring: string;
    noMargin?: boolean;
    noMarkers?: boolean;
  },
  ref?: React.Ref<CodeMirrorRef>
) {
  const getDecoratedLineInfo = () => {
    if (!metastring) {
      return [];
    }

    const linesToHighlight = getHighlights(metastring);
    const highlightedLineConfig = linesToHighlight.map((line) => {
      return {
        className: 'bg-github-highlight dark:bg-opacity-10',
        line,
      };
    });

    const inlineHighlightLines = getInlineHighlights(metastring, children);
    const inlineHighlightConfig = inlineHighlightLines.map(
      (line: InlineHiglight) => ({
        ...line,
        elementAttributes: {'data-step': `${line.step}`},
        className: cn('code-step bg-opacity-10 relative rounded-md p-1 ml-2', {
          'pl-3 before:content-[attr(data-step)] before:block before:w-4 before:h-4 before:absolute before:top-1 before:-left-2 before:rounded-full before:text-white before:text-center before:text-xs before:leading-4':
            !noMarkers,
          'bg-blue-40 before:bg-blue-40': line.step === 1,
          'bg-yellow-40 before:bg-yellow-40': line.step === 2,
          'bg-green-40 before:bg-green-40': line.step === 3,
          'bg-purple-40 before:bg-purple-40': line.step === 4,
        }),
      })
    );

    return highlightedLineConfig.concat(inlineHighlightConfig);
  };

  // e.g. "language-js"
  const language = className.substring(9);
  const filename = '/index.' + language;
  const decorators = getDecoratedLineInfo();
  return (
    <div
      translate="no"
      className={cn(
        'rounded-lg h-full w-full overflow-x-auto flex items-center bg-wash dark:bg-gray-95 shadow-lg',
        !noMargin && 'my-8'
      )}>
      <SandpackProvider
        customSetup={{
          entry: filename,
          files: {
            [filename]: {
              code: children.trimEnd(),
            },
          },
        }}>
        <SandpackThemeProvider theme={CustomTheme}>
          <ClasserProvider
            classes={{
              'sp-cm': styles.codeViewer,
            }}>
            <SandpackCodeViewer
              ref={ref}
              showLineNumbers={false}
              decorators={decorators}
            />
          </ClasserProvider>
        </SandpackThemeProvider>
      </SandpackProvider>
    </div>
  );
});

/**
 *
 * @param metastring string provided after the language in a markdown block
 * @returns array of lines to highlight
 * @example
 * ```js {1-3,7} [[1, 1, 20, 33], [2, 4, 4, 8]] App.js active
 * ...
 * ```
 *
 * -> The metastring is `{1-3,7} [[1, 1, 20, 33], [2, 4, 4, 8]] App.js active`
 */
function getHighlightLines(metastring: string, selected: number): string {
  try {
    const data = JSON.parse(metastring) as number[][];
    const [from, to] = data[selected];
    let highlights = '';
    if (from <= 0 || to < from) {
      throw new Error('syntax is [[from, to] ...]');
    } else {
      const n = to - from;
      const range = Array(n)
        .fill(0)
        .map((_element, index) => index + from);
      highlights = range.join(',');
    }

    return highlights;
  } catch (e) {
    console.error('couldnt parse lines', e);
    return '';
  }
}

/**
 *
 * @param metastring string provided after the language in a markdown block
 * @returns InlineHighlight[]
 * @example
 * ```js {1-3,7} [[1, 'count'], [4, 'setCount']] App.js active
 * ...
 * ```
 *
 * -> The metastring is `{1-3,7} [[1, 'count', [4, 'setCount']] App.js active`
 */
function getInlineHighlights(metastring: string, code: string) {
  const INLINE_HIGHT_REGEX = /(\[\[.*\]\])/;
  const parsedMetastring = INLINE_HIGHT_REGEX.exec(metastring);
  if (!parsedMetastring) {
    return [];
  }

  const lines = code.split('\n');
  const encodedHiglights = JSON.parse(parsedMetastring[1]);
  return encodedHiglights.map(
    ([lineNo, substr, fromIndex]: any[], idx: number) => {
      const line = lines[lineNo - 1];
      let index = line.indexOf(substr);
      const lastIndex = line.lastIndexOf(substr);
      if (index !== lastIndex) {
        if (fromIndex === undefined) {
          throw Error(
            "Found '" +
              substr +
              "' twice. Specify fromIndex as the third value in the vector."
          );
        }
        index = line.indexOf(substr, fromIndex);
      }
      if (index === -1) {
        throw Error(
          "Could not find: '" +
            substr +
            "' in line " +
            lineNo +
            '. Line content = ' +
            line
        );
      }
      return {
        step: idx + 1,
        line: lineNo,
        startColumn: index,
        endColumn: index + substr.length,
      };
    }
  );
}

type StaticCodeProps = {
  className?: string;
  metastring?: string;
  title?: string;
};

export const StaticCodeBlock: React.FC<StaticCodeProps> = React.forwardRef(
  function StaticCodeBlock (props, _ref) {
    // if any language selected or javascript by default
    const {className, children} = props;
    const router = useRouter();
    const selected = router.query.selected;

    const highlightedLines =
      props?.metastring?.length && selected
        ? getHighlightLines(props.metastring, parseInt(selected.toString()) - 1)
        : '1-100000'; //really?

    const language =
      className?.split('-')[0] === 'language'
        ? className.split('-')[1]
        : 'javascript';

    return (
      <div
        className={styles.staticCodeBlock}
        style={{background: ocean.backgroundColor}}>
        <div
          className="w-full rounded-t-lg"
          style={{background: ocean.functionColor}}>
          <div className="text-primary dark:text-primary flex text-sm px-4 py-0.5 relative">
            <IconTerminal className="inline-flex mr-2 self-center" />{' '}
            {props.title || props.metastring || 'Terminal'}
          </div>
        </div>
        <CopyBlock
          text={children}
          showLineNumbers={false}
          language={language}
          highlight={highlightedLines}
          theme={ocean}
          customStyle={{
            borderRadius: '10px',
            height: '100%',
          }}
          wrapLines
          codeBlock
        />
      </div>
    );
  }
);