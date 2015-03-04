FROM maven:3-jdk-8

COPY . /rss2twitter
WORKDIR /rss2twitter

RUN mvn package

CMD java -jar target/rss2twitter-1.0-SNAPSHOT.jar --spring.profiles.active=ippon
