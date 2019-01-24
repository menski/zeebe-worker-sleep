# Zeebe :zzz: Job Worker

This job worker is only intended for testing purpose. It will activate jobs
and then :sleeping: before completing. This should simulate the work done by
the handler.


## Configuration

The worker can be configured using the `application.conf` or by setting Java
properties matching the configuration settings, or the respective environment
variable.

The default configuration can be found in the [`application.conf`] file.


## Docker Image

The worker is available as [docker image](https://cloud.docker.com/repository/docker/menski/zeebe-worker-sleep/tags).
It can be configured for example using the `JAVA_OPTS` and the configuration environment variables specified in the
[`application.conf`] file.

```
docker run --name worker -e CLIENT_GATEWAY=192.168.0.12:26500 menski/zeebe-worker-sleep:SNAPSHOT
```

[`application.conf`]: /src/main/resources/application.conf
