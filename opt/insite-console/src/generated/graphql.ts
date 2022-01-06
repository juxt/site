import { useQuery, UseQueryOptions } from 'react-query';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };

function fetcher<TData, TVariables>(query: string, variables?: TVariables) {
  return async (): Promise<TData> => {
    const res = await fetch("http://localhost:5509/_site/graphql", {
    method: "POST",
    ...({"headers":{"Content-Type":"application/json"},"credentials":"include"}),
      body: JSON.stringify({ query, variables }),
    });

    const json = await res.json();

    if (json.errors) {
      const { message } = json.errors[0];

      throw new Error(message);
    }

    return json.data;
  }
}
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  JSON: any;
};

export type Api = {
  __typename?: 'Api';
  contents: Scalars['JSON'];
  id: Scalars['ID'];
  type: ApiType;
};

export enum ApiType {
  Graphql = 'GRAPHQL',
  Openapi = 'OPENAPI'
}

/** Root query object */
export type Query = {
  __typename?: 'Query';
  /** Access user details for all users in the system */
  allUsers?: Maybe<Array<Maybe<SiteUser>>>;
  /** See the available openapi and graphql APIs in the system */
  apis: Array<Api>;
  /** Access a request */
  request?: Maybe<SiteRequest>;
  requests?: Maybe<SiteRequests>;
  /** Access the currently logged in user */
  subject?: Maybe<SiteSubject>;
  /** Access Site's system */
  system?: Maybe<SiteSystem>;
  /** Access a single user's details */
  user?: Maybe<Array<Maybe<SiteUser>>>;
};


/** Root query object */
export type QueryRequestArgs = {
  id: Scalars['ID'];
};


/** Root query object */
export type QueryRequestsArgs = {
  limit?: InputMaybe<Scalars['Int']>;
  offset?: InputMaybe<Scalars['Int']>;
};


/** Root query object */
export type QueryUserArgs = {
  username: Scalars['String'];
};

export type SiteAttributeStat = {
  __typename?: 'SiteAttributeStat';
  attribute: Scalars['String'];
  frequency: Scalars['Int'];
};

export type SiteConfiguration = {
  __typename?: 'SiteConfiguration';
  baseUri: Scalars['String'];
  serverPortNumber: Scalars['Int'];
  unixPassPasswordPrefix: Scalars['String'];
};

export type SiteDatabaseStatus = {
  __typename?: 'SiteDatabaseStatus';
  attributeStats: Array<SiteAttributeStat>;
  estimateNumKeys: Scalars['Int'];
  indexVersion: Scalars['Int'];
  kvSize: Scalars['Int'];
  kvStore: Scalars['String'];
  revision: Scalars['String'];
  version: Scalars['String'];
};

export type SiteRequest = {
  __typename?: 'SiteRequest';
  date: Scalars['String'];
  detail: Scalars['JSON'];
  id: Scalars['ID'];
  method: Scalars['String'];
  /** The GraphQL operation name called by the request */
  operationName?: Maybe<Scalars['String']>;
  requestUri: Scalars['ID'];
  status: Scalars['Int'];
};

export type SiteRequestSummary = {
  __typename?: 'SiteRequestSummary';
  date: Scalars['String'];
  details: Scalars['JSON'];
  id: Scalars['ID'];
  status: Scalars['Int'];
};

export type SiteRequests = {
  __typename?: 'SiteRequests';
  count: Scalars['Int'];
  summaries: Array<SiteRequestSummary>;
};

/** A Site role, for using in policies to grant authorization to access certain resources. */
export type SiteRole = {
  __typename?: 'SiteRole';
  description?: Maybe<Scalars['String']>;
  id: Scalars['ID'];
  name: Scalars['String'];
};

export type SiteStatusDetails = {
  __typename?: 'SiteStatusDetails';
  docStoreAvail: Scalars['Int'];
  indexStoreAvail: Scalars['Int'];
  txLogAvail: Scalars['Int'];
};

export type SiteSubject = {
  __typename?: 'SiteSubject';
  authScheme?: Maybe<Scalars['String']>;
  user?: Maybe<SiteUser>;
};

export type SiteSystem = {
  __typename?: 'SiteSystem';
  /** Access Site's configuration */
  configuration: SiteConfiguration;
  database: SiteDatabaseStatus;
  status: SiteStatusDetails;
  version: SiteVersionDetails;
};

/** An object representing a Site user. Anyone who needs to log in to the system must have a user record. */
export type SiteUser = {
  __typename?: 'SiteUser';
  email?: Maybe<Scalars['String']>;
  id: Scalars['ID'];
  name?: Maybe<Scalars['String']>;
  roles?: Maybe<Array<Maybe<SiteRole>>>;
  username?: Maybe<Scalars['String']>;
};

export type SiteVersionDetails = {
  __typename?: 'SiteVersionDetails';
  gitSha: Scalars['String'];
};

export type AllApisQueryVariables = Exact<{ [key: string]: never; }>;


export type AllApisQuery = { __typename?: 'Query', apis: Array<{ __typename?: 'Api', type: ApiType, id: string }> };

export type AllRequestsQueryVariables = Exact<{ [key: string]: never; }>;


export type AllRequestsQuery = { __typename?: 'Query', requests?: { __typename?: 'SiteRequests', summaries: Array<{ __typename?: 'SiteRequestSummary', date: string, id: string, status: number }> } | null | undefined };

export type GetRequestSummaryQueryVariables = Exact<{
  uri: Scalars['ID'];
}>;


export type GetRequestSummaryQuery = { __typename?: 'Query', request?: { __typename?: 'SiteRequest', id: string, status: number, date: string, requestUri: string, method: string, operationName?: string | null | undefined, detail: any } | null | undefined };


export const AllApisDocument = `
    query allApis {
  apis {
    type
    id
  }
}
    `;
export const useAllApisQuery = <
      TData = AllApisQuery,
      TError = Error
    >(
      variables?: AllApisQueryVariables,
      options?: UseQueryOptions<AllApisQuery, TError, TData>
    ) =>
    useQuery<AllApisQuery, TError, TData>(
      variables === undefined ? ['allApis'] : ['allApis', variables],
      fetcher<AllApisQuery, AllApisQueryVariables>(AllApisDocument, variables),
      options
    );
useAllApisQuery.document = AllApisDocument;


useAllApisQuery.getKey = (variables?: AllApisQueryVariables) => variables === undefined ? ['allApis'] : ['allApis', variables];
;

useAllApisQuery.fetcher = (variables?: AllApisQueryVariables) => fetcher<AllApisQuery, AllApisQueryVariables>(AllApisDocument, variables);
export const AllRequestsDocument = `
    query allRequests {
  requests {
    summaries {
      date
      id
      status
    }
  }
}
    `;
export const useAllRequestsQuery = <
      TData = AllRequestsQuery,
      TError = Error
    >(
      variables?: AllRequestsQueryVariables,
      options?: UseQueryOptions<AllRequestsQuery, TError, TData>
    ) =>
    useQuery<AllRequestsQuery, TError, TData>(
      variables === undefined ? ['allRequests'] : ['allRequests', variables],
      fetcher<AllRequestsQuery, AllRequestsQueryVariables>(AllRequestsDocument, variables),
      options
    );
useAllRequestsQuery.document = AllRequestsDocument;


useAllRequestsQuery.getKey = (variables?: AllRequestsQueryVariables) => variables === undefined ? ['allRequests'] : ['allRequests', variables];
;

useAllRequestsQuery.fetcher = (variables?: AllRequestsQueryVariables) => fetcher<AllRequestsQuery, AllRequestsQueryVariables>(AllRequestsDocument, variables);
export const GetRequestSummaryDocument = `
    query GetRequestSummary($uri: ID!) {
  request(id: $uri) {
    id
    status
    date
    requestUri
    method
    operationName
    detail
  }
}
    `;
export const useGetRequestSummaryQuery = <
      TData = GetRequestSummaryQuery,
      TError = Error
    >(
      variables: GetRequestSummaryQueryVariables,
      options?: UseQueryOptions<GetRequestSummaryQuery, TError, TData>
    ) =>
    useQuery<GetRequestSummaryQuery, TError, TData>(
      ['GetRequestSummary', variables],
      fetcher<GetRequestSummaryQuery, GetRequestSummaryQueryVariables>(GetRequestSummaryDocument, variables),
      options
    );
useGetRequestSummaryQuery.document = GetRequestSummaryDocument;


useGetRequestSummaryQuery.getKey = (variables: GetRequestSummaryQueryVariables) => ['GetRequestSummary', variables];
;

useGetRequestSummaryQuery.fetcher = (variables: GetRequestSummaryQueryVariables) => fetcher<GetRequestSummaryQuery, GetRequestSummaryQueryVariables>(GetRequestSummaryDocument, variables);