(ns clj-sked.core-test
  (:use clojure.test
        clj-sked.core))

(deftest creating-scheduler-test
  (testing "Test creating an empty scheduler"
    (let [sked (create-scheduler)]
      (is (= {}  @(:next-fire-map sked)))
      (is (= {} @(:name-map sked)))
      (is (= nil @(:background-future sked))))))

(deftest insert-job-test
  (testing "Test creating an empty scheduler"
    (let [sked (create-scheduler)
          testjob {:fire-time (+ 5000 (System/currentTimeMillis)),
                   :name "testjob" }
          dmy   (insert-job sked testjob)]
      (is (= 1 (count @(:next-fire-map sked))))
      (is (= 1 (count @(:name-map sked))))
      (is (= (future? @(:background-future sked))))
;;      (is (= 0 (count @(:next-fire-map sked))))
;;      (is (= 0 (count @(:name-map sked))))
;;      (is (= (future-done? @(:background-future sked))))
      )))
