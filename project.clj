(defproject jsetags "1"
  :description "Generate Emacs TAGS files for JavaScript."
  :url "http://github.com/mcav/jsetags"
  :license {:name "Mozilla Public License 2.0"
            :url "https://www.mozilla.org/MPL/2.0/"}
  :main jsetags.core
  :aot :all
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.4"]])
