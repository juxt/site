import GraphiQL, {FetcherParams} from 'graphiql';
import GraphiQLExplorer from 'graphiql-explorer';
import {getIntrospectionQuery, buildClientSchema} from 'graphql';
import 'graphiql/graphiql.min.css';
import {useCallback, useEffect, useRef, useState} from 'react';
import {useNavigate, useSearch} from 'react-location';
import {LocationGenerics} from '../types';
import {useQuery} from 'react-query';

function fixExplorerStyles() {
  // nasty hack to make the explorer not rediculously wide, tempting to fork it really
  const explorerWrapEl = document.getElementsByClassName('docExplorerWrap')[0];
  const explorerContentsEl = document.getElementsByClassName(
    'doc-explorer-contents'
  )[0];
  explorerWrapEl.setAttribute('style', 'min-width: 200px');
  explorerContentsEl.setAttribute('style', 'min-width: 200px; padding: 0');
  const queryListEl = explorerContentsEl.childNodes[0].firstChild as Element;

  return queryListEl.setAttribute(
    'style',
    'overflow: auto; padding-left: 0.3em; flex-grow: 1;'
  );
}

const graphQLInitialFetcher = async (
  url: string,
  body = {query: getIntrospectionQuery()}
) => {
  const data = await fetch(url, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
    credentials: 'include',
  });
  return data
    .json()
    .catch(() => console.error(`error fetching from ${url}`, data));
};
const graphQLFetcher = async (params: FetcherParams, url: string) => {
  return graphQLInitialFetcher(url, params);
};
function Graphiql({url}: {url: string}) {
  const {data: schema, isLoading} = useQuery(
    url,
    () => graphQLInitialFetcher(url),
    {
      enabled: !!url,
      select: (result: {data: import('graphql').IntrospectionQuery}) => {
        if (result?.data) {
          return buildClientSchema(result.data);
        }
        return null;
      },
    }
  );
  const [explorerIsOpen, setExplorerIsOpen] = useState(true);
  const refGq = useRef<GraphiQL | null>(null);
  const navigate = useNavigate();
  const {query} = useSearch<LocationGenerics>();
  const handleEditQuery = useCallback((q?: string) => {
    navigate({replace: true, search: (old) => ({...old, query: q})});
  }, []);

  const handleToggleExplorer = useCallback(() => {
    setExplorerIsOpen((old) => !old);
  }, []);

  useEffect(() => {
    if (!isLoading && refGq.current) {
      fixExplorerStyles();
    }
    // needed to stop graphiql from remembering old queries which are potentially from the wrong api
    navigate({
      replace: true,
      search: (old) => ({...old, query: query || ''}),
    });
  }, [isLoading]);

  return (
    <>
      {isLoading ? (
        <div>Loading...</div>
      ) : (
        <>
          <GraphiQLExplorer
            schema={schema}
            query={query}
            onEdit={handleEditQuery}
            onRunOperation={(operationName?: string) =>
              refGq.current?.handleRunQuery(operationName)
            }
            explorerIsOpen={explorerIsOpen}
            onToggleExplorer={handleToggleExplorer}
          />
          <GraphiQL
            ref={refGq}
            schema={schema}
            fetcher={(params) => graphQLFetcher(params, url)}
            query={query}
            onEditQuery={handleEditQuery}
            defaultVariableEditorOpen
            toolbar={{
              additionalContent: (
                <GraphiQL.Button
                  onClick={handleToggleExplorer}
                  label="Explorer"
                  title="Toggle Explorer"
                />
              ),
            }}
          />
        </>
      )}
    </>
  );
}

function GraphiqlWrapper() {
  const {url} = useSearch<LocationGenerics>();

  return (
    <div className="graphiql-container">{url && <Graphiql url={url} />}</div>
  );
}

export default GraphiqlWrapper;
