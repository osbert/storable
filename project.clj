(defproject storable "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [environ "0.4.0"]
                 [com.datomic/datomic-pro "0.9.4755" :exclusions [org.apache.httpcomponents/httpclient]]]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg  :env/MY_DATOMIC_USERNAME]
                                   :password [:gpg :env/MY_DATOMIC_PASSWORD]}}
  :plugins [[lein-environ "0.5.0"]])
