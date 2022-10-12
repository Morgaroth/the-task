Libs
===
* tapir - abstraction over http endpoints
  * brings swagger docs + UI out of the box
  * openapi doc as well, however not used here
* zio - because I'm more familiar with ZIO syntax than with cats-effect's one.
* circe 
* circe-json-schema - maybe not the famous lib for schema validation, but compatible with circe, that helps
* better-files - for nicer syntax for java's File

Running
===

```shell
sbt run
```
then open http://localhost:8080/docs to see swagger UI and play with endpoints
