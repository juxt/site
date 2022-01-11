.PHONY: build install token

build:
	yarn
	yarn build

token:
	site -s check-token || site get-token -u admin -p admin

install:	token
	site put-static-site -d build -p _site/insite --spa true