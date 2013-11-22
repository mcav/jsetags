# jsetags: Emacs TAGS file generator for JavaScript

This package generates an Emacs TAGS file from JavaScript sources. You can use Emacs TAGS files to jump from a function call to the place where the function is defined (`M-.`) and back again (`M-,`).

At present, it only indexes function declarations. I built this because I couldn't find an existing Emacs-compatible tags generator for JavaScript files that didn't take ages to run. Currentoly this can index thousands of files in only a couple seconds.

## Installation

Requires Leiningen (`brew install leiningen`) and Java.

Run `./install.sh` to build and install into `/usr/local/bin`.

## Usage

Just run `jsetags` in your project directory. Detailed options are available with `jsetags -h`.

## Future Work

- Improve regexes for detection.
- Port to ClojureScript to avoid Java dependency (though speed may be an issue).

## License

Copyright © 2013 Marcus Cavanaugh. <http://mcav.com/>

Distributed under the [Mozilla Public License 2.0][1].

[1]: <https://www.mozilla.org/MPL/2.0/>