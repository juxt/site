import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
//import App from './App';
import reportWebVitals from './reportWebVitals';
import Page from './Page';

import {
  ApolloClient,
  InMemoryCache,
  ApolloProvider,
  useQuery,
  gql,
  createHttpLink
} from "@apollo/client";

const link = createHttpLink({
  uri: 'https://home.test/_site/graphql',
  credentials: 'include'
});

const client = new ApolloClient({
  cache: new InMemoryCache(),
  link,
});

// Does nothing interesting, just a test
client
  .query({
    query: gql`
    query GetRequest {
      request(id: "https://home.test/_site/requests/81ff3d9ed88e7440658291cc") {
        id
        status
        date
      }
    }
    `
  })
  .then(result => console.log(result));

const REQUEST_SUMMARY = gql`
query GetRequestSummary {
  request(id: "https://home.test/_site/requests/81ff3d9ed88e7440658291cc") {
    id
    status
    date
  }
}
`

ReactDOM.render(
  <React.StrictMode>
    <ApolloProvider client={client}>
      <Page />
    </ApolloProvider>
  </React.StrictMode>,
  document.getElementById('root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
