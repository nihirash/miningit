(ns miningit.server
  (:require [immutant.web :as web]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.util.response]
            [ring.middleware.params]
            [miningit.view :as view])
  (:use [clojure.java.shell :only [sh]]
        [miningit.config])
  (:gen-class))

(defn redirect [to]
  {:status 302
   :headers {"Location" to}})

(defn remove-action [repo]
  (let [directory (str (:path @config) "/" repo)
        result (:exit (sh "rm" "-rf" directory))]
    (if (= 0 result)
      (redirect "/")
      {:status 500
       :body "Internal error"})))

(defn create-repo-action [req]
  (let [name (str (get (:params req) "name") ".git")
        directory (io/file (:path @config) name)]
    (if (and (.mkdir directory) (= (:exit (sh "git" "init" "--bare" :dir directory))))
        (redirect "/")
        (redirect (str "/new?err=" name)))))

(defn config-update-action [req]
  (if (:trusted @config)
    (let [params (:params req)
          path (get params "path")
          clone-path (get params "clone-path")
          trusted (get params "trusted")]
      (save-config {:path path :clone-path clone-path :trusted trusted})
      (redirect (if trusted "/config" "/")))
    {:status 405 :body "Not in trusted mode"}))

(defn with-headers [handler headers] 
  (fn [request]
    (some-> (handler request)
            (update :headers merge headers))))

(compojure/defroutes routes
  (compojure/GET "/" [] (view/index-page))
  (compojure/GET "/repo/:name" [name] (view/repository-page name))
  (compojure/GET "/remove/:name" [name] (remove-action name))
  (compojure/GET "/new" [:as req] (view/create-repo-page req))
  (compojure/POST "/new" [:as req] (create-repo-action req))
  (compojure/GET "/config" [] (view/config-page))
  (compojure/POST "/config" [:as req] (config-update-action req))
  (compojure/GET "/about" [] (view/about-page))
  (route/resources "/")
  (route/not-found {:body "404"}))

(def app
  (compojure/routes 
   (-> routes
       (ring.middleware.params/wrap-params)
       (with-headers {"Cache-Control" "no-cache"
                      "Expires" "-1"}))))

(defn -main [& args]
    (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
      (read-config)
      (println "Starting web server on port" port-str)
      (web/run #'app {:port (Integer/parseInt port-str)})))

(comment
  (def server (-main "--port" "8000"))
  (web/stop server))
