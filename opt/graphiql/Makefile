.PHONY: build deploy token

build:
	yarn
	yarn build

token:
	site -s check-token || site get-token

deploy:	token
	site put-static-site -d build -p _site/graphiql
