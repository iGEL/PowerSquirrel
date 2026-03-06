(ns hashpr
  (:require
   [clojure.pprint]))

(def !system-err
  "Kaocha captures stdout and stderr, but for #pp etc we don't want that

  So instead of using kaocha's virtual streams, let's grab the original file descriptor"
  (delay
    (java.io.PrintStream. (java.io.FileOutputStream. java.io.FileDescriptor/err))))

;; Define tag functions. See data_readers.clj
;; Also see projects/hashpr/README.md

(defn hashpr [form]
  (let [result-sym (gensym "result")]
    `(let [~result-sym ~form]
       (.println @!system-err
                 (binding [clojure.core/*print-length* 20]
                   (str (pr-str '~form) " => "
                        (pr-str ~result-sym))))
       ~result-sym)))

(defn hashpr! [form]
  (let [result-sym (gensym "result")]
    `(let [~result-sym ~form]
       (.println @!system-err
                 (str (pr-str '~form) " => "
                      (pr-str ~result-sym)))
       ~result-sym)))

(defn hashpp [form]
  (let [result-sym (gensym "result")]
    `(let [~result-sym ~form]
       (.println @!system-err
                 (binding [clojure.core/*print-length* 20]
                   (str "***\n" ;; start with newline for indentation
                        (with-out-str (clojure.pprint/pprint '~form))
                        "=>\n"
                        (with-out-str (clojure.pprint/pprint ~result-sym)))))
       ~result-sym)))

(defn hashpp! [form]
  (let [result-sym (gensym "result")]
    `(let [~result-sym ~form]
       (.println @!system-err
                 (str "***\n" ;; start with newline for indentation
                      (with-out-str (clojure.pprint/pprint '~form))
                      "=>\n"
                      (with-out-str (clojure.pprint/pprint ~result-sym))))
       ~result-sym)))

(defn hashpc [form]
  (let [result-sym (gensym "result")]
    `(let [~result-sym ~form]
       (.println @!system-err
                 (binding [clojure.core/*print-length* 20]
                   (str (pr-str '~form) " => "
                        (pr-str ~result-sym))))
       ~result-sym)))

(defn hashp-tap [form]
  `(let [result-sym# ~form]
     (tap> result-sym#)
     result-sym#))
