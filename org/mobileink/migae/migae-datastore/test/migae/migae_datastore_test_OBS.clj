(ns migae.migae-datastore-test
  (:refer-clojure :exclude [name hash])
  (:import [com.google.appengine.tools.development.testing
            LocalServiceTestHelper
            LocalServiceTestConfig
            LocalMemcacheServiceTestConfig
            LocalMemcacheServiceTestConfig$SizeUnit
            LocalMailServiceTestConfig
            LocalDatastoreServiceTestConfig
            LocalUserServiceTestConfig]
           [migae.migae_datastore EntityMap] ;; temp
           [com.google.apphosting.api ApiProxy])
  (:require [clojure.test :refer :all]
            [migae.migae-datastore :as ds]
;;            [migae.migae-datastore.entity :as ent]
            [migae.migae-datastore.key :as dskey]))
;            [ring-zombie.core :as zombie]))
;  (:require [migae.migae-datastore.EntityMap])
  ;; (:use clojure.test
  ;;       [migae.migae-datastore :as ds]))

;; (defn datastore [& {:keys [storage? store-delay-ms
;;                            max-txn-lifetime-ms max-query-lifetime-ms
;;                            backing-store-location]
;;                     :or {storage? false}}]
;;   (let [ldstc (LocalDatastoreServiceTestConfig.)]
;;     (.setNoStorage ldstc (not storage?))
;;     (when-not (nil? store-delay-ms)
;;       (.setStoreDelayMs ldstc store-delay-ms))
;;     (when-not (nil? max-txn-lifetime-ms)
;;       (.setMaxTxnLifetimeMs ldstc max-txn-lifetime-ms))
;;     (when-not (nil? max-query-lifetime-ms)
;;       (.setMaxQueryLifetimeMs ldstc max-query-lifetime-ms))
;;     (if-not (nil? backing-store-location)
;;         (.setBackingStoreLocation ldstc backing-store-location)
;;         (.setBackingStoreLocation ldstc "/dev/null"))
;;         ;; (.setBackingStoreLocation ldstc (if (= :windows (os-type))
;;         ;;                                     "NUL"
;;         ;;                                     "/dev/null")))
;;     ldstc))

;; (defn- make-local-services-fixture-fn [services hook-helper]
(defn- ds-fixture
  [test-fn]
  (let [;; environment (ApiProxy/getCurrentEnvironment)
        ;; delegate (ApiProxy/getDelegate)
        helper (LocalServiceTestHelper.
                (into-array LocalServiceTestConfig
                            [(LocalDatastoreServiceTestConfig.)]))]
    (do (.setUp helper)
        (ds/get-datastore-service) 
        (test-fn)
        (.tearDown helper))))
        ;; (ApiProxy/setEnvironmentForCurrentThread environment)
        ;; (ApiProxy/setDelegate delegate))))

;(use-fixtures :once (fn [test-fn] (ds/get-datastore-service) (test-fn)))
(use-fixtures :once ds-fixture)

(deftest ^:init ds-init
  (testing "DS init"
    (is (= com.google.appengine.api.datastore.DatastoreServiceImpl
           (class (ds/get-datastore-service))))
    (is (= com.google.appengine.api.datastore.DatastoreServiceImpl
           (class @ds/*datastore-service*)))))

(deftest dump-ent
  (testing "dump entity"
    (let [newEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:flda "A", :fldb "B", :fldc 99})]
          (ds/dump-entity newEntity))))

(deftest ^:fail make-entity-fail-1
  (testing "newEntity entity with inconsistent args"
    ;; (ds/get-datastore-service)
    (let [newEntity (ds/Entities
                              ^{:kind :K, :id 123 :name "foo"}
                              {})]
      ;; expected: violation of precondition: only one of :id and :name allowed
      (is true))))

(deftest ^:fail make-entity-fail-2
  (testing "newEntity entity with inconsistent args"
    ;; (ds/get-datastore-service)
    (let [newKey (ds/Keys {:kind :Employee, :name "asalieri"})
          newEntity (ds/Entities
                              ^{:key newKey :id 123}
                              {}) ;; empty
          fetched (ds/ds {:kind :Employee, :name "asalieri"})]
      (is (= ((meta newEntity) :kind)
             :Employee))
      (is (= ((meta newEntity) :key)
             ((meta fetched) :key)))
      (is (= (:entity (meta newEntity))
             (:entity (meta fetched)))))))

(deftest ^:entities make-entity
  (testing "make-entity"
    ;; (ds/get-datastore-service)
    (let [theKey (ds/Keys {:kind :Employee, :name "asalieri"})
          e1 (ds/Entities theKey)
          e2 (ds/Entities ^{:kind :Employee, :name "asalieri"}{})
          e3 (ds/Entities ^{:kind :Employee, :id 123}{})]
      (prn "new entity by key: " e1)
      (prn "new entity by emap with name: " e2)
      (prn "new entity by emap with id: " e3)
      )))

(deftest ^:entities make-entity-with-key
  (testing "make-entity-with-key"
    ;; (ds/get-datastore-service)
    (let [theKey (ds/Keys {:kind :Employee, :name "asalieri"})
          newEntity (ds/Entities theKey) ;; ^{:key theKey}{})
          fetched-by-key (ds/Entities theKey)
          fetched-by-map (ds/Entities ^{:kind :Employee, :name "asalieri"}{})]
      (is (not= newEntity fetched-by-map))
      (is (not= newEntity fetched-by-key))
      (is (not= fetched-by-key fetched-by-map))
      ;; clj map emulation
      (is (= (:key (meta newEntity)) (:key (meta fetched-by-key))))
      (is (= (:key (meta newEntity)) (:key (meta fetched-by-map))))
      (is (= (:key (meta fetched-by-key)) (:key (meta fetched-by-map))))
      (is (= (:entity (meta newEntity)) (:entity (meta fetched-by-key))))
      (is (= (:entity (meta newEntity)) (:entity (meta fetched-by-map))))
      (is (= (:entity (meta fetched-by-key)) (:entity (meta fetched-by-map))))
      (is (= (type newEntity) :migae.migae-datastore/Entity))
      (is (= (type fetched-by-key) :migae.migae-datastore/Entity))
      (is (= (type fetched-by-map) :migae.migae-datastore/Entity))
      (is (instance? clojure.lang.IFn newEntity))
      (is (instance? clojure.lang.IFn fetched-by-key))
      (is (instance? clojure.lang.IFn fetched-by-map))
      (is (= (:kind (meta newEntity)) :Employee))
      (is (= (:kind (meta fetched-by-key)) :Employee))
      (is (= (:kind (meta fetched-by-map)) :Employee))
      (is (nil? (:parent (meta newEntity))))
      (is (nil? (:parent (meta fetched-by-key))))
      (is (nil? (:parent (meta fetched-by-map))))
      (is (= (:key (meta newEntity)) (:key (meta fetched-by-key)))))))

(deftest ^:entities make-entity-with-kind
  (testing "newEntity entity with kind only"
    ;; (ds/get-datastore-service)
    (let [e1 (ds/Entities ^{:kind :Employee}{})
          e2 (ds/Entities ^{:kind :Employee}{})]
      (is (= (type e1) :migae.migae-datastore/Entity))
      (is (instance? clojure.lang.IFn e1))
      (is (= (:kind (meta e1)) :Employee))
      ;; name-less, id-less entities always assigned unique numeric id
      (is (not (nil? (:id (meta e1)))))
      (is (not (nil? (:id (meta e2)))))
      (is (not= (:id (meta e1)) (:id (meta e2))))
      (is (nil? (:name (meta e1)))))))

(deftest ^:entities make-entity-with-name
  (testing "newEntity entity with string id"
    ;; (ds/get-datastore-service)
    (let [e1 (ds/Entities ^{:kind :Employee, :name "jones"}{})
          e2 (ds/Entities ^{:kind :Employee, :name "smith"}{})
          newEntity (ds/Entities ^{:kind :Employee, :name "asalieri"}{})
          fetched (ds/Entities ^{:kind :Employee, :name "asalieri"}{})]
      (is (= (type newEntity) :migae.migae-datastore/Entity))
      (is (= (type fetched) :migae.migae-datastore/Entity))
      (is (instance? clojure.lang.IFn newEntity))
      (is (instance? clojure.lang.IFn fetched))
      (is (= (:kind (meta newEntity)) :Employee))
      (is (= (:kind (meta fetched)) :Employee))
      ;; new id-less entities always get id = 0?
      (is (= 0 (:id (meta newEntity))))
      (is (= 0 (:id (meta e1))))
      (is (= 0 (:id (meta e2))))
      (is (= (:name (meta newEntity)) "asalieri"))
      (is (= (:name (meta fetched)) "asalieri"))
      (is (nil? (:parent (meta newEntity))))
      (is (nil? (:parent (meta fetched))))
      (is (= ((meta newEntity) :key) ((meta fetched) :key)))
      (is (= (:entity (meta newEntity))
             (:entity (meta fetched)))))))

(deftest ^:entities make-entity-with-id
  (testing "new entity with numeric id"
    (let [newEntity (ds/Entities
                        ^{:kind :Employee, :id 123}
                        {}) ;; empty
          fetched (ds/Entities ^{:kind :Employee, :id 123}{})]
      (is (= (type newEntity) :migae.migae-datastore/Entity))
      (is (= (type fetched) :migae.migae-datastore/Entity))
      (is (instance? clojure.lang.IFn newEntity))
      (is (instance? clojure.lang.IFn fetched))
      (is (= (:kind (meta newEntity)) :Employee))
      (is (= (:kind (meta fetched)) :Employee))
      (is (= (:id (meta newEntity)) 123))
      (is (= (:id (meta fetched)) 123))
      (is (nil? (:name (meta newEntity))))
      (is (nil? (:name (meta fetched))))
      (is (nil? (:parent (meta newEntity))))
      (is (nil? (:parent (meta fetched))))
      (is (= ((meta newEntity) :key) ((meta fetched) :key)))
      (is (= (:entity (meta newEntity)) (:entity (meta fetched)))))))

(deftest ^:parent make-entity-no-parent
  (testing "no parent"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:fname "Antonio", :lname "Salieri"})]
      (is (nil? (:parent (meta theEntity)))))))

(deftest ^:parent make-entity-with-parent-key
  (testing "entity with parent"
    (let [theParent (ds/Entities ^{:kind :Person, :name "parent"}{})
          theChild  (ds/Entities ^{:kind :Person,
                                      :name "child",
                                      :parent theParent}{})]
      (is (= (type theParent) :migae.migae-datastore/Entity))
      (is (= (type theChild) :migae.migae-datastore/Entity))
      (is (instance? clojure.lang.IFn theParent))
      (is (instance? clojure.lang.IFn theChild))
      (is (= (:kind (meta theParent)) :Person))
      (is (= (:kind (meta theChild)) :Person))
      ;; new id-less entities always get id = 0?
      (is (= 0 (:id (meta theParent))))
      (is (= 0 (:id (meta theChild))))
      (is (= (:name (meta theParent)) "parent"))
      (is (= (:name (meta theChild)) "child"))
      (is (nil? (:parent (meta theParent))))
      (is (= (:parent (meta theChild)) (:key (meta theParent)))))))

(deftest ^:parent make-entity-with-parent-keymap
  (testing "entity with parent"
    (let [theParent (ds/Entities ^{:kind :Person, :name "parent"}{})
          theChild  (ds/Entities ^{:kind :Person,
                                      :name "child",
                                      :parent ^{:kind :Person
                                                :name "parent"}{}
                                      }{})]
      (is (= (type theParent) :migae.migae-datastore/Entity))
      (is (= (type theChild) :migae.migae-datastore/Entity))
      (is (instance? clojure.lang.IFn theParent))
      (is (instance? clojure.lang.IFn theChild))
      (is (= (:kind (meta theParent)) :Person))
      (is (= (:kind (meta theChild)) :Person))
      ;; new id-less entities always get id = 0?
      (is (= 0 (:id (meta theParent))))
      (is (= 0 (:id (meta theChild))))
      (is (= (:name (meta theParent)) "parent"))
      (is (= (:name (meta theChild)) "child"))
      (is (nil? (:parent (meta theParent))))
      (is (= (:parent (meta theChild)) (:key (meta theParent)))))))

(deftest ^:parent make-entity-with-grandparent-keymap
  (testing "three-level entity; ancestor path keys need not be
  instantiated as entities?"
    (let [;theParent (ds/Entities ^{:kind :Person, :name "parent"}{})
          theChild  (ds/Entities
                     ^{:kind :Person, :name "child",
                       :parent ^{:kind :Person :name "parent"
                                 :parent ^{:kind :Person :name "gramps"}{}
                                 }{}
                       }{})]
;      (is (= (type theParent) :migae.migae-datastore/Entity))
;      (is (= (type theParent) :migae.migae-datastore/Entity))
      (is (= (type theChild) :migae.migae-datastore/Entity))
;      (is (instance? clojure.lang.IFn theParent))
      (is (instance? clojure.lang.IFn theChild))
;      (is (= (:kind (meta theParent)) :Person))
      (is (= (:kind (meta theChild)) :Person))
      ;; new id-less entities always get id = 0?
;      (is (= 0 (:id (meta theParent))))
      (is (= 0 (:id (meta theChild))))
;      (is (= (:name (meta theParent)) "parent"))
      (is (= (:name (meta theChild)) "child"))
      (prn "key: " (:key (meta theChild)))
      (prn "parent key: " (:parent (meta theChild)))
      ;; TODO: better syntax for getting ancestor keys
      (prn "gramps key: " (:parent (meta (ds/ds (:parent (meta theChild))))))
;      (is (nil? (:parent (meta theParent))))
;      (is (= (:parent (meta theChild)) (:key (meta theParent)))))))
      )))

(deftest ^:fields make-entity-with-fields
  (testing "theEntity entity with string id"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:fname "Antonio", :lname "Salieri"})
          fetched (ds/ds {:kind :Employee, :name "asalieri"}) ;; keymap
          ;; fetched-flds (fetched :theEntity)]
          ]
      ;; (prn (str "new key: " ((ds/meta? theEntity) :key)))
      ;; (prn (str "new entity: " theEntity))
      ;; (prn (str "new entitymap: " (doall fetched)))
      (is (not (= theEntity
                  fetched)))
      (is (= {:fname "Antonio", :lname "Salieri"}
             (fetched)))
      (is (= ((meta theEntity) :key)
             ((meta fetched) :key)))
      (is (= ((meta theEntity) :kind)
             ((meta fetched) :kind)))
      ;; (is (= (:theEntity theEntity)
      ;;        (:theEntity fetched))))))
      )))

(deftest ^:temp field-access
  (testing "field access by key"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:fname "Antonio", :lname "Salieri"})]
      ;; (prn (str "new key: " ((ds/meta? theEntity) :key)))
      ;; (prn (str "new entity: " theEntity))
      ;; (prn (str "new entitymap: " (doall fetched)))

      (is (= (theEntity :fname)
             "Antonio")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest ^:map map-test-1
  (testing "test map emulation"
    (let [foo "foo" ;; (augment-contents {:a :b})
          bar (ds/EntityMap. {:kind :Person}) ;; temp
          theEntity (ds/EntityMap. ^{:kind :Employee, :name "asalieri"} ;; temp
                                   {:fname "Antonio", :lname "Salieri"})]
      (prn theEntity)
      (prn (meta theEntity))
      (prn (format "map? %s" (map? theEntity)))
      (prn (str ":kindkind " (:kindkind theEntity)))
      ;; (prn (str "new entity: " theEntity))
      ;; (prn (str "new entitymap: " (doall fetched)))

      (is (= (map? theEntity) true))
      ;; predefined default fields:
      (is (= (:kindkind theEntity) :kind))
      (is (= (:status theEntity) :default))
      ;; app fields:
      (is (= (:kind (meta theEntity)) ":Employee"))
      (is (= (:name (meta theEntity)) "asalieri"))
      (is (= (:fname theEntity) "Antonio"))
      (is (= (:lname theEntity) "Salieri"))
      )))

(deftest ^:maps map-test-2
  (testing "test map emulation"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:fname "Antonio", :lname "Salieri"})]
      (prn (format "map? %s" (map? theEntity)))
      (prn (str ":kindkind " (theEntity :kindkind)))
      ;; (prn (str "new entity: " theEntity))
      ;; (prn (str "new entitymap: " (doall fetched)))

      (is (= (map? theEntity) true))
      ;; predefined default fields:
      (is (= (theEntity :kindkind) :kind))
      (is (= (theEntity :status) :default))
      ;; app fields:
      (is (= (theEntity :name) "asalieri"))
      (is (= (theEntity :fname) "Antonio"))
      (is (= (theEntity :lname) "Salieri"))
      )))

;; TODO:  make ds/Keys work with std EntityMap (i.e. keymap metadata)
;; (deftest ^:keys make-key
;;   (testing "make key"
;;     (let [theKey (ds/Keys {:kind :Employee, :name "asalieri"})]
;;       ;; (is (= (:key (meta theEntity))
;;       ;;        theKey))
;;       ;; (is (= ((meta theEntity) :key)
;;       ;;        theKey))

;;       )))

;; (deftest ^:keys make-ent-and-key
;;   (testing "get entity kind"
;;     (let [theEntity (ds/Entities
;;                         ^{:kind :Employee, :name "asalieri"}
;;                         {:fname "Antonio", :lname "Salieri"})
;;           theKey (ds/Keys {:kind :Employee, :name "asalieri"})]
;;       (is (= (:key (meta theEntity))
;;              theKey))
;;       (is (= ((meta theEntity) :key)
;;              theKey))
;;       )))

(deftest ^:meta metadata-kind
  (testing "get entity kind"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:fname "Antonio", :lname "Salieri"})]
      (is (= ((meta theEntity) :kind)
             :Employee)))))

(deftest ^:meta metadata-id
  (testing "get entity id"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :id 123}
                        {:fname "Antonio", :lname "Salieri"})]
      (is (= ((meta theEntity) :id)
             123)))))

;; (deftest ^:meta metadata-id-fail
;;   (testing "get entity id - expected failure"
;;     (let [theEntity (ds/Entities
;;                         ^{:kind :Employee, :name "asalieri"}
;;                         {:fname "Antonio", :lname "Salieri"})]
;;       (is (= ((ds/meta? theEntity) :id)
;;              "asalieri")))))

(deftest ^:meta metadata-name
  (testing "get entity name"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:fname "Antonio", :lname "Salieri"})]
      (is (= ((meta theEntity) :name)
             "asalieri")))))

(deftest ^:meta metadata-keynamespace
  (testing "get entity kind"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:fname "Antonio", :lname "Salieri"})]
      (is (nil? ((meta theEntity) :keynamespace))))))

(deftest ^:meta metadata-keystring
  (testing "get entity kind"
    (let [theEntity (ds/Entities
                        ^{:kind :Employee, :name "asalieri"}
                        {:fname "Antonio", :lname "Salieri"})]
      (is (not (nil? (:keystring (meta theEntity))))))))

;; ################################################################
;; ################################################################
(deftest ^:emap entity-map-1
  (testing "entitymap deftype"
    ;;(ds/new-entitymap ...
    (let [em ^{:_kind :Employee,
               :_name "asalieri"},
          {:fname "Antonio",
           :lname "Salieri"}]
 ;      (println em)
      ;; (is (= (type em)
      ;;        migae.migae_datastore.EntityMap))
      (is (= (:fname em)
             "Antonio"))
      (is (= (:fname (merge em {:fname "Wolfie"})
             "Wolfie")))
      )))

(deftest ^:emap entity-map-save
  (testing "entitymap deftype saving"
    (let [em ^{:_kind :Employee,
               :_name "asalieri"} {:fname "Antonio",
                                   :lname "Salieri"}]
      (let [e (ds/persist em)]
        ;; (is (= (type e)
        ;;        com.google.appengine.api.datastore.Key))
        (is (= (dskey/id (:_key (meta e)))
               0))
        (is (= (dskey/name (:_key (meta e)))
               "asalieri")))
      )))

(deftest ^:emap entity-map-update
  (testing "entitymap deftype update"
    (let [em ^{:_kind :Employee,
               :_name "asalieri"},
          {:fname "Antonio",
           :lname "Salieri"}]
      (let [e (ds/persist em)
            f (ds/fetch {:_kind :Employee :_name "asalieri"})]
        (println "meta: " (meta f))
        (println f)
        (is (= (:fname f)
               "Antonio"))
        (is (= (type (:fname f))
               java.lang.String))
        (is (= (:lname f)
               "Salieri"))
      ))))

