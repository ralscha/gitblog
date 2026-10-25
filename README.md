The application that powers my blog: https://blog.rasc.ch

For more information check out https://blog.rasc.ch/2020/05/gitblog.html

## Deployment after the Spring Boot 4 upgrade

Use Java 25 or newer and launch with `java -jar gitblog.jar`. Embedded Unix launch
scripts were removed from Spring Boot, so update any service whose `ExecStart`
directly executes the jar (including an `/etc/init.d` symlink) to call Java.

The embedded HTTP server is now Tomcat; review any external `server.undertow.*`
settings before deployment. The standard port, TLS, and reverse-proxy settings
remain Spring Boot settings. The Error MVC auto-configuration exclusion now uses
its Spring Boot 4 package name. GraalJS uses the Community runtime POM plus the
explicit ScriptEngine provider; the Maven tests verify provider discovery and
JavaScript invocation.

See [Spring Boot's migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).
