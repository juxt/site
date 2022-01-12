/* eslint-disable global-require */
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';
import {useSearch} from 'react-location';
import {LocationGenerics} from '../types';
import {Box} from '@mui/material';

function Swagger() {
  const {url} = useSearch<LocationGenerics>();
  // polyfils required for swagger-ui
  (window as any).process = {browser: true};
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  window.Buffer = window.Buffer || require('buffer').Buffer;

  return (
    <Box sx={{width: '100%', pb: 2}}>{url && <SwaggerUI url={url} />}</Box>
  );
}

export default Swagger;
