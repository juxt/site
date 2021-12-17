/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 */

import React from 'react';
import Head from 'next/head';
import {withRouter, Router} from 'next/router';
import {useTheme} from 'next-themes';

export interface SeoProps {
  title: string;
  description?: string;
  image?: string;
  // jsonld?: JsonLDType | Array<JsonLDType>;
  children?: React.ReactNode;
}

export const Seo = withRouter(
  ({
    title,
    description = 'A site for Site.',
    image = '/logo-og.png',
    router,
    children,
  }: SeoProps & {router: Router}) => {
    const {theme} = useTheme();
    const favicon = theme === 'light' ? '/favicon.ico' : '/favicon_dark.ico';
    return (
      <Head>
        {/* DEFAULT */}

        <meta name="viewport" content="width=device-width, initial-scale=1" />

        {title != null && <title key="title">{title}</title>}
        {description != null && (
          <meta name="description" key="description" content={description} />
        )}
        <link rel="icon" type="image/x-icon" href={favicon} />
        <link rel="apple-touch-icon" href={favicon} />
        {/* OPEN GRAPH */}
        <meta property="og:type" key="og:type" content="website" />
        <meta
          property="og:url"
          key="og:url"
          content={`https://juxt.site${router.pathname}`}
        />
        {title != null && (
          <meta property="og:title" content={title} key="og:title" />
        )}
        {description != null && (
          <meta
            property="og:description"
            key="og:description"
            content={description}
          />
        )}

        <meta
          property="og:image"
          key="og:image"
          content={`https://juxt.site${image}`}
        />

        {/* TWITTER */}
        <meta
          name="twitter:card"
          key="twitter:card"
          content="summary_large_image"
        />
        <meta name="twitter:site" key="twitter:site" content="@juxtpro" />
        <meta name="twitter:creator" key="twitter:creator" content="@juxtpro" />
        {title != null && (
          <meta name="twitter:title" key="twitter:title" content={title} />
        )}
        {description != null && (
          <meta
            name="twitter:description"
            key="twitter:description"
            content={description}
          />
        )}

        <meta
          name="twitter:image"
          key="twitter:image"
          content={`https://juxt.site${image}`}
        />

        {children}
      </Head>
    );
  }
);
