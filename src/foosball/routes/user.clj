(ns foosball.routes.user
  (:use [compojure.core :only [GET POST]]
        [hiccup.page :only [html5]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout   :as layout]
            [foosball.models.domains :as d]
            [foosball.util           :as util]
            [foosball.uri-misc       :as uri-misc]
            [foosball.auth           :as auth]
            [ring.util.response      :as response]
            [cemerick.friend         :as friend]
            [compojure.core          :as compojure]))

(defn- assign-player-page [{:keys [db config-options]}]
  (let [current-auth (auth/current-auth)
        {:keys [playerid playername]} current-auth]
    (if (or playername (nil? current-auth))
      (response/redirect "/")
      (let [unclaimed-players (d/get-players-without-openids db)]
        (layout/common
         config-options
         :title "Assign Player"
         :content (html5
                   [:div
                    [:h1 "Assign a player to your login"]
                    (when (and (nil? playerid) (not (empty? unclaimed-players)))
                      [:div.panel.panel-success
                       [:div.panel-heading
                        [:h3 "Do you have an existing player?"]]
                       [:div.panel-body
                        [:form.form-inline {:action "/user/claim-player" :method "POST"}
                         [:div.form-group.col-lg-4
                          [:select.input-lg.form-control {:id "playerid" :name "playerid"}
                           [:option {:value "nil" :disabled "disabled" :selected "selected"} "Unclaimed players"]
                           (for [{:keys [id name]} unclaimed-players]
                             [:option {:value id} name])]]
                         [:div.form-group.col-lg-3
                          [:input.button.btn.btn-success.btn-lg.btn-block {:type "submit" :value "This is me!"}]]]]])
                    [:div.panel.panel-primary
                     [:div.panel-heading [:h3 "Create a new player"]]
                     [:div.panel-body
                      [:form.form-inline {:action "/user/create-player" :method "POST"}
                       [:div.form-group.col-lg-4
                        [:input.input-lg.form-control.col-lg-3 {:type "text" :name "playername" :placeholder "New Player"}]]
                       [:div.form-group.col-lg-3
                        [:button.btn.btn-primary.btn-lg.btn-block {:type "submit" :value "Report"} "Create!"]]]]]]))))))

(defn- user-page [{:keys [db config-options]} req]
  (let [unclaimed-players (d/get-players-without-openids db)]
    (layout/common
     config-options
     :title "User"
     :content (html5
               [:div
                [:h1 "Authenticating"]
                (if-let [auth (auth/current-auth)]
                  (let [{:keys [playerid playername]} auth]
                    [:div
                     [:p "Some information from openid:"
                      [:ul (for [[k v] auth]
                             [:li [:strong (str (name k) ":  ")] v])]]
                     [:h2 (str "nil? playerid " (nil? playerid) " not empty? " (not (empty? unclaimed-players)))]
                     (when (and (nil? playerid) (not (empty? unclaimed-players)))
                       [:div
                        [:h2 "Unclaimed players"]
                        [:table
                         [:thead [:tr [:th "Player"] [:th ""]]]
                         [:tbody (for [{:keys [id name]} unclaimed-players]
                                   [:tr [:td name] [:td [:a.button {:href (str "/user/claim-player/" id)} "Claim"]]])]]
                        [:form.form-inline {:action "/user/create-player" :method "POST"}
                         [:h1 "Create player"]
                         [:div.form-group.col-lg-6
                          [:input.form-control.col-lg-3 {:type "text" :name "playername" :placeholder "New Player"}]]
                         [:div.form-group.col-lg-6
                          [:button.btn.btn-primary {:type "submit" :value "Report"} "Create!"]]]])
                     (when playername
                       [:h2 (str "Welcome " playername "!")])
                     [:div
                      [:h3 "Logging out:"]
                      (auth/logout-form)]])
                  [:div
                   [:h3 "Login with:"]
                   (auth/login-form)])
                [:h3 (str "IS USER: "  (auth/user?))]
                [:h3 (str "IS ADMIN: " (auth/admin?))]
                [:h3 (str "current identity:" (auth/current-auth :identity))]
                [:h3 (str "current playername: " (auth/current-auth :playername))]]))))

(defn claim-player [{:keys [db]} id]
  (let [current-auth       (auth/current-auth)
        openid             (:identity current-auth)
        current-playername (:playername current-auth)
        id                 (util/parse-id id)
        player             (d/get-player db id)
        players-openids    (d/get-player-openids db id)]
    (if (and (not (auth/user?))
             openid
             (nil? current-playername)
             player
             (empty? players-openids))
      (do
        (info {:claiming-player player :for-openid openid :user-auth current-auth})
        (d/add-openid-to-player! db id openid)
        (friend/logout* (response/redirect (str "/user/created/" id))))
      (do
        (error ["cannot claim player" (util/symbols-as-map openid id current-playername players-openids)])
        (response/status (response/response "cannot claim player") 405)))))

(defn create-player [{:keys [db]} playername]
  (let [current-auth       (auth/current-auth)
        openid             (:identity current-auth)
        current-playername (:playername current-auth)]
    (if (and (not (auth/user?))
             openid
             (nil? current-playername))
      (do
        (info {:create-player playername :for-openid openid :user-auth current-auth})
        (let [id (d/create-player! db playername openid)]
          (friend/logout* (response/redirect (str "/user/created/" id)))))
      (do
        (error ["cannot create player" (util/symbols-as-map openid playername current-playername)])
        (response/status (response/response "cannot create player") 405)))))

(defn created-page [{:keys [config-options db]} id]
  (let [player (d/get-player db (util/parse-id id))]
    (layout/common config-options
     :title "Player Created"
     :content (html5
               [:h1 (str "Successfully created player " player)]
               [:p.lead "You have been logged out, please login again to continue using the system."]
               (auth/login-form)))))

(defn routes [deps]
  (compojure/routes
   (GET  "/user"
         req
         (user-page deps req))
   (GET  "/user/assign-player"
         req
         (assign-player-page deps))
   (POST "/user/claim-player"
         [playerid]
         (claim-player deps playerid))
   (POST "/user/create-player"
         [playername]
         (create-player deps playername))
   (GET  "/user/created/:id"
         [id]
         (created-page deps id))
   (GET  "/logout"
         req
         (friend/logout* (response/redirect (str (:context req) "/"))))))
