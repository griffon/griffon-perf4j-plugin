/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Andres Almiray
 */
class Perf4jGriffonPlugin {
    // the plugin version
    String version = '1.0.1'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.3.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [lombok: '0.5.0']
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-perf4j-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Trace performance with Perf4j'

    String description = '''
The Perf4j Plugin integrates the [Perf4J profiling library][1] into Griffon.
Perf4J is a simple yet powerful utility for collecting performance statistics.

It is easily described with this analogy (taken from their website):

> Perf4J is to System.currentTimeMillis() as log4j is to System.out.println()

For detailed information, please have a look at the [Developer Guide][2].

The general idea of Perf4J is to wrap critical code blocks with some timing code,
which will then result in a message (including timing information) being logged
for each execution of such a code block. Perf4J supports various logging
infrastructures, however this plugin sticks with Slf4j, since this is the
built-in standard for Griffon applications. So, basically, each execution of a
profiled code block results in a Log4J message like this:

    INFO  perf4j.TimingLogger  - start[1236523341290] time[234] tag[someCodeBlock]

Taken alone, this is not very meaningful. However, Perf4J provides some special
Log4j appenders that group these single timing messages together and analyze them.
You can for example output min/max/mean execution times as well as TPS for each
code block in 30 second intervals.

Usage
-----

The plugin will inject the following dynamic methods:

 * `<R> R withStopwatch(Closure<R> closure)`
 * `<R> R withStopwatch(Callable<R> callable)`
 * `<R> R withStopwatch(Map<String, Object> params, Closure<R> closure)`
 * `<R> R withStopwatch(Map<String, Object> params, Callable<R> callable)`

Where params may contain

| Property                     | Type     | Required  | Default |
| ---------------------------- | -------- | --------- | ------- |
| tag                          | String   | no        |         |
| message                      | String   | no        |         |
| enabled                      | boolean  | no        | true    |
| exceptionPriority            | int      | no        |         |
| normalAndSlowSuffixesEnabled | boolean  | no        | false   |
| normalPriority               | int      | no        |         |
| normalSuffix                 | String   | no        |         |
| slowSuffix                   | String   | no        |         |
| timeThreshold                | long     | no        |         |

All of these properties (except `tag` and `message`) may be specified in the
application's configuration (`Config.groovy`) using `perf4j.` as a prefix.
Values specified in this way are considered global to every invocation of the
`withStopwatch()` methods.

These methods are also accessible to any component through the singleton
`griffon.plugins.perf4j.DefaultPerf4jProvider`. You can inject these methods to
non-artifacts via metaclasses. Simply grab hold of a particular metaclass and
call `griffon.plugins.perf4j.Perf4jEnhancer.enhance(metaClassInstance)`.

Configuration
-------------

### Perf4jAware AST Transformation

The preferred way to mark a class for method injection is by annotating it with
`@griffon.plugins.perf4j.Perf4jAware`. This transformation injects the
`griffon.plugins.perf4j.Perf4jContributionHandler` interface and default behavior
that fulfills the contract.

### Dynamic Method Injection

Dynamic methods will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.perf4j.injectInto = ['controller', 'service']

Dynamic method injection will be skipped for classes implementing
`griffon.plugins.perf4j.Perf4jContributionHandler`.

### Logging Appenders

The greatest power of Perf4J lies in its Log4J appenders, which allow to transform
the raw collected performance data into meaningful statistics and charts. It's
highly recommended that you read the [section about log4j appenders][3] in the
Perf4J Developer Guide!

The following is just an example to get you started with the Griffon syntax for
custom appender configuration in `Config.groovy`. For more information on appender
configuration in Griffon read the [logging section][4] in the Griffon User Guide.

    import org.perf4j.log4j.*
    import org.apache.log4j.FileAppender

    log4j = {
        // Example of changing the log pattern for the default console
        // appender:
        appenders {
            console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c - %m%n')

            // file appender that writes out the URLs of the Google Chart API
            // graphs generated by the performanceGraphAppender
            def performanceGraphFileAppender = new FileAppender(
                fileName: "log/perfGraphs.log",
                layout: pattern(conversionPattern: '%m%n'))
            appender name: 'performanceGraphFileAppender', performanceGraphFileAppender

            // this appender creates the Google Chart API graphs
            def performanceGraphAppender = new GraphingStatisticsAppender(
                graphType: 'Mean', // possible options: Mean, Min, Max, StdDev, Count or TPS
                tagNamesToGraph: 'tag1,tag2', dataPointsPerGraph: 5)
            performanceGraphAppender.addAppender(performanceGraphFileAppender)
            appender name: 'performanceGraph', performanceGraphAppender

            // file appender that writes out the textual, aggregated performance
            // stats generated by the performanceStatsAppender
            def performanceStatsFileAppender = new FileAppender(
                fileName: "log/perfStats.log",
                layout: pattern(conversionPattern: '%m%n'))
            // alternatively use the StatisticsCsvLayout to generate CSV
            appender name: 'performanceStatsFileAppender', performanceStatsFileAppender

            // this is the most important appender and first in the appender chain.
            // It aggregates all profiling data withing a certain time frame.
            // the GraphingStatisticsAppender is attached as a child to this appender
            // and uses its aggregated data.
            def performanceStatsAppender = new AsyncCoalescingStatisticsAppender(timeSlice: 10000/*ms*/)
            performanceStatsAppender.addAppender(performanceStatsFileAppender)
            performanceStatsAppender.addAppender(performanceGraphAppender)
            appender name: 'performanceStatsAppender', performanceStatsAppender
        }

        info performanceStatsAppender: 'org.perf4j.TimingLogger'

        error  'org.codehaus.griffon'

        info  'griffon.util',
               'griffon.core',
               'griffon.swing',
               'griffon.app'
    }


Testing
-------

Dynamic methods will not be automatically injected during unit testing, because
addons are simply not initialized for this kind of tests. However you can use
`Perf4jEnhancer.enhance(metaClassInstance, perf4jProviderInstance)` where
`perf4jProviderInstance` is of type `griffon.plugins.perf4j.Perf4jProvider`.
The contract for this interface looks like this

    public interface Perf4jProvider {
        <R> R withStopwatch(Closure<R> closure);
        <R> R withStopwatch(Callable<R> callable);
        <R> R withStopwatch(Map<String, Object> params, Closure<R> closure);
        <R> R withStopwatch(Map<String, Object> params, Callable<R> callable);
    }

It's up to you define how these methods need to be implemented for your tests.
For example, here's an implementation that never fails regardless of the
arguments it receives

    class MyPerf4jProvider implements Perf4jProvider {
        public <R> R withStopwatch(Closure<R> closure) { null }
        public <R> R withStopwatch(Callable<R> callable) { null }
        public <R> R withStopwatch(Map<String, Object> params, Closure<R> closure) { null }
        public <R> R withStopwatch(Map<String, Object> params, Callable<R> callable) { null }
    }
    
This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            Perf4jEnhancer.enhance(service.metaClass, new MyPerf4jProvider())
            // exercise service methods
        }
    }

On the other hand, if the service is annotated with `@Perf4jAware` then usage
of `Perf4jEnhancer` should be avoided at all costs. Simply set
`perf4jProviderInstance` on the service instance directly, like so, first the
service definition

    @griffon.plugins.perf4j.Perf4jAware
    class MyService {
        def serviceMethod() { ... }
    }

Next is the test

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            service.perf4jProvider = new MyPerf4jProvider()
            // exercise service methods
        }
    }

Tool Support
------------

### DSL Descriptors

This plugin provides DSL descriptors for Intellij IDEA and Eclipse (provided
you have the Groovy Eclipse plugin installed). These descriptors are found
inside the `griffon-perf4j-compile-x.y.z.jar`, with locations

 * dsdl/perf4j.dsld
 * gdsl/perf4j.gdsl

### Lombok Support

Rewriting Java AST in a similar fashion to Groovy AST transformations is
possible thanks to the [lombok][5] plugin.

#### JavaC

Support for this compiler is provided out-of-the-box by the command line tools.
There's no additional configuration required.

#### Eclipse

Follow the steps found in the [Lombok][5] plugin for setting up Eclipse up to
number 5.

 6. Go to the path where the `lombok.jar` was copied. This path is either found
    inside the Eclipse installation directory or in your local settings. Copy
    the following file from the project's working directory

         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/perf4j-<version>/dist/griffon-perf4j-compile-<version>.jar .

 6. Edit the launch script for Eclipse and tweak the boothclasspath entry so
    that includes the file you just copied

        -Xbootclasspath/a:lombok.jar:lombok-pg-<version>.jar:griffon-lombok-compile-<version>.jar:griffon-perf4j-compile-<version>.jar

 7. Launch Eclipse once more. Eclipse should be able to provide content assist
    for Java classes annotated with `@Perf4jAware`.

#### NetBeans

Follow the instructions found in [Annotation Processors Support in the NetBeans
IDE, Part I: Using Project Lombok][6]. You may need to specify
`lombok.core.AnnotationProcessor` in the list of Annotation Processors.

NetBeans should be able to provide code suggestions on Java classes annotated
with `@Perf4jAware`.

#### Intellij IDEA

Follow the steps found in the [Lombok][5] plugin for setting up Intellij IDEA
up to number 5.

 6. Copy `griffon-perf4j-compile-<version>.jar` to the `lib` directory

         $ pwd
           $USER_HOME/Library/Application Support/IntelliJIdea11/lombok-plugin
         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/perf4j-<version>/dist/griffon-perf4j-compile-<version>.jar lib

 7. Launch IntelliJ IDEA once more. Code completion should work now for Java
    classes annotated with `@Perf4jAware`.


[1]: http://perf4j.codehaus.org/
[2]: http://perf4j.codehaus.org/devguide.html
[3]: http://perf4j.codehaus.org/devguide.html#Using_the_log4j_Appenders_to_Generate_Real-Time_Performance_Information
[4]: http://griffon.codehaus.org/guide/latest/guide/configuration.html#logging
[5]: /plugin/lombok
[6]: http://netbeans.org/kb/docs/java/annotations-lombok.html
'''
}
