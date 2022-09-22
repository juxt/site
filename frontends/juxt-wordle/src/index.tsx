import React from 'react'
import ReactDOM from 'react-dom'
import './index.css'
import App from './App'
import reportWebVitals from './reportWebVitals'
import { ReactQueryDevtools } from 'react-query/devtools'
import { QueryClient, QueryClientProvider } from 'react-query'

const queryClient = new QueryClient()
const username = localStorage.getItem('username')?.toLowerCase().trim()

if (username) {
  ReactDOM.render(
    <React.StrictMode>
      <QueryClientProvider client={queryClient}>
        <App username={username} />
        <ReactQueryDevtools />
      </QueryClientProvider>
    </React.StrictMode>,
    document.getElementById('root')
  )
}

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()
