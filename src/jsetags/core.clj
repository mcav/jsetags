(ns jsetags.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :refer [cli]])
  (:gen-class))

;; Main configuration options; edit these if you like.
(def js-regexes [#"^(\s*?function\s*)(\S+?)\s*\("
                 #"^(\s*?\b)(\S+?):\s*function"
                 #"^(\s*?\S+\.)(\S+)\s*=\s*function"])
(def file-extensions [".js" ".jsm"])
(def ^:dynamic *excluded-directories*
  #{".git" "node_modules" "build" "ext" "target" "output"})

;; Utilities for output and debugging:
(def log-agent (agent *err*))
(defn log [& more]
  (send log-agent #(binding [*out* %]
                     (apply println more)
                     %)))
(def files-done (atom 0))
(def files-total (atom 0))
(def tags-total (atom 0))
(def ^:dynamic *cwd* (.getCanonicalPath (io/file ".")))

(defn symlink? [f]
  (not= (.getAbsolutePath f) (.getCanonicalPath f)))

(defn filtered-file-seq
  "A tree seq on files, where you can filter the direcories to traverse."
  [dir exclude?]
  (tree-seq
   (fn [^java.io.File f] (.isDirectory f))
   (fn [^java.io.File d] (when-not (or (exclude? (.getName d))
                                       (symlink? d))
                           (seq (filter #(.exists %) (.listFiles d)))))
   dir))

(defn list-files
  "Return a seq of all files in cwd to be examined."
  [cwd]
  (->> (filtered-file-seq (io/file cwd) *excluded-directories*)
       (filter (memfn isFile)) ; exclude dirs
       (filter (fn [^java.io.File f]
                 (some #(.endsWith (.getName f) %) file-extensions)))))

(defn tags-in-file
  "Return [lineno, offset, tag, context] for each tag found in f."
  [f]
  (loop [lines (line-seq (io/reader f)), tags [], lineno 1, offset 0]
    (if (seq lines)
      (let [line (first lines)]
        (if-let [[context prefix tag] (some #(re-find % line)
                                            js-regexes)]
          (do
            (swap! tags-total inc)
            (recur (rest lines)
                   (conj tags [lineno offset tag context])
                     (inc lineno)
                     (+ offset (inc (count line)))))
          (recur (rest lines) tags (inc lineno) (+ offset (count line)))))
      tags)))

(defn relative-path
  "Return a path for f relative to cwd, otherwise return its full path."
  [f cwd]
  (let [f (.getAbsolutePath (io/file f))
        cwd (.getAbsolutePath (io/file cwd))]
    (if (.startsWith f cwd)
      (.substring f (inc (count cwd)))
      f)))

(defn short-relative-path [f cwd]
  (let [s (relative-path f cwd)]
    (if (<= (count s) 60)
      s
      (str (subs s 0 60)))))

; format via <https://en.wikipedia.org/wiki/Ctags#Etags>
(defn format-tags
  "Return a string in Emacs TAGS format, given file f and an array of
  [lineno, offset, tag, context] as returned from tags-in-file."
  [^java.io.File f, tags]
  (swap! files-done inc)
  (let [width (inc (int (/ (Math/log (max @files-total 1)) (Math/log 10))))]
    (log (format (str "[%" width "d/%" width "d] %s (%d tags)")
                 @files-done @files-total (short-relative-path f *cwd*)
                 (count tags))))
  (let [tag-data (apply str (for [[lineno offset tag context] tags]
                              (str context "\u007f" tag "\u0001"
                                   lineno "," offset \newline)))]
    (str "\u000c" \newline
         (.getPath f) "," (count tag-data) \newline
         tag-data)))

(defn now [] (System/currentTimeMillis))
(defn -main [& args]
  (let [[opts args banner]
        (cli args
             (str
              "jsetags: Generates an Emacs TAGS file from JavaScript sources."
              "\n\nUsage: jsetags [-o outfile] dir1 [dir2...]"
              "\n\nIf no files are listed, scans the current directory recursively.\n"
              "Alternately, you can pipe in a list of filenames (i.e. from `find`)."
              "\n\nBy default, jsetags excludes the following patterns:\n  "
              (str/join ", " *excluded-directories*))
             ["-o" "--output" "Output file (use \"-\" for stdout)" :default "TAGS"]
             ["-e" "--exclude" "Exclude directories named DIR (one name each, can be specified multiple times)"
              :assoc-fn (fn [previous key val]
                          (assoc previous key
                                 (if-let [oldval (get previous key)]
                                   (merge oldval val)
                                   (hash-set val))))
              :default "(see above)"]
             ["-h" "--help" "Print this help file." :flag true :default false])
        outfile (if (= "-" (:output opts))
                  *out*
                  (io/writer (io/file (:output opts))))
        files (binding [*excluded-directories* (into #{} (:exclude opts))]
                (cond
                 (pos? (count args)) (apply concat (map list-files args))
                 (pos? (.available System/in)) (apply concat
                                                      (map list-files
                                                           (str/split-lines (slurp System/in))))
                 :else (list-files *cwd*)))
        start-time (now)]
    (when (:help opts)
      (prn opts args)
      (println banner)
      (System/exit 0))
    (try
      (reset! files-done 0)
      (reset! files-total (count files))
      (reset! tags-total 0)
      (.write outfile (apply str (pmap #(format-tags % (tags-in-file %))
                                       files)))
      (.close outfile)
      (log (format "Done. %d files and %d tags found in %.3f seconds."
                   @files-total @tags-total
                   (/ (float (- (now) start-time)) 1000)))
      (finally
        (shutdown-agents)))))
