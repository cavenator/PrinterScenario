PrinterScenario
===============

This is a simple project which allowed me to focus on development with Akka Actors in Scala.  The scenario models a real-life scenario:  Multiple people using the same printer can send multiple documents to the printer without interfering with others request. The printer is responsible for queueing requests while printing is in-progress.  However, the printer can fail sporadically so it must be supervised appropriately.

The supervisor strategy currently has the printer terminate and provide each user a list of their remaining documents if the printer has to restart itself X amount of times within a time limit.  One could easily adjust the restart frequently and experiment with the scenario if desired.

To execute from command line, "mvn compile exec:java" (even though it was built in Scala).  You could always execute Main from within your favorite IDE too.

If you would prefer to execute the jar instead, be sure you execute maven's package goal "mvn package".  Then you should be able to execute the jar by typing "java -jar <relative path to jar>" (granted java is in your classpath).
