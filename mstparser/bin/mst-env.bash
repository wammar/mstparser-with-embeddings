#!/bin/bash
# sets MST environment variables
export MST_LIB=$MSTPARSER_DIR/lib
export DIRLIBS=$MST_LIB/trove.jar
export CP=${MSTPARSER_DIR}/output/classes:${DIRLIBS}
export JAVA=$JAVA_HOME/bin/java
export JAVA_CMD="$JAVA -Xmx1800m -classpath $CP "
