(defproject storable "0.2.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [environ "1.0.0"]
                 [com.datomic/datomic-pro "0.9.5130" :exclusions [org.apache.httpcomponents/httpclient]]]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg  :env/MY_DATOMIC_USERNAME]
                                   :password [:gpg :env/MY_DATOMIC_PASSWORD]}}
  :plugins [[lein-environ "1.0.0"]])

