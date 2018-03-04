(ns miningit.server
  (:require [immutant.web :as web]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [ring.util.response]
            [ring.middleware.params]
            [hiccup.core :as hiccup])
  (:use [clojure.java.shell :only [sh]])
  (:gen-class))

(def config (atom {:path "/home/git"
                   :clone-path "git@localhost"
                   :trusted true}))

(defn safe-slurp [source]
  "Reads source. On fail returns nil"
  (try
    (slurp source)
    (catch Exception e
      nil)))

(defn read-config []
  (some->> (safe-slurp "config.edn")
          (edn/read-string)
          (reset! config)))

(defn save-config [new-config]
  (reset! config new-config)
  (spit "config.edn" new-config))

(defn get-repositories-list []
  (for [name (seq (.list (io/file (:path @config))))
        :let [child (io/file (:path @config) name)]
        :when (and
               (.isDirectory child)
               (re-find #".(.git)" (.getName child)))]
    name))

(defn get-commits [repo steps]
  (:out (sh "git" "log" (str "-" steps) "--pretty=%ci\t%cN\t%B(%H) %%" :dir (io/file (:path @config) repo))))

(defn get-last-commit-message [repo]
  (some-> (get-commits repo 1)
          (str/split #" %")
          (first)
          (str/split #"\t")
          (last)))

(defn read-strings [strings]
  (map #(str/split % #"\t") strings))

(defn get-commits-history [repo depth]
  (some-> (get-commits repo depth)
          (str/split #" %")
          (read-strings)))

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
            [:link {:href "/style.css" :rel "stylesheet" :type "text/css"}]
            [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no"}]
            [:title (str "MininGit - " title)]]
           [:body
            (nav-bar {:link "/" :title "List"}
                     {:link "/new" :title "Create"}
                     (when (:trusted @config) {:link "/config" :title "Configure"})
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

(defn commit-list [repo]
  [:table.table
   [:thead
    [:tr
     [:th "Date"]
     [:th "Author"]
     [:th "Commit"]]]
   [:tbody
    (for [commit (get-commits-history repo 10)]
      [:tr
       [:td (first commit)]
       [:td (second commit)]
       [:td (last commit)]])]])

(defn repository-page [repo]
  (common-page-template
   (str "Information about " repo)
   [:h1 repo]
   [:h2 "Latest commits"]
   [:p "For cloning repository execute:"
   [:input {:type "text"
            :readonly true
            :value (str "git clone " (:clone-path @config) ":" (:path @config) "/" repo)}]]
   (commit-list repo)
   (when (:trusted @config)
     (list
      [:h2 "Maintaince area"]
      [:p "To remove repository press a button: "
       [:a.btn.btn-sm.btn-c {:href (str "/remove/" repo)
                             :onclick "return confirm('This action cannot be undone! Are you sure?');"}
        "Remove"]]))))

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

(defn create-repo-page [req]
  (common-page-template
   "Create new repository"
   [:h1 "Creating new repository"]
   (when (get (:params req) "err")
     [:div.msg
      [:strong "Error! "]
      "Cannot create repository \""
      [:b 
       (get (:params req) "err")]
      "\". Maybe it already exists"])
   [:br]
   [:form {:method "post"
           :action "/new"}
    [:p "Enter new repository name(allowed latin characters and minus as sepparator)." ]
    [:p [:b "NB"] " \".git\" postfix will be added automaticly"]
    [:input {:type "text"
             :name "name"
             :pattern "^[A-Za-z0-9\\-]+$"}]
    [:button.btn.btn-sm.btn-a "Create"]]))

(defn create-repo-action [req]
  (let [name (str (get (:params req) "name") ".git")
        directory (io/file (:path @config) name)]
    (if (and (.mkdir directory) (= (:exit (sh "git" "init" "--bare" :dir directory))))
        (redirect "/")
        (redirect (str "/new?err=" name)))))

(defn config-page []
  (if (:trusted @config)
  (common-page-template
   "configuration page"
   [:h1 "Configuration"]
   [:form {:method "post"
           :action "/config"}
    [:p "Path where repositories are stored: "
     [:input {:type "text"
              :name "path"
              :value (:path @config)}]]
    [:p "Git path prefix: "
     [:input {:type "text"
              :name "clone-path"
              :value (:clone-path @config)}]]
    [:p
     [:input {:type "checkbox"
              :name "trusted"
              :checked true}]
     " Running in trusted mode (possible to remove repositories and change settings)"]
    [:p [:b "NB! "] "Trusted flag can be enabled  only by hands in configuration file"]
    [:button.btn.btn-sm.btn-a "Update config"]])
  {:status 405 :body "Not in trusted mode"}))

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

(defn about-page []
  (common-page-template
   "About this software"
   [:h1 "About this software"]
   [:p "MininGit is minimalistic GIT managment tool for local networks"]
   [:b "Copyright &copy; 2018 Alexander Sharikhin(aka nihirash)"]
   [:p "This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version."]
   [:p "This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details."]
   [:p "You should have received a copy of the GNU General Public License
    along with this program.  If not, see "
    [:a {:href "https://www.gnu.org/licenses/"} "https://www.gnu.org/licenses/" ]]))

(compojure/defroutes routes
  (compojure/GET "/" [] (index-page))
  (compojure/GET "/repo/:name" [name] (repository-page name))
  (compojure/GET "/remove/:name" [name] (remove-action name))
  (compojure/GET "/new" [:as req] (create-repo-page req))
  (compojure/POST "/new" [:as req] (create-repo-action req))
  (compojure/GET "/config" [] (config-page))
  (compojure/POST "/config" [:as req] (config-update-action req))
  (compojure/GET "/about" [] (about-page))
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
