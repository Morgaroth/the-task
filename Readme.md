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


Thoughts
=== 
1. I could implement storage in mongo, because I have ready code to copy-paste from
[my another repo](https://gitlab.com/mateuszjaje/gitlab-merge-request-linker/-/blob/main/src/main/scala/gitlabbot/storage/mongo/MongoProjectSimpleInfoCache.scala), 
but amount of copied code would be too big comparing to gain.
2. I refrained from providing configuration (files storage parent dir, http port etc), 
but if I would, I would use `scopt` for this copying some pieces of code [from here](https://gitlab.com/mateuszjaje/scala-scraps/-/blob/main/src/main/scala/scraps/scoptparser.scala)
or use typesafe config wrapped by pureconfig

