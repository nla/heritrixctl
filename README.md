# heritrixctl

Command-line tool and Java library for running and controlling the [Heritrix](https://github.com/internetarchive/heritrix3)
archival web crawler via its [REST API](https://heritrix.readthedocs.io/en/latest/api.html). It is intended to be used
for running Heritirx as a cronjob or controlling it from another application.

## Installing

Currently heritrixctl is in an early stage of development and must be compiled from source using [Maven](https://github.com/nla/heritrixctl.git).

    git clone https://github.com/nla/heritrixctl.git
    cd heritrixctl
    mvn package  # builds jar for command-line usage
    mvn install  # installs Java library
    
## Command-line usage

To connect to an existing instance of Heritrix specifying the URL and credentials via environment variables:

    export HERITRIX_URL=https://localhost:8443
    export HERITRIX_USER=admin
    export HERITIRX_PASSWORD=password
    
To run a new instance of Heritrix set HERITRIX_HOME. If the path does not exist heritrixctl will automatically
download the latest Heritrix build. When operating in this mode Heritrix will exit after the crawl finishes.

    export HERITRIX_HOME=/opt/heritrix
    
### Running a crawl

To run the crawl configuration from a file named `myjob.cxml` use:

    java -Xmx64m -jar heritrixctl.jar run myjob.cxml

## Java API

First connect to a running instance of Heritrix:

```java
HeritrixClient heritrix = new HeritrixClient("https://localhost:8443", "admin", "password");
heritrix.ignoreCertificateErrors(); // remove this if Heritrix is configured with a proper certificate
```

Or run a new local instance of Heritrix installed in the directory `heritrixHome`:

```java
try (HeritrixProcess heritrix = new HeritrixRunner(heritrixHome).start()) {
    heritrix.waitForStartup();
    ...
}
```

Or download the latest Heritrix snapshot, install into `heritrixHome` and run it:

```java
try (HeritrixProcess heritrix = HeritrixRunner.downloadLatestSnapshot(heritrixHome).start()) {
    heritrix.waitForStartup();
    ...
}
```
