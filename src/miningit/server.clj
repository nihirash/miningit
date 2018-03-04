(ns miningit.server
  (:require [immutant.web :as web]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.util.response]
            [ring.middleware.params]
            [hiccup.core :as hiccup])
  (:use [clojure.java.shell :only [sh]])
  (:gen-class))

(def path "/home/git")

(defn get-repositories-list []
  (->>
   (for [name (seq (.list (io/file path)))
         :let [child (io/file path name)]
         :when (.isDirectory child)]
     name)
   (sort)))

(defn get-last-commit-message [repo]
  (try 
    (:out (sh "git" "log" "-1" "--pretty=%B" :dir (io/file path repo)))
    (catch Exception e "Calling git command issue")))

(defn nav-bar [& items]
  (list [:nav.nav
         [:div.container
          [:a.pagename.current "MininGit"]
          (for [item items]
            [:a {:href (:link item)} (:title item)])]]
        [:button.btn-close.btn.btn-sm "x"]))

(defn common-page-template [title & childs]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"} 
   :body (hiccup/html
          [:html
           [:head
            [:link {:href "style.css" :rel "stylesheet" :type "text/css"}]
            [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no"}]
            [:title (str "MininGit - " title)]]
           [:body
            (nav-bar {:link "/" :title "List"}
                     {:link "/new" :title "Create"}
                     {:link "/about" :title "About"})
            [:div.container
             childs]]])})

(defn repository-element [repo]
  [:tr
   [:td [:a {:href (str "/repo/" repo)} repo]]
   [:td [:a {:href (str "/repo/" repo)} (get-last-commit-message repo)]]])

(defn repository-list [repos]
  [:table.table
   [:thead
    [:tr
     [:th "Repository"]
     [:th "Last commit"]]]
   [:tbody
    (map repository-element repos)]])

(defn index-page []
  (common-page-template
   "Current repositories"
   [:h1 "Your repositories list"]
   [:p "Click on row to open repository information and managment"]
   (repository-list
    (get-repositories-list))))

(defn with-headers [handler headers]
  (fn [request]
    (some-> (handler request)
            (update :headers merge headers))))

(compojure/defroutes routes
  (compojure/GET "/" [] (index-page))
  (compojure/GET "/repo/:name" [name] (str "Info about " name))
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
      (println "Starting web server on port" port-str)
      (web/run #'app {:port (Integer/parseInt port-str)})))

(comment
  (def server (-main "--port" "8000"))
  (web/stop server))
