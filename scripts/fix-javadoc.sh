#!/bin/bash -eu

shopt -s globstar

style='pre[class*="language-"]::after, pre[class*="language-"]::before { box-shadow: unset; }'
style+=' pre[class*="language-"] > code { z-index: unset; }</style>'

additions=(
    '<link rel="icon" type="image/png" href="/tree-sitter/assets/images/favicon-32x32.png" sizes="32x32">'
    '<link rel="icon" type="image/png" href="/tree-sitter/assets/images/favicon-16x16.png" sizes="16x16">'
    '<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism-coy.min.css" '
    ' integrity="sha256-pO/fumqgrAT0qA1DpFzDZg5oE3ur2H1xhD8fpTnZoIY=" crossorigin="anonymous">'
    '<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/plugins/toolbar/prism-toolbar.min.css" '
    ' integrity="sha256-jsQbSO1yj62eu6dP6f2fJESRgkHNNEpxrK8NnO6/oyc=" crossorigin="anonymous">'
    '<script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-core.min.js" type="text/javascript" '
    ' integrity="sha256-4mJNT2bMXxcc1GCJaxBmMPdmah5ji0Ldnd79DKd1hoM=" crossorigin="anonymous" defer></script>'
    '<script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/plugins/toolbar/prism-toolbar.min.js" type="text/javascript" '
    ' integrity="sha256-NSwb7O0HrDJcC+2SASgGuCP8+tdpIhqnzLTZkGRJRCk=" crossorigin="anonymous" defer></script>'
    '<script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js" type="text/javascript" '
    ' integrity="sha256-qf3MqHLzDh4qqAnc9QVmqjEWBA4CfzP1YwRFbfNfpnE=" crossorigin="anonymous" defer></script>'
    '<script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-clike.min.js" type="text/javascript" '
    ' integrity="sha256-x2uk4kCTK9x1VGvjDlUPW6XhOBX/cVEcduniesMHJEQ=" crossorigin="anonymous" defer></script>'
    '<script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-java.min.js" type="text/javascript" '
    ' integrity="sha256-TC3IHfye+lHjinVzk4BlKIxjxkhQ8Boy+KeyCj4kxac=" crossorigin="anonymous" defer></script>'
    "<style>$style</style>"
)

for f in "${1:-target}"/reports/apidocs/**/*.html; do
    for line in "${additions[@]}"; do
        sed -i "/<\/head>/i $line" "$f"
    done
done
