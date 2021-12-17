/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 */

import * as React from 'react';
import {StaticCodeBlock} from './CodeBlock';

interface PackageImportProps {
  children: React.ReactNode;
}

export function PackageImport({children}: PackageImportProps) {
  const terminal = React.Children.toArray(children).filter((child: any) => {
    return !child.props.children.props?.metastring;
  });
  const code = React.Children.toArray(children).map((child: any, i: number) => {
    if (child.props.children.props?.metastring) {
      return (
        <StaticCodeBlock
          {...child.props.children.props}
          key={i}
        />
      );
    } else {
      return null;
    }
  });
  return (
    <section className="my-8 grid grid-cols-1 lg:grid-cols-2 gap-x-8 gap-y-4">
      <div className="flex flex-col justify-center gap-4">{terminal}</div>
      <div className="flex flex-col justify-center">{code}</div>
    </section>
  );
}
