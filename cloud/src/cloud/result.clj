(ns cloud.result)

(defrecord Ok [val])
(defrecord Err [err])

(def ok? (partial instance? Ok))
(def err? (partial instance? Err))

(defn branch-ok [result fn]
  (if (ok? result)
    (fn (:val result))
    result))

(defn branch-err [result fn]
  (if (err? result)
    (fn (:err result))
    result))

(defmacro try-result [& body]
  `(try
     (->Ok (do ~@body))
     (catch Exception e#
       (->Err e#))))
