(in-ns 'test.core)


(deftest adam-directives
  "Adam: Compulsive Hacker - install 3 directives"
  (do-game
    (new-game
      (default-corp)
      (make-deck "Adam: Compulsive Hacker" [(qty "Neutralize All Threats" 3) (qty "Safety First" 3)
                                                   (qty "Always Be Running" 3) (qty "Bank Job" 3)]))
    (let [nat (find-card "Neutralize All Threats" (get-in @state [:runner :rig :resource]))
          sf (find-card "Safety First" (get-in @state [:runner :rig :resource]))
          abr (find-card "Always Be Running" (get-in @state [:runner :rig :resource]))]
      (is (and nat sf abr) "3 directives installed")
      (is (= 3 (count (get-in @state [:runner :rig :resource]))) "Only the directives were installed"))))

(deftest andromeda
  "Andromeda - 9 card starting hand, 1 link"
  (do-game
    (new-game
      (default-corp)
      (make-deck "Andromeda: Dispossessed Ristie" [(qty "Sure Gamble" 3) (qty "Desperado" 3)
                                                   (qty "Security Testing" 3) (qty "Bank Job" 3)]))
    (is (= 1 (:link (get-runner))) "1 link")
    (is (= 9 (count (:hand (get-runner)))) "9 cards in Andromeda starting hand")))

(deftest andromeda-mulligan
  "Andromeda - 9 card starting hand after mulligan"
  (do-game
    (new-game
      (default-corp)
      (make-deck "Andromeda: Dispossessed Ristie" [(qty "Sure Gamble" 3) (qty "Desperado" 3)
                                                   (qty "Security Testing" 3) (qty "Bank Job" 3)])
      {:mulligan :runner})
    (is (= 1 (:link (get-runner))) "1 link")
    (is (= 9 (count (:hand (get-runner)))) "9 cards in Andromeda starting hand")))

(deftest andromeda-palana
  "Andromeda - should not grant Palana credits."
  (do-game
    (new-game
      (make-deck "Pālanā Foods: Sustainable Growth" [(qty "Hedge Fund" 3)])
      (make-deck "Andromeda: Dispossessed Ristie" [(qty "Sure Gamble" 3) (qty "Desperado" 3)
                                                   (qty "Security Testing" 3) (qty "Bank Job" 3)]))
    (is (= 5 (:credit (get-corp))) "Palana does not gain credit from Andromeda's starting hand")))


(deftest apex-facedown-console
  "Apex - Allow facedown install of a second console. Issue #1326"
  (do-game
    (new-game
      (default-corp)
      (make-deck "Apex: Invasive Predator" [(qty "Heartbeat" 2)]))
    (take-credits state :corp)
    (prompt-choice :runner "Done") ; no facedown install on turn 1
    (play-from-hand state :runner "Heartbeat")
    (is (= 1 (count (get-in @state [:runner :rig :hardware]))))
    (take-credits state :runner)
    (take-credits state :corp)
    (prompt-select :runner (find-card "Heartbeat" (:hand (get-runner))))
    (is (= 1 (count (get-in @state [:runner :rig :facedown]))) "2nd console installed facedown")))

(deftest argus-security
  "Argus Security - Runner chooses to take 1 tag or 2 meat damage when stealing an agenda"
  (do-game
    (new-game
      (make-deck "Argus Security: Protection Guaranteed" [(qty "Hostile Takeover" 2)])
      (default-runner))
    (play-from-hand state :corp "Hostile Takeover" "New remote")
    (play-from-hand state :corp "Hostile Takeover" "New remote")
    (take-credits state :corp)
    (let [ht1 (get-content state :remote1 0)
          ht2 (get-content state :remote2 0)]
      (run-empty-server state "Server 1")
      (prompt-choice :runner "Steal")
      (prompt-choice :runner "1 tag")
      (is (= 1 (:tag (get-runner))) "Took 1 tag from stealing an agenda")
      (run-empty-server state "Server 2")
      (prompt-choice :runner "Steal")
      (prompt-choice :runner "2 meat damage")
      (is (= 2 (count (:discard (get-runner)))) "Took 2 meat damage from stealing an agenda"))))

(deftest cerebral-imaging-max-hand-size
  "Cerebral Imaging - Maximum hand size equal to credits"
  (do-game
    (new-game
      (make-deck "Cerebral Imaging: Infinite Frontiers" [(qty "Hedge Fund" 3)])
      (default-runner))
    (play-from-hand state :corp "Hedge Fund")
    (play-from-hand state :corp "Hedge Fund")
    (is (= 13 (:credit (get-corp))) "Has 13 credits")
    (is (= 13 (core/hand-size state :corp)) "Max hand size is 13")))

(deftest chronos-protocol
  "Chronos Protocol - Choose Runner discard for first net damage of a turn"
  (do-game
    (new-game
      (make-deck "Chronos Protocol: Selective Mind-mapping" [(qty "Pup" 1) (qty "Neural EMP" 2)])
      (default-runner [(qty "Imp" 3)]))
    (play-from-hand state :corp "Pup" "HQ")
    (take-credits state :corp)
    (run-on state :hq)
    (let [pup (get-ice state :hq 0)]
      (core/rez state :corp pup)
      (card-ability state :corp pup 0)
      (prompt-choice :corp "Yes")
      (let [imp (find-card "Imp" (:hand (get-runner)))]
        (prompt-choice :corp imp)
        (is (= 1 (count (:discard (get-runner)))))
        (card-ability state :corp pup 0)
        (is (empty? (:prompt (get-corp))) "No choice on second net damage")
        (is (= 2 (count (:discard (get-runner)))))
        (run-jack-out state)
        (take-credits state :runner)
        (core/move state :runner (find-card "Imp" (:discard (get-runner))) :hand)
        (play-from-hand state :corp "Neural EMP")
        (prompt-choice :corp "No")
        (is (= 2 (count (:discard (get-runner)))) "Damage dealt after declining ability")
        (play-from-hand state :corp "Neural EMP")
        (is (empty? (:prompt (get-corp))) "No choice after declining on first damage")
        (is (= 3 (count (:discard (get-runner)))))))))

(deftest edward-kim
  "Edward Kim - Trash first operation accessed each turn, but not if first one was in Archives"
  (do-game
    (new-game
      (default-corp [(qty "Hedge Fund" 3) (qty "Restructure" 2) (qty "PAD Campaign" 1)])
      (make-deck "Edward Kim: Humanity's Hammer" [(qty "Eater" 1) (qty "Sure Gamble" 2)]))
    (play-from-hand state :corp "Hedge Fund")
    (trash-from-hand state :corp "PAD Campaign")
    (take-credits state :corp)
    (run-empty-server state "Archives")
    (run-empty-server state "HQ")
    (is (= 2 (count (:discard (get-corp)))) "No operation trashed from HQ; accessed one in Archives first")
    (take-credits state :runner)
    (core/move state :corp (find-card "Hedge Fund" (:discard (get-corp))) :hand)
    (is (= 1 (count (:discard (get-corp)))))
    (take-credits state :corp)
    (run-empty-server state "Archives")
    (run-empty-server state "HQ")
    (is (= 2 (count (:discard (get-corp)))) "1 operation trashed from HQ; accessed non-operation in Archives first")
    (take-credits state :runner)
    (play-from-hand state :corp "Hedge Fund")
    (take-credits state :corp)
    (play-from-hand state :runner "Eater")
    (let [eater (get-in @state [:runner :rig :program 0])]
      (run-on state "Archives")
      (card-ability state :runner eater 0) ; pretend to break a sub so no cards in Archives will be accessed
      (run-successful state)
      (is (= 3 (count (:discard (get-corp)))))
      (run-empty-server state "HQ")
      (is (= 4 (count (:discard (get-corp)))) "1 operation trashed from HQ; accessed non-operation in Archives first"))))

(deftest gabriel-santiago
  "Gabriel Santiago - Gain 2c on first successful HQ run each turn"
  (do-game
    (new-game
      (default-corp)
      (make-deck "Gabriel Santiago: Consummate Professional" [(qty "Easy Mark" 1)]))
    (take-credits state :corp)
    (run-empty-server state :rd)
    (is (= 5 (:credit (get-runner))) "No credits gained")
    (run-empty-server state :hq)
    (is (= 7 (:credit (get-runner))) "Gained 2c")
    (run-empty-server state :hq)
    (is (= 7 (:credit (get-runner))) "No credits gained")))

(deftest grndl-power-unleashed
  "GRNDL: Power Unleashed - start game with 10 credits and 1 bad pub."
  (do-game
    (new-game
      (make-deck "GRNDL: Power Unleashed" [(qty "Hedge Fund" 3)])
      (default-runner))
    (is (= 10 (:credit (get-corp))) "GRNDL starts with 10 credits")
    (is (= 1 (:bad-publicity (get-corp))) "GRNDL starts with 1 bad publicity")))

(deftest grndl-valencia
  "GRNDL vs Valencia - only 1 bad pub at start"
  (do-game
    (new-game
      (make-deck "GRNDL: Power Unleashed" [(qty "Hedge Fund" 3)])
      (make-deck "Valencia Estevez: The Angel of Cayambe" [(qty "Sure Gamble" 3)]))
    (is (= 10 (:credit (get-corp))) "GRNDL starts with 10 credits")
    (is (= 1 (:bad-publicity (get-corp))) "GRNDL starts with 1 bad publicity")))

(deftest haarpsichord-studios
  "Haarpsichord Studios - Prevent stealing more than 1 agenda per turn"
  (do-game
    (new-game
      (make-deck "Haarpsichord Studios: Entertainment Unleashed" [(qty "15 Minutes" 3)])
      (default-runner [(qty "Gang Sign" 1)]))
    (take-credits state :corp)
    (play-from-hand state :runner "Gang Sign")
    (run-empty-server state "HQ")
    (prompt-choice :runner "Steal")
    (is (= 1 (:agenda-point (get-runner))))
    (run-empty-server state "HQ")
    (prompt-choice :runner "Steal")
    (is (= 1 (:agenda-point (get-runner))) "Second steal of turn prevented")
    (take-credits state :runner)
    (play-from-hand state :corp "15 Minutes" "New remote")
    (score-agenda state :corp (get-content state :remote1 0))
    (let [gs (get-in @state [:runner :rig :resource 0])]
      (card-ability state :runner gs 0)
      (prompt-choice :runner "Steal")
      (is (= 2 (:agenda-point (get-runner))) "Steal prevention didn't carry over to Corp turn"))))

(deftest haarpsichord-studios-employee-strike
  "Haarpsichord Studios - Interactions with Employee Strike. Issue #1313."
  (do-game
    (new-game
      (make-deck "Haarpsichord Studios: Entertainment Unleashed" [(qty "15 Minutes" 3)])
      (default-runner [(qty "Employee Strike" 1) (qty "Scrubbed" 1)]))
    (take-credits state :corp)
    (core/gain state :runner :click 5)
    (run-empty-server state "HQ")
    (prompt-choice :runner "Steal")
    (is (= 1 (:agenda-point (get-runner))))
    (play-from-hand state :runner "Employee Strike")
    (run-empty-server state "HQ")
    (prompt-choice :runner "Steal")
    (is (= 2 (:agenda-point (get-runner))) "Second steal not prevented")
    (play-from-hand state :runner "Scrubbed")
    (run-empty-server state "HQ")
    (prompt-choice :runner "Steal")
    (is (= 2 (:agenda-point (get-runner))) "Third steal prevented")))

(deftest haas-bioroid-stronger-together
  "Stronger Together - +1 strength for Bioroid ice"
  (do-game
    (new-game
      (make-deck "Haas-Bioroid: Stronger Together" [(qty "Eli 1.0" 1)])
      (default-runner))
    (play-from-hand state :corp "Eli 1.0" "Archives")
    (let [eli (get-ice state :archives 0)]
      (core/rez state :corp eli)
      (is (= 5 (:current-strength (refresh eli))) "Eli 1.0 at 5 strength"))))

(deftest iain-stirling-credits
  "Iain Stirling - Gain 2 credits when behind"
  (do-game
    (new-game
      (default-corp [(qty "Breaking News" 1)])
      (make-deck "Iain Stirling: Retired Spook" [(qty "Sure Gamble" 3)]))
    (play-from-hand state :corp "Breaking News" "New remote")
    (let [ag1 (get-in @state [:corp :servers :remote1 :content 0])]
      (core/advance state :corp {:card (refresh ag1)})
      (core/advance state :corp {:card (refresh ag1)})
      (core/score state :corp {:card (refresh ag1)})
      (take-credits state :corp)
      (is (= 1 (:agenda-point (get-corp))) "Corp gains 1 agenda point from Breaking News")
      (take-credits state :runner 1)
      (is (= 8 (:credit (get-runner))) "Gained 2 credits from being behind on points"))))

(deftest industrial-genomics-trash-cost
  "Industrial Genomics - Increase trash cost"
  (do-game
    (new-game
      (make-deck "Industrial Genomics: Growing Solutions" [(qty "PAD Campaign" 3)
                                                           (qty "Hedge Fund" 3)])
      (default-runner))
    (play-from-hand state :corp "PAD Campaign" "New remote")
    (trash-from-hand state :corp "PAD Campaign")
    (trash-from-hand state :corp "PAD Campaign")
    (trash-from-hand state :corp "Hedge Fund")
    (trash-from-hand state :corp "Hedge Fund")
    (let [pad (get-content state :remote1 0)]
      (core/rez state :corp pad)
      (take-credits state :corp)
      (run-empty-server state "Server 1")
      (is (= 8 (core/trash-cost state :runner (refresh pad)))))))

(deftest jesminder-sareen-ability
  "Jesminder Sareen - avoid tags only during a run"
  (do-game
    (new-game (default-corp [(qty "SEA Source" 1) (qty "Data Raven" 1)])
              (make-deck "Jesminder Sareen: Girl Behind the Curtain" [(qty "Sure Gamble" 3)]))
    (play-from-hand state :corp "Data Raven" "Archives")
    (take-credits state :corp)
    (let [dr (-> @state :corp :servers :archives :ices first)]
      (core/rez state :corp dr)
      (core/click-run state :runner {:server "Archives"})
      (card-ability state :corp dr 0)
      (is (= 0 (:tag (get-runner))) "Jesminder avoided first tag during the run")
      (card-ability state :corp dr 0)
      (is (= 1 (:tag (get-runner))) "Jesminder did not avoid the second tag during the run")
      (core/no-action state :corp nil)
      (core/continue state :runner nil)
      (core/no-action state :corp nil)
      (core/successful-run state :runner nil)
      (run-empty-server state "R&D") ; clear per-run buffer
      (take-credits state :runner)
      (play-from-hand state :corp "SEA Source")
      (prompt-choice :corp 0)
      (prompt-choice :runner 0)
      (is (= 2 (:tag (get-runner))) "Jesminder did not avoid the tag outside of a run"))))

(deftest jesminder-john-masanori
  "Jesminder Sareen - don't avoid John Masanori tag"
  (do-game
    (new-game (default-corp)
              (make-deck "Jesminder Sareen: Girl Behind the Curtain" [(qty "John Masanori" 1)]))
    (take-credits state :corp)
    (play-from-hand state :runner "John Masanori")
    (run-on state "HQ")
    (core/jack-out state :runner nil)
    (is (= 1 (:tag (get-runner))) "Jesminder did not avoid John Masanori tag")))

(deftest jinteki-replicating-perfection
  "Replicating Perfection - Prevent runner from running on remotes unless they first run on a central"
  (do-game
    (new-game
      (make-deck "Jinteki: Replicating Perfection" [(qty "Mental Health Clinic" 3)])
      (default-runner))
    (play-from-hand state :corp "Mental Health Clinic" "New remote")
    (take-credits state :corp)
    (is (not (core/can-run-server? state "Server 1")) "Runner can only run on centrals")
    (run-empty-server state "HQ")
    (is (boolean (core/can-run-server? state "Server 1")) "Runner can run on remotes")))

(deftest jinteki-replicating-perfection-employee-strike
  "Replicating Perfection - interaction with Employee Strike. Issue #1313."
  (do-game
    (new-game
      (make-deck "Jinteki: Replicating Perfection" [(qty "Mental Health Clinic" 3)])
      (default-runner [(qty "Employee Strike" 1) (qty "Scrubbed" 1)]))
    (play-from-hand state :corp "Mental Health Clinic" "New remote")
    (take-credits state :corp)
    (is (not (core/can-run-server? state "Server 1")) "Runner can only run on centrals")
    (play-from-hand state :runner "Employee Strike")
    (is (boolean (core/can-run-server? state "Server 1")) "Runner can run on remotes")))

(deftest kate-mac-mccaffrey-discount
  "Kate 'Mac' McCaffrey - Install discount"
  (do-game
    (new-game (default-corp)
              (make-deck "Kate \"Mac\" McCaffrey: Digital Tinker" [(qty "Magnum Opus" 1)]))
    (take-credits state :corp)
    (play-from-hand state :runner "Magnum Opus")
    (is (= 1 (:credit (get-runner))) "Installed Magnum Opus for 4 credits")))

(deftest kate-mac-mccaffrey-no-discount
  "Kate 'Mac' McCaffrey - No discount for 0 cost"
  (do-game
    (new-game (default-corp)
              (make-deck "Kate \"Mac\" McCaffrey: Digital Tinker"
                         [(qty "Magnum Opus" 1)
                          (qty "Self-modifying Code" 1)]))
    (take-credits state :corp)
    (play-from-hand state :runner "Self-modifying Code")
    (play-from-hand state :runner "Magnum Opus")
    (is (= 0 (:credit (get-runner))) "No Kate discount on second program install")))

(deftest kate-mac-mccaffrey-discount-cant-afford
  "Kate 'Mac' McCaffrey - Can Only Afford With the Discount"
  (do-game
    (new-game (default-corp)
              (make-deck "Kate \"Mac\" McCaffrey: Digital Tinker" [(qty "Magnum Opus" 1)]))
    (take-credits state :corp)
    (core/lose state :runner :credit 1)
    (is (= 4 (:credit (get-runner))))
    (play-from-hand state :runner "Magnum Opus")
    (is (= 1 (count (get-in @state [:runner :rig :program]))) "Magnum Opus installed")
    (is (= 0 (:credit (get-runner))) "Installed Magnum Opus for 4 credits")))

(deftest ken-tenma-run-event-credit
  "Ken 'Express' Tenma - Gain 1 credit when first Run event played"
  (do-game
    (new-game (default-corp)
              (make-deck "Ken \"Express\" Tenma: Disappeared Clone" [(qty "Account Siphon" 2)]))
    (take-credits state :corp)
    (play-run-event state (first (:hand (get-runner))) :hq)
    (is (= 6 (:credit (get-runner))) "Gained 1 credit for first Run event")
    (prompt-choice :runner "Run ability")
    (play-run-event state (first (:hand (get-runner))) :hq)
    (is (= 16 (:credit (get-runner))) "No credit gained for second Run event")))

(deftest maxx-wyldside-start-of-turn
  "MaxX and Wyldside - using Wyldside during Step 1.2 should lose 1 click"
  (do-game
    (new-game (default-corp)
              (make-deck "MaxX: Maximum Punk Rock" [(qty "Wyldside" 3)
                                                     (qty "Sure Gamble" 3)
                                                     (qty "Infiltration" 3)
                                                     (qty "Corroder" 3)
                                                     (qty "Eater" 3)]))
    (take-credits state :corp)
    (is (= 2 (count (:discard (get-runner)))) "MaxX discarded 2 cards at start of turn")
    (starting-hand state :runner ["Wyldside"])
    (play-from-hand state :runner "Wyldside")
    (take-credits state :runner 3)
    (is (= 5 (:credit (get-runner))) "Runner has 5 credits at end of first turn")
    (is (find-card "Wyldside" (get-in @state [:runner :rig :resource])) "Wyldside was installed")
    (take-credits state :corp)
    (is (= 0 (:click (get-runner))) "Runner has 0 clicks")
    (is (:runner-phase-12 @state) "Runner is in Step 1.2")
    (let [maxx (get-in @state [:runner :identity])
          wyld (find-card "Wyldside" (get-in @state [:runner :rig :resource]))]
      (card-ability state :runner maxx 0)
      (card-ability state :runner wyld 0)
      (core/end-phase-12 state :runner nil)
      (is (= 4 (count (:discard (get-runner)))) "MaxX discarded 2 cards at start of turn")
      (is (= 3 (:click (get-runner))) "Wyldside caused 1 click to be lost")
      (is (= 3 (count (:hand (get-runner)))) "3 cards drawn total"))))

(deftest nasir-ability-basic
  "Nasir Ability - Basic"
  (do-game
    (new-game
      (default-corp [(qty "Ice Wall" 3)])
      (make-deck "Nasir Meidan: Cyber Explorer" []))
    (play-from-hand state :corp "Ice Wall" "HQ")
    (take-credits state :corp)

    (run-on state "HQ")
    (let [iwall (get-ice state :hq 0)
          nasir (get-in @state [:runner :identity])]
      (core/rez state :corp iwall)
      (is (= 5 (:credit (get-runner))) "Nasir Ability does not trigger automatically")
      (card-ability state :runner nasir 0)
      (is (= 1 (:credit (get-runner))) "Credits at 1 after Nasir ability trigger"))))

(deftest nasir-ability-xanadu
  "Nasir Ability - Xanadu"
  (do-game
    (new-game
      (default-corp [(qty "Ice Wall" 1)])
      (make-deck "Nasir Meidan: Cyber Explorer" [(qty "Xanadu" 1)]))
    (play-from-hand state :corp "Ice Wall" "HQ")
    (take-credits state :corp)

    (swap! state assoc-in [:runner :credit] 6)
    (play-from-hand state :runner "Xanadu")
    (run-on state "HQ")
    (let [iwall (get-in @state [:corp :servers :hq :ices 0])
          nasir (get-in @state [:runner :identity])]
      (core/rez state :corp iwall)
      (is (= 3 (:credit (get-runner))) "Pay 3 to install Xanadu")
      (card-ability state :runner nasir 0)
      (is (= 2 (:credit (get-runner))) "Gain 1 more credit due to Xanadu"))))

(deftest nisei-division
  "Nisei Division - Gain 1 credit from every psi game"
  (do-game
    (new-game
      (make-deck "Nisei Division: The Next Generation" [(qty "Snowflake" 2)])
      (default-runner))
    (play-from-hand state :corp "Snowflake" "HQ")
    (play-from-hand state :corp "Snowflake" "HQ")
    (take-credits state :corp)
    (let [s1 (get-in @state [:corp :servers :hq :ices 0])
          s2 (get-in @state [:corp :servers :hq :ices 1])]
      (run-on state "HQ")
      (core/rez state :corp s2)
      (is (= 4 (:credit (get-corp))))
      (card-ability state :corp s2 0)
      (prompt-choice :corp "0 [Credits]")
      (prompt-choice :runner "0 [Credits]")
      (is (= 5 (:credit (get-corp))) "Gained 1 credit from psi game")
      (core/no-action state :corp nil)
      (core/rez state :corp s1)
      (is (= 4 (:credit (get-corp))))
      (card-ability state :corp s1 0)
      (prompt-choice :corp "0 [Credits]")
      (prompt-choice :runner "1 [Credits]")
      (is (= 5 (:credit (get-corp))) "Gained 1 credit from psi game"))))

(deftest quetzal-ability
  "Quetzal ability- once per turn"
  (do-game
    (new-game
      (default-corp [(qty "Ice Wall" 3)])
      (make-deck "Quetzal: Free Spirit" [(qty "Sure Gamble" 3)]))
    (play-from-hand state :corp "Ice Wall" "HQ")
    (take-credits state :corp)
    (run-on state "HQ")
    (let [q (get-in @state [:runner :identity])
          iwall (get-ice state :hq 0)
          qdef (core/card-def (get-in @state [:runner :identity]))]
      (core/rez state :corp iwall)
      (card-ability state :runner q 0)
      (is (last-log-contains? state (get-in qdef [:abilities 0 :msg]))
          "Quetzal ability did trigger")
      (run-jack-out state)
      (core/click-credit state :runner nil)
      (run-on state "HQ")
      (card-ability state :runner (refresh q) 0)
      (is (not (last-log-contains? state (get-in qdef [:abilities 0 :msg])))
          "Quetzal ability did not trigger")
      (run-jack-out state)
      (take-credits state :runner)
      (take-credits state :corp)
      (core/click-credit state :runner nil)
      (run-on state "HQ")
      (card-ability state :runner (refresh q) 0)
      (is (last-log-contains? state (get-in qdef [:abilities 0 :msg]))
          "Quetzal ability did trigger")
      (core/jack-out state :runner nil))))

(deftest reina-rez-cost-increase
  "Reina Roja - Increase cost of first rezzed ICE"
  (do-game
    (new-game
      (default-corp [(qty "Quandary" 3)])
      (make-deck "Reina Roja: Freedom Fighter" []))
    (play-from-hand state :corp "Quandary" "R&D")
    (take-credits state :corp)
    (is (= 7 (:credit (get-corp))))
    (run-on state "R&D")
    (let [quan (get-ice state :rd 0)]
      (core/rez state :corp quan)
      (is (= 5 (:credit (get-corp))) "Rez cost increased by 1"))))

(deftest spark-advertisements
  "Spark Agency - Rezzing advertisements"
  (do-game
    (new-game
      (make-deck "Spark Agency: Worldswide Reach" [(qty "Launch Campaign" 2)])
      (default-runner))
    (play-from-hand state :corp "Launch Campaign" "New remote")
    (play-from-hand state :corp "Launch Campaign" "New remote")
    (let [lc1 (get-content state :remote1 0)
          lc2 (get-content state :remote2 0)]
      (core/rez state :corp lc1)
      (is (= 4 (:credit (get-runner)))
          "Runner lost 1 credit from rez of advertisement (Corp turn)")
      (take-credits state :corp)
      (run-on state "Server 1")
      (core/rez state :corp lc2)
      (is (= 3 (:credit (get-runner)))
          "Runner lost 1 credit from rez of advertisement (Runner turn)"))))

(deftest strategic-innovations-future-forward
  "Strategic Innovations: Future Forward - Ability"
  (do-game
    (new-game
      (make-deck "Strategic Innovations: Future Forward" [(qty "Hedge Fund" 2)
                                                          (qty "Eli 1.0" 2)
                                                          (qty "Crick" 2)])
      (default-runner))
    (play-from-hand state :corp "Eli 1.0" "New remote")
    (play-from-hand state :corp "Hedge Fund")
    (play-from-hand state :corp "Crick" "New remote")
    (let [i1 (get-ice state :remote1 0)
          i2 (get-ice state :remote2 0)]
      (take-credits state :corp 0)
      (take-credits state :runner)
      (core/rez state :corp i1)
      (take-credits state :corp)
      (take-credits state :runner)
      (is (= 1 (count (:prompt (get-corp)))) "Corp prompted to trigger Strategic Innovations")
      (prompt-select :corp (first (:discard (get-corp))))
      (is (empty? (:discard (get-corp))) "Hedge Fund moved back to R&D")
      (take-credits state :corp)
      (core/rez state :corp i2)
      (take-credits state :runner)
      (is (= 0 (count (:prompt (get-corp))))
          "Corp not prompted to trigger Strategic Innovations"))))

(deftest titan-agenda-counter
  "Titan Transnational - Add a counter to a scored agenda"
  (do-game
    (new-game
      (make-deck "Titan Transnational: Investing In Your Future" [(qty "Project Atlas" 1)])
      (default-runner))
    (play-from-hand state :corp "Project Atlas" "New remote")
    (let [atl (get-content state :remote1 0)]
      (core/gain state :corp :click 1)
      (core/advance state :corp {:card (refresh atl)})
      (core/advance state :corp {:card (refresh atl)})
      (core/advance state :corp {:card (refresh atl)})
      (core/score state :corp {:card (refresh atl)})
      (let [scored (get-in @state [:corp :scored 0])]
        (is (= 1 (:counter scored)) "1 counter added by Titan")))))

(deftest wyvern-chemically-enhanced
  "Wyvern: Chemically Enhanced - Ability"
  (do-game
    (new-game (default-corp [(qty "Launch Campaign" 3)])
              (make-deck "Wyvern: Chemically Enhanced" [(qty "Sure Gamble" 2)
                                                        (qty "Corroder" 1)
                                                        (qty "Clone Chip" 1)
                                                        (qty "Easy Mark" 1)]))
    (play-from-hand state :corp "Launch Campaign" "New remote")
    (play-from-hand state :corp "Launch Campaign" "New remote")
    (take-credits state :corp)
    (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
    (play-from-hand state :runner "Sure Gamble")
    (play-from-hand state :runner "Easy Mark")
    (play-from-hand state :runner "Corroder")
    (run-empty-server state "Server 1")
    (prompt-choice :runner "Yes")
    ;; trash Launch Campaign, should trigger wyvern
    (is (= "Sure Gamble" (:title (last (:discard (get-runner)))))
        "Sure Gamble still in Wyvern's discard")
    (is (some #(= "Easy Mark" (:title %)) (:deck (get-runner))) "Easy Mark moved to deck")
    (take-credits state :runner)
    (take-credits state :corp)
    (play-from-hand state :runner "Clone Chip")
    (run-empty-server state "Server 2")
    (prompt-choice :runner "Yes")
    (is (= "Sure Gamble" (:title (last (:discard (get-runner)))))
        "Sure Gamble still in Wyvern's discard")))
