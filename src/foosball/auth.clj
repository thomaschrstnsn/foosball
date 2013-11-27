(ns foosball.auth
  (:require [foosball.models.domains :as d]
            [foosball.util           :as util]
            [cemerick.friend         :as friend]
            [cemerick.friend.openid  :as openid]))

(defn has-role? [role]
  (friend/authorized? #{role} friend/*identity*))

(defn user? []
  (has-role? ::user))

(defn admin? []
  (has-role? ::admin))

(derive ::admin ::user)

(def admin ::admin)
(def user  ::user)

(defn current-auth
  ([]  (friend/current-authentication friend/*identity*))
  ([k] (k (current-auth))))

(defn credential-fn [db {:keys [identity] :as credentials}]
  (if-let [{:keys [playername playerid playerrole] :as player}
           (d/get-player-with-given-openid db identity)]
    (merge credentials
           {:roles [({:admin ::admin
                      :user  ::user} playerrole)]}
           (util/symbols-as-map playername playerid))
    credentials))

(defn wrap-friend-openid [db handler]
  (friend/authenticate handler
                       {:allow-anon? true
                        :default-landing-uri "/user/assign-player"
                        :workflows [(openid/workflow :openid-uri "/login"
                                                     :credential-fn (partial credential-fn db))]}))

(comment  {:name "Yahoo" :url "http://me.yahoo.com/"}
          {:name "AOL" :url "http://openid.aol.com/"}
          {:name "Wordpress.com" :url "http://username.wordpress.com"}
          {:name "MyOpenID" :url "http://username.myopenid.com/"})

(def provider {:name "Google" :url "https://www.google.com/accounts/o8/id"})

(defn login-form [& {:keys [form-class button-class button-text] :or {button-text "Login"}}]
  (let [{:keys [url]} provider
        base-login-url (str "/login?identifier=" url)
        dom-id (str (gensym))]
    [:form {:class form-class :method "POST" :action "/login"}
     [:input {:type "hidden" :name "identifier" :value url :id dom-id}]
     [:input.button.btn.btn-info {:class button-class :type "submit" :value button-text}]]))

(defn logout-form [& {:keys [text extra-class title] :or {text "Logout"}}]
  [:form {:class extra-class :method "GET" :action "/logout"}
   [:input.button.btn.btn-default {:type "submit" :value text :title title}]])
