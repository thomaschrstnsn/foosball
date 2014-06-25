(ns foosball.auth)

(defn login-form [auth & {:keys [form-class button-class button-text] :or {button-text "Login"}}]
  (let [dom-id (str (gensym))
        url    (-> auth :provider :url)]
    [:form {:class form-class :method "POST" :action "/login"}
     [:input {:type "hidden" :name "identifier" :value url :id dom-id}]
     [:input.button.btn.btn-info {:class button-class :type "submit" :value button-text}]]))

(defn logout-form [& {:keys [text extra-class title] :or {text "Logout"}}]
  [:form {:class extra-class :method "POST" :action "/logout"}
   [:input.button.btn.btn-default {:type "submit" :value text :title title}]])
