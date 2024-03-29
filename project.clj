(defproject miningit "0.1.0"
  :description "Minimalist git managment tool"
  :url "https://github.com/nihirash/miningit"
   :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url "https://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring "1.6.3"]
                 [org.immutant/web "2.1.10"]
                 [compojure "1.6.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [hiccup "1.0.5"]]
  :main miningit.server
  :profiles {:uberjar
             {:aot [miningit.server]
             :uberjar-name "miningit.jar"}})
