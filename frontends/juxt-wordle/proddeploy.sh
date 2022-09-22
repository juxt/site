SITE_BASE_URI='https://alexd.uk/site'
GRAPHQL_PATH='/wordle/graphql'
GRAPHQL_API_URL="${SITE_BASE_URI}${GRAPHQL_PATH}"
yarn generate
yarn build
site put-static-site -d build -p _apps/wordle --spa true
