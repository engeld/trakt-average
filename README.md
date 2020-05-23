# trakt-average

## Background

### History
Everytime I watch something and it gets scrobbled to [trakt](https://trakt.tv), I meticulously rate the item. So I was always annoyed when I finished watching a season or a show and it didn't use my rated episodes to calculate my season and/or show average rating - why should I have to rate this separately, again?

### How this works
It finds shows ~~where either a season or a show is completed~~ which has watched episodes and then calculates the average over those items. Then it prints out those averages. In a second step, those values will be written back to trakt to update the season/show ratings.

### Why Java?
Because my java was getting rusty and I wanted to brush it up a little bit. Maybe I'll rewrite it in another language later, we'll see.

## How to use

### Use precompiled binary

Download the jar (here) and run:
`java -jar traktaverage-0.0.0-alpha.jar `

`java -jar target/traktaverage-0.0.0-alpha.jar -Dapp.properties="/Users/engeld/Dev/Workspaces/Eclipse/traktaverage/target/app.properties"`

### Use the source, Luke

Download the source (here) and run:
`java cc.engeld.traktaverage.App`

`/Library/Java/JavaVirtualMachines/adoptopenjdk-14.0.1.jdk/Contents/Home/bin/java -Dfile.encoding=UTF-8 -classpath /Users/engeld/Dev/Workspaces/Eclipse/traktaverage/target/classes:/Users/engeld/.m2/repository/com/uwetrottmann/trakt5/trakt-java/6.5.0/trakt-java-6.5.0.jar:/Users/engeld/.m2/repository/com/squareup/retrofit2/retrofit/2.6.4/retrofit-2.6.4.jar:/Users/engeld/.m2/repository/com/squareup/okhttp3/okhttp/3.12.0/okhttp-3.12.0.jar:/Users/engeld/.m2/repository/com/squareup/okio/okio/1.15.0/okio-1.15.0.jar:/Users/engeld/.m2/repository/com/squareup/retrofit2/converter-gson/2.6.4/converter-gson-2.6.4.jar:/Users/engeld/.m2/repository/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar:/Users/engeld/.m2/repository/org/threeten/threetenbp/1.4.1/threetenbp-1.4.1.jar:/Users/engeld/.m2/repository/org/apache/logging/log4j/log4j-api/2.13.3/log4j-api-2.13.3.jar:/Users/engeld/.m2/repository/org/apache/logging/log4j/log4j-core/2.13.3/log4j-core-2.13.3.jar cc.engeld.traktaverage.App`