![build](https://github.com/jkatzwinkel/tla-es/workflows/build/badge.svg)
![deploy](https://github.com/jkatzwinkel/tla-es/workflows/deploy/badge.svg)
![search](https://github.com/jkatzwinkel/tla-es/workflows/searchtest/badge.svg)
![LINE](https://img.shields.io/badge/line--coverage-82.38%25-brightgreen.svg)
![METHOD](https://img.shields.io/badge/method--coverage-81.84%25-brightgreen.svg)

# tla-es

Thesaurus Linguae Aegyptiae (TLA) backend.

Copyright (C) 2019-2021 Berlin-Brandenburgische Akademie der Wissenschaften


## Overview

The TLA backend server is a Spring Boot application using Elasticsearch as a search engine.


## Installation

> [!TIP]
> the **TL;DR:** version of all of the below is:
> ```bash
> SAMPLE_URL=http://aaew64.bbaw.de/resources/tla-data/tla-sample-20210115-1000t.tar.gz docker compose up -d
> ```

There are two methods for getting this thing up and running.

1. [As a Docker container setup](#1-using-docker)
2. [Run or build with Gradle](#2-using-gradle)


### 1. Using Docker

Requirements:

- Docker Compose

The environment variable `SAMPLE_URL` is required. Add it to your environment any way you like, e.g.:

```bash
  export SAMPLE_URL=http://example.org/sample.tar.gz
```

Start the docker container setup configured in `docker-compose.yml`:

```bash
  docker compose up -d
```

This will build and run three containers:

- `tla-es`: Elasticsearch container
- `tla-ingest`: temporarily executed instance of the backend application, used for populating the Elasticsearch container
- `tla-backend`: the actual backend app

The `tla-ingest` container will take its time downloading the TLA corpus data archive file and uploading it into Elasticsearch.
You can check its progress by taking a look into its log output:

```bash
  docker logs -f tla-ingest
```


### 2. Using Gradle

Requirements:

- Java 17
- Elasticsearch 7.17.0 or higher *or* Docker Compose v2


#### 2.1. Prerequesites

1. This method requires you to provide a running Elasticsearch instance. If you have Docker Compose, you can simply start one in a
   container by using the configuration coming with this repository:
   ```bash
   docker compose up -d es
   ```
   Before continuing, make sure Elasticsearch is running by checking the output of `docker ps --all` or
   accessing [its REST interface](http://localhost:9200) in a browser (change `9200` in case that you
   set a different port via the `ES_PORT` environment variable).

2. Once Elasticsearch is up and running, TLA corpus data needs to be loaded into it. In order to do so,
   you must set the `SAMPLE_URL` environment variable to a URL pointing to a tar-compressed TLA corpus data
   file.

   ```bash
     export SAMPLE_URL=http://example.org/sample.tar.gz
   ```

3. Finally, download and store TLA corpus data from the specified source by running the `populate` gradle task:

   ```bash
   ./gradlew populate
   ```


#### 2.2. Run application

Run the app using the `bootRun` task:

```bash
  ./gradlew tasks # lists available gradle tasks
  ./gradlew bootrun
```



## Tests

There are 3 Gradle tasks for running tests:

- `:test`: run unit tests
- `:testSearch`: run search tests against live Elasticsearch instance
- `:testAll`: run all of those tests

Note that due to the way Spring-Data works, there is an Elasticsearch instance required even for the unit tests,
although it may well be entirely empty. For the search tests however, the Elasticsearch instance must be fully
populated so that search results can actually be verified against the specified expectations. This means you must
have executed the `:populate` task (`./gradlew populate`) prior executing `:testSearch` or `:testAll`.

Search tests are being performed based on search scenarios specified in JSON files. The specification model can be
found in [`SearchTestSpecs.java`](src/test/java/tla/backend/search/SearchTestSpecs.java). Individual specification
instances consist of at least a name and a search command. JSON files containing a list of several search test
specifications have to be located within the classpath directory set via the
[application property](src/test/resources/application-test.yml) `tla.searchtest.path`, each under a sub-directory
whose name can be used to identify the entity service to be used to execute the contained search commands.
The paths used to identify the entity services can be found in the `@ModelClass` annotations of the entity services.

Test runs create JUnit and Jacoco reports at the usual output locations.

Limit test runs to single classes by using the `--test` option:

```bash
  ./gradlew test --tests=QueryResultTest
```


## Misc

*Note:* You can configure the Elasticsearch HTTP port to which the application will try to connect.
Both the Docker Compose configuration and the `bootRun` and `test` gradle tasks are going to read
it from the local `.env` file.

When running the application using the  `bootRun` task, comma-separated arguments can be passed via
`args` property in the following ways:

```bash
  ./gradlew bootRun --Pargs=--data-file=sample.tar.gz,--foo=bar
  ./gradlew bootRun --args="--data-file=sample.tar.gz --foo=bar"
```

Populate database with a corpus dump and shut down after:

```bash
  ./gradlew bootRun --args="--data-file=sample.tar.gz --shutdown"
```

There is a gradle task for populating the backend app's elasticsearch indices with corpus data obtained
from a URL specified via the `SAMPLE_URL` environment variable:

```bash
  ./gradlew populate
```

You can check for the newest version of package dependencies by running:

```bash
  ./gradlew dependencyUpdates
```

<!--- vim: set ts=2 sw=2 tw=0 noet ft=markdown : -->
