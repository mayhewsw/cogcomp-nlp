mvn install:install-file -Dfile=lib/LBJava-1.3.0-SNAPSHOT.jar -DgroupId=edu.illinois.cs.cogcomp -DartifactId=LBJava -Dversion=1.3.0-SNAPSHOT -Dpackaging=jar
mvn dependency:copy-dependencies
