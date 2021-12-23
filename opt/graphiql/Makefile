.PHONY: build install token

build:
	yarn
	yarn build

token:
	site -s check-token || site get-token

install:	token
	site put-static-site -d build -p _site/graphiql
