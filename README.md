# WirehubCheck
WirehubCheck will monitor Wirehub to check that it is running.
It queries the current number of articles in Wirehub and compares it to the
previous run. If the number has not increased by 5 articles in a hour it sends
an email as an alert.

It will be deployed as WirehubCheck.jar and run with the Nagios system.

To Build:<br>
export CLASSPATH=/path_to/j2ee.jar:.<br>
javac WirehubCheck.java<br>
jar cvfm WirehubCheck.jar manifest.txt *.class j2ee.jar<br>

Syntax to run is:<br>
java -jar WirehubCheck.jar http://XYZ.com/wp-json/wp/v2/posts
