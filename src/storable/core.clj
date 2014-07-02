(ns storable.core
  "Transform maps into format suitable for storage in Datomic."
  (:require [clojure.set :refer [rename-keys]]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [datomic.api :as d]
            [clojure.edn :as edn]))

(def ^:dynamic *uri* (or (env :datomic-uri)
                         "datomic:mem://storable"))

(defn load-schema
  ([] (load-schema (io/resource "schema.edn")))
  ([filename] (clojure.edn/read-string {:readers *data-readers*} (slurp filename))))

(defn setup
  ([] (setup *uri* (io/resource "schema.edn")))
  ([uri schema] 
     (d/create-database uri)
     (if-let [conn (d/connect uri)]
       (doto conn
         (d/transact (load-schema schema))))))

(defn transform-keys
  "Rename all keys in m by applying (func key).

(transform-keys {:key :value}
                #(keyword (apply str (reverse (name %)))))
;; => {:yek :value}"
  [m func]
  (rename-keys m
               (->> m
                    keys
                    (map (juxt identity func))
                    (into {}))))

(defn add-prefix
  "Rename keys in m by adding namespace prefix.

(add-prefix :pre {:key :val})
;; => {:pre/key :val} "
  [prefix m]
  (transform-keys m (fn [k] (keyword (name prefix) (name k)))))

(defn remove-prefix
  "Rename keys in m by removing namespace prefix.

(remove-prefix {:pre/key :val})
;; => {:key :val}"
  [m]
  (transform-keys m (comp keyword name)))

(defn ->datomic [prefix]
  (partial add-prefix prefix))

(defn ->clj [entity]
  (->> entity
       (into {})
       remove-prefix))

(defprotocol Storable
  (persist-tx [this] "Return array of Datomic transactions required to persist this."))

(defn persist!
  "Persist storable in Datomic storage."
  [conn storable]
  (d/transact conn (persist-tx storable)))

(defn read-resource [filename]
  (-> filename
      slurp
      java.io.StringReader.
      java.io.PushbackReader.
      edn/read))
