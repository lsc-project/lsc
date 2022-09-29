#!/bin/bash


# replace <mail@lsc-project.org> with html compliant &lt;mail@lsc-project.org&gt; within java files
find . -name "*.java" | xargs sed -i  's/<\([a-z]*@lsc-project.org\)>/\&lt;\1\&gt;/'

# when '<>' was already fully removed
find . -name "*.java" | xargs sed -i 's/ \([a-z]*@lsc-project.org\)$/\&lt;\1\&gt;/'
