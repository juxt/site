#!/bin/bash

PATH=$HOME/src/github.com/juxt/site/bin:$PATH
user=mal
fullname="Malcolm Sparks"

# Use the webmaster account
site set username webmaster

# Get a token to interact with the Site server
site get-token

# Add the superuser role
#site put-role -r superuser

# Allow superusers to do anything
#site put-rule -n superuser -r rules/superuser-rule.json

# Create a new user
#site put-user -u $user -n "$fullname"

# Assign the user to the superuser role
#site assign-role -u $user -r superuser

# Switch user
site clear-token
site set username $user
site get-token

# Install Swagger UI
# (arguably this does not have to be a site command)
site put-swagger-ui

# Install CSS and compressed variants
site put-asset --file style/target/styles.css --type text/css --path /css/styles.css
site put-asset --file style/target/styles.css.gz --type text/css --encoding gzip --path /css/styles.css.gz
site put-variant -r https://home.juxt.site/css/styles.css -v https://home.juxt.site/css/styles.css.gz
site put-asset --file style/target/styles.css.br --type text/css --encoding br --path /css/styles.css.br
site put-variant -r https://home.juxt.site/css/styles.css -v https://home.juxt.site/css/styles.css.br

# Add login resources
site put-asset --file assets/favicon.ico --type image/x-icon --path /favicon.ico
site put-asset --file assets/login.html --type 'text/html;charset=utf-8' --path /login.html
site put-asset --file assets/live.js --type 'application/javascript' --path /js/live.js
site put-asset --file assets/juxt-logo-on-black.svg --type 'image/svg+xml' --path /juxt-logo-on-black.svg
site put-asset --file assets/juxt-logo-on-white.svg --type 'image/svg+xml' --path /juxt-logo-on-white.svg
site put-rule -n login-form -r rules/login-form-rule.json

# Add a redirect from / to a login page with a query parameters
site put-redirect -r / -l /login.html
