# blog

To run locally:

```
hugo server --disableFastRender --bind "0.0.0.0" --minify --theme book
```

Awesome way to update sub modules to the latest

```
git submodule foreach git pull origin master
```

Uses CircleCI to build and publish
