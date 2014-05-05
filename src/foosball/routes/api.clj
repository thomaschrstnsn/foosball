(ns foosball.routes.api
  (:require [liberator.core :refer [resource defresource]]
            [cemerick.friend :as friend]
            [compojure.core :as compojure :refer [ANY]]
            [foosball.auth :as auth]
            [foosball.models.domains :as d]
            [clojure.data.json :as json]))

(extend java.util.UUID
  json/JSONWriter
  {:-write (fn [obj out]
             (json/-write (str obj) out))})

(defresource players [db]
  :available-media-types ["application/edn" "text/html" "application/json"]
  :handle-ok (fn [_] (d/get-players db)))

(defn routes [{:keys [db]}]
  (let [player-route (ANY "/api/players" [] (players db))]
    (compojure/routes
     (compojure/context "/private"
                        request
                        (friend/wrap-authorize (compojure/routes player-route)
                                               #{auth/user}))
     player-route)))
