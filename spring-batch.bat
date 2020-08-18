gradlew clean build
cd build\libs
java -jar spring-batch-0.0.1-SNAPSHOT.jar "item=shoes" "run.date(date)=2020/08/02"