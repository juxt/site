import {MakeGenerics} from 'react-location';

export type LocationGenerics = MakeGenerics<{
  Params: {
    requestId: string;
  };
  Search: {
    requestId: string;
    url: string;
    query: string;
  };
}>;
