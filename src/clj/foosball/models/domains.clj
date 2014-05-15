(ns foosball.models.domains)

(defprotocol Players
  (get-player [this id])
  (get-players [this])

  (create-player! [this id name openid])
  (rename-player! [this id new-name])

  (activate-player! [this id])
  (deactivate-player! [this id]))

(defprotocol Matches
  (get-matches [this])
  (create-match! [this match])
  (delete-match! [this id]))

(defprotocol OpenIds
  (get-players-with-openids [this])
  (get-players-without-openids [this])
  (get-player-openids [this id])
  (add-openid-to-player! [this playerid openid])
  (get-player-with-given-openid [this openid]))
