# InSite Console

This console contains several tools for exploring and manipulating the data in your InSite project.

If you do not have direnv installed, make sure you export the graphql API variable before running the build step (this example assumes you have site running at localhost on port 2021):
```
export GRAPHQL_API_URL='http://localhost:2021/_site/graphql'
```

Then to install the console, run the following command:

```
make build
make install
```

You will need node.js and yarn to install the console, if you get errors check you are on the lts version of node.

Once installed you can visit `{{base-uri}}/_site/insite/home` to view the console

## Troubleshooting

If you get any graphql errors, try running (update-site-graphql) from the repl to make sure sites graphql schema is up to date.

If you get errors building the console, make sure node is on the lts version and that the `GRAPHQL_API_URL` variable is set
