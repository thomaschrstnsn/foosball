#! /bin/sh

# TODO: use terminal-notifier if non-empty $1
# echo "got extra parameters: '$1'"

phantomjs extern/test/runner-none.js target/cljs/test target/cljs/testable.js extern/test/bind-shim.js resources/public/js/extern/react-0.9.0.js skipRootBind=true
