import { createRoot } from 'react-dom/client'
import 'tailwindcss/tailwind.css'
import { App } from 'components/App'
import { ReactQueryDevtools } from 'react-query/devtools'
import 'react-toastify/dist/ReactToastify.css'
import { QueryClient, QueryClientProvider } from 'react-query'
import { ToastContainer } from 'react-toastify'
import { Outlet, Router } from '@tanstack/react-location'
import { Worker } from '@react-pdf-viewer/core'
import { NewLocation } from './utils'

const location = NewLocation()
const queryClient = new QueryClient()
const container = document.getElementById('root') as HTMLDivElement
const root = createRoot(container)

root.render(
  <QueryClientProvider client={queryClient}>
    <Router
      location={location}
      routes={[
        {
          path: '/_apps/kanban/index.html',
          element: <App />
        },
        {
          path: '/_apps/kanban',
          element: <App />
        },
        {
          path: '/',
          element: <App />
        }
      ]}
    >
      <Worker workerUrl="https://unpkg.com/pdfjs-dist@2.12.313/build/pdf.worker.min.js">
        <Outlet />
      </Worker>
    </Router>
    <ToastContainer
      position="bottom-center"
      autoClose={3000}
      hideProgressBar={false}
      newestOnTop={false}
      closeOnClick
      rtl={false}
      pauseOnFocusLoss
      draggable
      pauseOnHover
    />
    <ReactQueryDevtools initialIsOpen={false} />
  </QueryClientProvider>
)
