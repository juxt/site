# InSite Console

This console contains several tools for exploring and manipulating the data in your InSite project.

By default we assume your Site instance is running on the same domain as this console, if you would like to host the console elsewhere (or for local dev purposes) change the endpoint parameter in codegen.js

Then to install the console, run the following command:

```
make build
make install
```

You will need node.js and yarn to install the console, if you get errors check you are on the lts version of node.

Once installed you can visit `{{base-uri}}/_site/insite/home` to view the console

## Troubleshooting

If you get any graphql errors, try running (update-site-graphql) from the repl to make sure sites graphql schema is up to date.

If you get errors building the console, make sure node is on the lts version.
