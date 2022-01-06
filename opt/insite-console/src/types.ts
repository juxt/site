import {MakeGenerics} from 'react-location';

export type LocationGenerics = MakeGenerics<{
  Params: {
    requestId: string;
  };
  Search: {
    requestId: string;
    graphqlUrl: string;
    query: string;
  };
}>;
