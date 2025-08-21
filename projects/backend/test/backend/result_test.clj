(ns backend.result-test
  (:require
   [backend.result :as result]
   [clojure.test :refer [deftest is]]))

(deftest ok?
  (is (result/ok? (result/->Ok 1)))
  (is (not (result/ok? (result/->Err 1))))
  (is (not (result/ok? 1))))

(deftest err?
  (is (result/err? (result/->Err 1)))
  (is (not (result/err? (result/->Ok 1))))
  (is (not (result/err? 1))))

(deftest branch-ok
  (is (= 2
         (-> (result/->Ok 1)
             (result/branch-ok inc))))
  (is (= 1
         (-> (result/->Err 1)
             (result/branch-ok inc)
             :err))))

(deftest branch-err
  (is (= 2
         (-> (result/->Err 1)
             (result/branch-err inc))))
  (is (= 1
         (-> (result/->Ok 1)
             (result/branch-err inc)
             :val))))

(deftest try-result
  (is (= 4
         (-> (result/try-result (/ 8 2))
             :val)))
  (let [exception (ex-info "Icecream melted" {})]
    (is (= exception
           (-> (result/try-result (throw exception))
               :err)))))
