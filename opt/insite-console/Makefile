.PHONY: build install token

build:
	@echo 'Console will use ${GRAPHQL_API_URL} as the Site endpoint - Make sure this is correct!'
	yarn
	yarn build

token:
	site -s check-token || site get-token -u admin -p admin

install:	token
	site put-static-site -d build -p _site/insite --spa true
