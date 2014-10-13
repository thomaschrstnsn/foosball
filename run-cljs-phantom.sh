#! /bin/sh

function notify {
    terminal_notifier_exec="terminal-notifier"
    path_to_terminal_notifier=$(which $terminal_notifier_exec)
    if [ -x "$path_to_terminal_notifier" ] ; then
        $terminal_notifier_exec -title "$1" -message "$2"
    else
        echo $1 " - " $2
    fi
}

function phantom_test_runner {
    CLJSTEST="clojurescript.test"
    time phantomjs extern/test/runner-none.js target/cljs/test target/cljs/testable.js extern/test/bind-shim.js resources/public/extern/js/react-0.11.1.js
    if [[ $? -eq 0 ]];
    then
        notify $CLJSTEST "success"
    else
        notify $CLJSTEST "fail"
    fi
}

if [[ $# -eq 1 ]];
then
    notify "cljsbuild" $1
    if [[ $1 == *Successfully* ]]
    then
        phantom_test_runner
    else
        echo Error: during cljsbuild
    fi
else
    phantom_test_runner
fi;

date "+Finished at: %H:%M:%S"
