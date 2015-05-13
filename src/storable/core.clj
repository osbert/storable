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
     (when-let [conn (d/connect uri)]
       @(d/transact conn (load-schema schema))
       conn)))

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

(defn ->clj
  "Given an entity map, convert it into a Clojure map."
  [entity]
  (->> entity
       (into {})
       remove-prefix))

(defn eid->entity
  "Given an entity ID, turn it into a Datomic entity map."
  [db eid]
  (->> eid
       (d/entity db)
       d/touch))

(defn eid->clj
  "Given an entity ID, return a Clojure map representing it's datoms."
  [db eid]
  (->> eid
       (eid->entity db)
       ->clj))

(defprotocol Storable
  (persist-tx [this] "Return array of Datomic transactions required to persist this.")
  (retract-tx [this] "Return array of Datomic transactions required to retract this"))

(defn annotate-tx
  "I can never remember how to annotate a tx."
  [annotations]
  (if annotations
    (merge annotations {:db/id (d/tempid :db.part/tx)})))

(defn persist!
  "Persist storable in Datomic storage."
  [conn storable & tx-annotations]
  (-> storable
      persist-tx
      (concat (annotate-tx tx-annotations))
      (->> (d/transact conn))))

(defn retract!
  "Retract storable from Datomic storage."
  [conn storable & tx-annotations]
  (-> storable
      retract-tx
      (concat (annotate-tx tx-annotations)) 
      (->> (d/transact conn))))

(defn read-resource [filename]
  (-> filename
      slurp
      java.io.StringReader.
      java.io.PushbackReader.
      edn/read))

(defn valid-attributes
  "Return the set of valid attributes out of an input set of keys."
  [db attrs]
  (->> (d/q '[:find ?a :in $ [?a ...] :where
               [?e :db/ident ?a]]
             db
             attrs)
       (map first)
       (into #{})))

(defn find-storable
  "Return full entity given id-key and id."
  [db id-key id]
  (if-let [eid (d/entity db [id-key id])]
    (d/touch eid)))

(defn find-all
  "Return all entities given an id-key"
  [db id-key]
  (->> (d/q '[:find ?e :in $ ?attr :where [?e ?attr]] db id-key)
       (map first)
       (map (partial d/entity db))
       (map d/touch)))
