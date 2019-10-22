# heritrixctl

Java library for running and controlling [Heritrix](https://github.com/internetarchive/heritrix3) via the
[REST API](https://heritrix.readthedocs.io/en/latest/api.html).

## Basic Usage

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
