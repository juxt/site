import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
//import reportWebVitals from './reportWebVitals';
import Page from './Page';

const mockRequest = require('./mockRequest.json').request;

ReactDOM.render(
  <div>
    <Page request={window.RESOURCE_STATE || mockRequest}/>
  </div>,
  document.getElementById('root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals

// reportWebVitals();
