(in-ns 'test.core)

(deftest adonis-campaign
  (do-game
    (new-game (default-corp [(qty "Adonis Campaign" 1)])
              (default-runner))
    (play-from-hand state :corp "Adonis Campaign" "New remote")
    (let [ac (get-content state :remote1 0)]
      (core/rez state :corp ac)
      (is (= 1 (get-in @state [:corp :credit])))
      (is (= 12 (get-in (refresh ac) [:counter])) "12 counters on Adonis")
      (take-credits state :corp 2)
      (take-credits state :runner)
      (is (= 6 (get-in @state [:corp :credit])) "Gain 3 from Adonis")
      (is (= 9 (get-in (refresh ac) [:counter]))) "9 counter remaining on Adonis")))

(deftest aggressive-secretary
  (do-game
    (new-game
      (default-corp [(qty "Aggressive Secretary" 1)])
      (default-runner [(qty "Cache" 3)]))
    (play-from-hand state :corp "Aggressive Secretary" "New remote")
    (let [as (get-content state :remote1 0)]
      (core/advance state :corp as)
      (take-credits state :corp)
      ;; Run on AggSec with 3 programs
      (play-from-hand state :runner "Cache")
      (play-from-hand state :runner "Cache")
      (play-from-hand state :runner "Cache")
      (run-empty-server state "Server 1")
      (prompt-choice :corp "Yes")
      (is (= 3 (get-in @state [:corp :credit])))
      (prompt-select :corp (get-in @state [:runner :rig :program 1]))
      (prompt-choice :corp "Done")
      ;; There should be one Cache left
      (is (= 3 (get-in @state [:corp :credit])))
      (is (= 2 (count (get-in @state [:runner :rig :program])))))))

(deftest alix-t4lb07
  (do-game
    (new-game
      (default-corp [(qty "Alix T4LB07" 1) (qty "PAD Campaign" 3)])
      (default-runner))
    (play-from-hand state :corp "Alix T4LB07" "New remote")
    (let [alix (get-content state :remote1 0)]
      (core/rez state :corp alix)
      (play-from-hand state :corp "PAD Campaign" "New remote")
      (play-from-hand state :corp "PAD Campaign" "New remote")
      (take-credits state :corp)
      (take-credits state :runner)
      (is (= 2 (get-in (refresh alix) [:counter])) "Two counters on Alix")
      (is (= 4 (get-in @state [:corp :credit])))
      (card-ability state :corp alix 0)
      (is (= 8 (get-in @state [:corp :credit]))))) "Gain 4 credits from Alix")

(deftest brain-taping-warehouse
  "Brain-Taping Warehouse - Lower rez cost of bioroid ICE by 1 for each unspent Runner click"
  (do-game
    (new-game (default-corp [(qty "Brain-Taping Warehouse" 1) (qty "Ichi 1.0" 1)
                             (qty "Eli 1.0" 1)])
              (default-runner))
    (play-from-hand state :corp "Brain-Taping Warehouse" "New remote")
    (play-from-hand state :corp "Ichi 1.0" "Server 1")
    (play-from-hand state :corp "Eli 1.0" "HQ")
    (let [ichi (get-ice state :remote1 0)
          eli (get-ice state :hq 0)]
      (take-credits state :corp)
      (run-on state :remote1)
      (core/rez state :corp (get-content state :remote1 0))
      (is (= 3 (:click (get-runner))))
      (core/rez state :corp ichi)
      (is (= 2 (:credit (get-corp))) "Paid only 2c to rez Ichi; reduction of 3c")
      (run-jack-out state)
      (run-on state :hq)
      (is (= 2 (:click (get-runner))))
      (core/rez state :corp eli)
      (is (= 1 (:credit (get-corp))) "Paid only 1c to rez Eli; reduction of 2c"))))

(deftest capital-investors
  "Capital Investors - Click for 2 credits"
  (do-game
    (new-game (default-corp [(qty "Capital Investors" 1)])
              (default-runner))
    (play-from-hand state :corp "Capital Investors" "New remote")
    (let [cap (get-content state :remote1 0)]
      (core/rez state :corp cap)
      (card-ability state :corp cap 0)
      (card-ability state :corp cap 0)
      (is (= 0 (:click (get-corp))) "Used twice, spent 2 clicks")
      (is (= 7 (:credit (get-corp))) "Used twice, gained 4 credits"))))

(deftest chairman-hiro
  "Chairman Hiro - Reduce Runner max hand size; add as 2 agenda points if Runner trashes him"
  (do-game
    (new-game (default-corp [(qty "Chairman Hiro" 2)])
              (default-runner))
    (play-from-hand state :corp "Chairman Hiro" "New remote")
    (play-from-hand state :corp "Chairman Hiro" "Server 1")
    (is (= 1 (count (:discard (get-corp)))) "First Hiro trashed")
    (is (= 0 (:agenda-point (get-runner))) "No points for Runner if trashed by Corp")
    (let [hiro (get-content state :remote1 0)]
      (core/rez state :corp hiro)
      (is (= 3 (core/hand-size state :runner)) "Runner max hand size reduced by 2")
      (take-credits state :corp)
      (take-credits state :runner 3)
      (run-empty-server state "Server 1")
      (prompt-choice :runner "Yes") ; trash Hiro
      (is (= 2 (:credit (get-runner))) "Runner paid 6 credits to trash")
      (is (= 5 (core/hand-size state :runner)) "Runner max hand size restored to 5")
      (is (= 1 (count (get-in @state [:runner :scored])))
          "Chairman Hiro added to Runner score area")
      (is (= 2 (:agenda-point (get-runner))) "Runner gained 2 agenda points"))))

(deftest city-surveillance
  "City Surveillance - Runner chooses to pay 1 credit or take 1 tag at start of their turn"
  (do-game
    (new-game (default-corp [(qty "City Surveillance" 1)])
              (default-runner))
    (play-from-hand state :corp "City Surveillance" "New remote")
    (let [surv (get-content state :remote1 0)]
      (core/rez state :corp surv)
      (take-credits state :corp)
      (prompt-choice :runner "Pay 1 [Credits]")
      (is (= 4 (:credit (get-runner))) "Runner paid 1 credit")
      (is (= 0 (:tag (get-runner))) "Runner didn't take a tag")
      (take-credits state :runner)
      (take-credits state :corp)
      (prompt-choice :runner "Take 1 tag")
      (is (= 8 (:credit (get-runner))) "Runner paid no credits")
      (is (= 1 (:tag (get-runner))) "Runner took 1 tag"))))

(deftest dedicated-response-team
  "Dedicated Response Team - Do 2 meat damage when successful run ends if Runner is tagged"
  (do-game
    (new-game (default-corp [(qty "Dedicated Response Team" 1)])
              (default-runner))
    (play-from-hand state :corp "Dedicated Response Team" "New remote")
    (let [drt (get-content state :remote1 0)]
      (core/rez state :corp drt)
      (take-credits state :corp)
      (run-empty-server state :rd)
      (is (empty? (:discard (get-runner))) "Not tagged, no damage done")
      (core/gain state :runner :tag 1)
      (run-on state :rd)
      (run-jack-out state)
      (is (empty? (:discard (get-runner))) "Tagged but run unsuccessful, no damage done")
      (run-empty-server state :rd)
      (is (= 2 (count (:discard (get-runner)))) "Suffered 2 damage for successful run w/ tag"))))

(deftest early-premiere
  "Early Premiere - Pay 1c at start of turn to place an advancement on a card in a server"
  (do-game
    (new-game (default-corp [(qty "Early Premiere" 1) (qty "Ice Wall" 1)
                             (qty "Ghost Branch" 1) (qty "Blacklist" 1)])
              (default-runner))
    (core/gain state :corp :click 1)
    (play-from-hand state :corp "Early Premiere" "New remote")
    (play-from-hand state :corp "Blacklist" "New remote")
    (play-from-hand state :corp "Ghost Branch" "New remote")
    (play-from-hand state :corp "Ice Wall" "HQ")
    (let [ep (get-content state :remote1 0)
          bl (get-content state :remote2 0)
          gb (get-content state :remote3 0)
          iw (get-ice state :hq 0)]
      (core/rez state :corp ep)
      (take-credits state :corp)
      (take-credits state :runner)
      (card-ability state :corp ep 0)
      (prompt-select :corp iw)
      (is (nil? (:advance-counter (refresh iw))) "Ice Wall can't targeted, not in server")
      (prompt-select :corp bl)
      (is (nil? (:advance-counter (refresh bl))) "Blacklist can't targeted, can't be advanced")
      (prompt-select :corp gb)
      (is (= 1 (:advance-counter (refresh gb))) "1 advancement on Ghost Branch")
      (is (= 4 (:credit (get-corp)))))))

(deftest edge-of-world
  "Edge of World - ability"
  (do-game
    (new-game (default-corp [(qty "Edge of World" 3) (qty "Ice Wall" 3)])
              (default-runner))
    (core/gain state :corp :credit 6 :click 1)
    (play-from-hand state :corp "Edge of World" "New remote")
    (play-from-hand state :corp "Edge of World" "New remote")
    (play-from-hand state :corp "Ice Wall" "Server 1")
    (play-from-hand state :corp "Ice Wall" "Server 1")
    (take-credits state :corp)
    (run-empty-server state "Server 1")
    (is (= :waiting (-> @state :runner :prompt first :prompt-type))
        "Runner waiting for Corp to act")
    (prompt-choice :corp "Yes")
    (prompt-choice :runner "Yes")
    (is (= 2 (:brain-damage (get-runner))) "Runner took 2 brain damage")
    (run-empty-server state "Server 2")
    (prompt-choice :corp "Yes")
    (prompt-choice :runner "Yes")
    (is (= 2 (:brain-damage (get-runner))) "Runner did not take brain damage when no ICE protected Edge of World")))

(deftest elizabeth-mills
  "Elizabeth Mills - Remove 1 bad publicity when rezzed; click-trash to trash a location"
  (do-game
    (new-game (default-corp [(qty "Elizabeth Mills" 1)])
              (default-runner [(qty "Earthrise Hotel" 1)]))
    (core/gain state :corp :bad-publicity 1)
    (play-from-hand state :corp "Elizabeth Mills" "New remote")
    (take-credits state :corp)
    (play-from-hand state :runner "Earthrise Hotel")
    (take-credits state :runner)
    (let [liz (get-content state :remote1 0)
          hotel (get-in @state [:runner :rig :resource 0])]
      (core/rez state :corp liz)
      (is (= 0 (:bad-publicity (get-corp))) "1 bad publicity removed")
      (card-ability state :corp liz 0)
      (prompt-select :corp hotel)
      (is (= 1 (count (:discard (get-runner)))) "Earthrise trashed")
      (is (= 1 (count (:discard (get-corp)))) "Elizabeth Mills trashed")
      (is (= 1 (:bad-publicity (get-corp))) "1 bad publicity taken from trashing a location"))))

(deftest elizas-toybox
  "Eliza's Toybox - Rez a card ignoring all costs"
  (do-game
    (new-game (default-corp [(qty "Eliza's Toybox" 1) (qty "Wotan" 1)])
              (default-runner))
    (core/gain state :corp :click 2)
    (play-from-hand state :corp "Wotan" "R&D")
    (play-from-hand state :corp "Eliza's Toybox" "New remote")
    (let [wotan (get-ice state :rd 0)
          eliza (get-content state :remote1 0)]
      (core/rez state :corp eliza)
      (is (= 1 (:credit (get-corp))))
      (card-ability state :corp eliza 0)
      (prompt-select :corp wotan)
      (is (get-in (refresh wotan) [:rezzed]))
      (is (= 0 (:click (get-corp))) "3 clicks spent")
      (is (= 1 (:credit (get-corp))) "No credits spent"))))

(deftest encryption-protocol
  "Encryption Protocol - Trash cost of installed cards increased by 1"
  (do-game
    (new-game (default-corp [(qty "Encryption Protocol" 2)])
              (default-runner))
    (play-from-hand state :corp "Encryption Protocol" "New remote")
    (play-from-hand state :corp "Encryption Protocol" "New remote")
    (let [ep1 (get-content state :remote1 0)
          ep2 (get-content state :remote2 0)]
      (core/rez state :corp ep1)
      (core/rez state :corp ep2)
      (take-credits state :corp)
      (run-empty-server state "Server 1")
      (is (= 4 (core/trash-cost state :runner (refresh ep1)))
          "Trash cost increased to 4 by two active Encryption Protocols")
      (prompt-choice :runner "Yes") ; trash first EP
      (run-empty-server state "Server 2")
      (is (= 3 (core/trash-cost state :runner (refresh ep2)))
          "Trash cost increased to 3 by one active Encryption Protocol"))))

(deftest eve-campaign
  (do-game
    (new-game (default-corp [(qty "Eve Campaign" 1)])
              (default-runner))
    (play-from-hand state :corp "Eve Campaign" "New remote")
    (let [eve (get-content state :remote1 0)]
      (core/rez state :corp eve)
      (is (= 0 (get-in @state [:corp :credit])))
      (is (= 16 (get-in (refresh eve) [:counter])))
      (take-credits state :corp 2)
      (take-credits state :runner)
      (is (= 4 (get-in @state [:corp :credit])))
      (is (= 14 (get-in (refresh eve) [:counter]))))))

(deftest executive-boot-camp-suppress-start-of-turn
  "Executive Boot Camp - suppress the start-of-turn event on a rezzed card. Issue #1346."
  (do-game
    (new-game (default-corp [(qty "Eve Campaign" 1) (qty "Executive Boot Camp" 1)])
              (default-runner))
    (play-from-hand state :corp "Eve Campaign" "New remote")
    (play-from-hand state :corp "Executive Boot Camp" "New remote")
    (take-credits state :corp)
    (is (= 6 (:credit (get-corp))) "Corp ends turn with 6 credits")
    (let [eve (get-content state :remote1 0)
          ebc (get-content state :remote2 0)]
      (core/rez state :corp ebc)
      (take-credits state :runner)
      (is (:corp-phase-12 @state) "Corp in Step 1.2")
      (card-ability state :corp ebc 0)
      (prompt-select :corp eve)
      (is (= 2 (:credit (get-corp))) "EBC saved 1 credit on the rez of Eve")
      (is (= 16 (get-in (refresh eve) [:counter])))
      (core/end-phase-12 state :corp nil)
      (is (= 2 (:credit (get-corp))) "Corp did not gain credits from Eve")
      (is (= 16 (get-in (refresh eve) [:counter])) "Did not take counters from Eve")
      (take-credits state :corp)
      (take-credits state :runner)
      (is (not (:corp-phase-12 @state)) "With nothing to rez, EBC does not trigger Step 1.2")
      (is (= 14 (get-in (refresh eve) [:counter])) "Took counters from Eve"))))

(deftest franchise-city
  (do-game
    (new-game (default-corp [(qty "Franchise City" 1) (qty "Accelerated Beta Test" 1)])
              (default-runner))
    (play-from-hand state :corp "Franchise City" "New remote")
    (play-from-hand state :corp "Accelerated Beta Test" "New remote")
    (core/rez state :corp (get-content state :remote1 0))
    (take-credits state :corp 1)
    (run-empty-server state "Server 2")
    (prompt-choice :runner "Steal")
    (is (= 0 (count (get-in @state [:corp :servers :server2 :content]))) "Agenda was stolen")
    (is (= 2 (:agenda-point (get-runner))) "Runner stole 2 points")
    (is (= 0 (count (get-in @state [:corp :servers :server1 :content])))
        "Franchise City no longer installed")
    (is (find-card "Franchise City" (:scored (get-corp))) "Franchise City in corp scored area")
    (is (= 1 (:agenda-point (get-corp))) "Corp has 1 point")))

(deftest genetics-pavilion
  "Genetics Pavilion - Limit Runner to 2 draws per turn, but only during Runner's turn"
  (do-game
    (new-game (default-corp [(qty "Genetics Pavilion" 1)])
              (default-runner [(qty "Diesel" 1) (qty "Sure Gamble" 3) (qty "Sports Hopper" 1)]))
    (play-from-hand state :corp "Genetics Pavilion" "New remote")
    (let [gp (get-content state :remote1 0)]
      (take-credits state :corp)
      (core/rez state :corp gp)
      (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
      (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
      (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
      (play-from-hand state :runner "Sports Hopper")
      (play-from-hand state :runner "Diesel")
      (is (= 2 (count (:hand (get-runner)))) "Drew only 2 cards because of Genetics Pavilion")
      (take-credits state :runner)
      (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
      (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
      (let [hopper (get-in @state [:runner :rig :hardware 0])]
        (card-ability state :runner hopper 0)
        (is (= 3 (count (:hand (get-runner)))) "Able to draw 3 cards during Corp's turn")
        (core/derez state :corp (refresh gp))
        (take-credits state :corp)
        (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
        (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
        (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
        (core/move state :runner (find-card "Diesel" (:discard (get-runner))) :hand)
        (is (= 1 (count (:hand (get-runner)))))
        (play-from-hand state :runner "Diesel")
        (is (= 3 (count (:hand (get-runner)))) "Drew 3 cards with Diesel")
        (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
        (core/rez state :corp (refresh gp))
        (core/draw state :runner)
        (is (= 2 (count (:hand (get-runner)))) "No card drawn; GP counts cards drawn prior to rez")))))

(deftest genetics-pavilion-fisk-investment
  (do-game
    (new-game (default-corp [(qty "Genetics Pavilion" 1) (qty "Hedge Fund" 3)])
              (default-runner [(qty "Fisk Investment Seminar" 1) (qty "Sure Gamble" 3)]))
    (play-from-hand state :corp "Genetics Pavilion" "New remote")
    (let [gp (get-content state :remote1 0)]
      (take-credits state :corp)
      (core/rez state :corp gp)
      (core/move state :corp (find-card "Hedge Fund" (:hand (get-corp))) :deck)
      (core/move state :corp (find-card "Hedge Fund" (:hand (get-corp))) :deck)
      (core/move state :corp (find-card "Hedge Fund" (:hand (get-corp))) :deck)
      (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
      (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
      (core/move state :runner (find-card "Sure Gamble" (:hand (get-runner))) :deck)
      (is (= 1 (count (:hand (get-runner)))))
      (is (= 0 (count (:hand (get-corp)))))
      (play-from-hand state :runner "Fisk Investment Seminar")
      (is (= 2 (count (:hand (get-runner)))) "Drew only 2 cards because of Genetics Pavilion")
      (is (= 3 (count (:hand (get-corp)))) "Drew all 3 cards"))))

(deftest ghost-branch
  "Ghost Branch - Advanceable; give the Runner tags equal to advancements when accessed"
  (do-game
    (new-game (default-corp [(qty "Ghost Branch" 1)])
              (default-runner))
    (play-from-hand state :corp "Ghost Branch" "New remote")
    (let [gb (get-content state :remote1 0)]
      (core/advance state :corp {:card (refresh gb)})
      (core/advance state :corp {:card (refresh gb)})
      (is (= 2 (get-in (refresh gb) [:advance-counter])))
      (take-credits state :corp)
      (run-empty-server state "Server 1")
      (prompt-choice :corp "Yes") ; choose to do the optional ability
      (is (= 2 (:tag (get-runner))) "Runner given 2 tags"))))

(deftest it-department
  "IT Department - Add strength to rezzed ICE until end of turn"
  (do-game
    (new-game (default-corp [(qty "IT Department" 1) (qty "Wall of Static" 1)])
              (default-runner))
    (play-from-hand state :corp "IT Department" "New remote")
    (play-from-hand state :corp "Wall of Static" "Server 1")
    (let [itd (get-content state :remote1 0)
          wos (get-ice state :remote1 0)]
      (core/rez state :corp itd)
      (core/rez state :corp wos)
      (card-ability state :corp itd 1)
      (is (= 0 (:click (get-corp))) "Spent 1 click")
      (is (= 1 (:counter (refresh itd))) "IT Dept has 1 counter")
      (core/add-prop state :corp (refresh itd) :counter 4)
      (is (= 5 (:counter (refresh itd))) "IT Dept has 5 counters")
      (card-ability state :corp itd 0)
      (prompt-select :corp wos)
      ;; refer to online guides for summary of how this ludicrous formula is calculated
      (is (= 8 (:current-strength (refresh wos))) "Gained 5 strength")
      (is (= 4 (:counter (refresh itd))) "Spent 1 counter")
      (card-ability state :corp itd 0)
      (prompt-select :corp wos)
      (is (= 11 (:current-strength (refresh wos))) "Gained total of 8 strength")
      (is (= 3 (:counter (refresh itd))) "Spent 1 counter")
      (card-ability state :corp itd 0)
      (prompt-select :corp wos)
      (is (= 12 (:current-strength (refresh wos))) "Gained total of 9 strength")
      (is (= 2 (:counter (refresh itd))) "Spent 1 counter")
      (card-ability state :corp itd 0)
      (prompt-select :corp wos)
      (is (= 11 (:current-strength (refresh wos))) "Gained total of 8 strength")
      (is (= 1 (:counter (refresh itd))) "Spent 1 counter")
      (take-credits state :corp)
      (is (= 3 (:current-strength (refresh wos))) "Back to default strength"))))

(deftest jackson-howard-draw
  "Jackson Howard - Draw 2 cards"
  (do-game
    (new-game (default-corp [(qty "Jackson Howard" 3)
                             (qty "Hedge Fund" 3)
                             (qty "Restructure" 2)])
              (default-runner))
    ;; guaranteed to be at least 1 jhow in hand after draw, and 2 cards in R&D
    (play-from-hand state :corp "Jackson Howard" "New remote")
    (let [jhow (get-content state :remote1 0)]
      (core/rez state :corp jhow)
      (is (= 5 (count (:hand (get-corp)))))
      (is (= 2 (:click (get-corp))))
      (card-ability state :corp jhow 0)
      (is (= 7 (count (:hand (get-corp)))) "Drew 2 cards")
      (is (= 1 (:click (get-corp)))))))

(deftest launch-campaign
  (do-game
    (new-game (default-corp [(qty "Launch Campaign" 1)])
              (default-runner))
    (play-from-hand state :corp "Launch Campaign" "New remote")
    (let [launch (get-content state :remote1 0)]
      (core/rez state :corp launch)
      (is (= 4 (get-in @state [:corp :credit])))
      (is (= 6 (get-in (refresh launch) [:counter])))
      (take-credits state :corp 2)
      (take-credits state :runner)
      (is (= 8 (get-in @state [:corp :credit])))
      (is (= 4 (get-in (refresh launch) [:counter]))))))

(deftest mark-yale
  "Mark Yale - Spend agenda counters or trash himself to gain credits"
  (do-game
    (new-game (default-corp [(qty "Firmware Updates" 1) (qty "Mark Yale" 1)])
              (default-runner))
    (play-from-hand state :corp "Firmware Updates" "New remote")
    (play-from-hand state :corp "Mark Yale" "New remote")
    (let [firm (get-content state :remote1 0)
          yale (get-content state :remote2 0)]
      (score-agenda state :corp firm)
      (core/rez state :corp yale)
      (let [firmscored (get-in @state [:corp :scored 0])]
        (is (= 3 (get-in firmscored [:counter])))
        (card-ability state :corp yale 1)
        (prompt-select :corp firmscored)
        (is (= 7 (:credit (get-corp))) "Gained 3 credits")
        (card-ability state :corp yale 0)
        (is (= 9 (:credit (get-corp))) "Gained 2 credits")
        (is (= 1 (count (:discard (get-corp)))) "Mark Yale trashed")))))

(deftest mental-health-clinic
  "Mental Health Clinic - Gain 1 credit when turn begins; Runner max hand size increased by 1"
  (do-game
    (new-game (default-corp [(qty "Mental Health Clinic" 1)])
              (default-runner))
    (play-from-hand state :corp "Mental Health Clinic" "New remote")
    (let [mhc (get-content state :remote1 0)]
      (core/rez state :corp mhc)
      (is (= 6 (core/hand-size state :runner)) "Runner max hand size increased by 1")
      (take-credits state :corp)
      (take-credits state :runner)
      (is (= 8 (:credit (get-corp))) "Gained 1 credit at start of turn"))))

(deftest net-police
  "Net Police - Recurring credits equal to Runner's link"
  (do-game
    (new-game
      (default-corp [(qty "Net Police" 1)])
      (make-deck "Sunny Lebeau: Security Specialist" [(qty "Dyson Mem Chip" 1)
                                                      (qty "Access to Globalsec" 1)]))
    (play-from-hand state :corp "Net Police" "New remote")
    (is (= 2 (:link (get-runner))))
    (let [netpol (get-content state :remote1 0)]
      (core/rez state :corp netpol)
      (is (= 2 (:rec-counter (refresh netpol))) "2 recurring for Runner's 2 link")
      (take-credits state :corp)
      (play-from-hand state :runner "Dyson Mem Chip")
      (take-credits state :runner)
      (is (= 3 (:rec-counter (refresh netpol))) "3 recurring for Runner's 3 link")
      (take-credits state :corp)
      (play-from-hand state :runner "Access to Globalsec")
      (take-credits state :runner)
      (is (= 4 (:rec-counter (refresh netpol))) "4 recurring for Runner's 4 link"))))

(deftest political-dealings
  (do-game
    (new-game (default-corp [(qty "Political Dealings" 1) (qty "Medical Breakthrough" 1) (qty "Oaktown Renovation" 1)])
              (default-runner))
    (core/move state :corp (find-card "Medical Breakthrough" (:hand (get-corp))) :deck)
    (core/move state :corp (find-card "Oaktown Renovation" (:hand (get-corp))) :deck)
    (play-from-hand state :corp "Political Dealings" "New remote")
    (is (= "Political Dealings" (:title (get-content state :remote1 0))) "Political Dealings installed")
    (let [pd (get-content state :remote1 0)]
      (core/rez state :corp (refresh pd))
      (core/draw state :corp)
      (let [mb (first (:hand (get-corp)))]
        (card-ability state :corp pd 0)
        (prompt-choice :corp mb)
        (prompt-choice :corp "New remote")
        (is (= "Medical Breakthrough" (:title (get-content state :remote2 0)))
            "Medical Breakthrough installed by Political Dealings")
        (core/draw state :corp)
        (let [oak (first (:hand (get-corp)))]
          (card-ability state :corp pd 0)
          (prompt-choice :corp oak)
          (prompt-choice :corp "New remote")
          (is (= "Oaktown Renovation" (:title (get-content state :remote3 0)))
              "Oaktown Renovation installed by Political Dealings")
          (is (= true (:rezzed (get-content state :remote3 0)))
              "Oaktown Renovation installed face up"))))))

(deftest psychic-field
  "Psychic Field - Do 1 net damage for every card in Runner's hand when accessed/exposed"
  (do-game
    (new-game (default-corp [(qty "Psychic Field" 2)])
              (default-runner [(qty "Infiltration" 3) (qty "Sure Gamble" 3)]))
    (play-from-hand state :corp "Psychic Field" "New remote")
    (play-from-hand state :corp "Psychic Field" "New remote")
    (let [psyf1 (get-content state :remote1 0)
          psyf2 (get-content state :remote2 0)]
      (take-credits state :corp)
      (starting-hand state :runner ["Infiltration" "Sure Gamble" "Sure Gamble"])
      (play-from-hand state :runner "Infiltration")
      (prompt-choice :runner "Expose a card")
      (prompt-select :runner psyf1)
      (is (= 2 (count (:hand (get-runner)))))
      (prompt-choice :corp "2 [Credits]")
      (prompt-choice :runner "0 [Credits]")
      (is (= 3 (count (:discard (get-runner)))) "Suffered 2 net damage on expose and psi loss")
      (core/gain state :runner :click 3)
      (core/draw state :runner 3)
      (is (= 3 (count (:hand (get-runner)))))
      (run-empty-server state :remote2)
      (prompt-choice :corp "1 [Credits]")
      (prompt-choice :runner "0 [Credits]")
      (is (= 6 (count (:discard (get-runner)))) "Suffered 3 net damage on access and psi loss"))))

(deftest public-support
  "Public support scoring and trashing"
  ;; TODO could also test for NOT triggering "when scored" events
  (do-game
    (new-game (default-corp [(qty "Public Support" 2)])
              (default-runner))
    ;; Corp turn 1, install and rez public supports
    (play-from-hand state :corp "Public Support" "New remote")
    (play-from-hand state :corp "Public Support" "New remote")
    (let [publics1 (get-content state :remote1 0)
          publics2 (get-content state :remote2 0)]
      (core/rez state :corp (refresh publics1))
      (core/rez state :corp (refresh publics2))
      (take-credits state :corp)

      ;; Runner turn 1, creds
      (is (= 2 (:credit (get-corp))))
      (is (= 3 (get-in (refresh publics1) [:counter])))
      (take-credits state :runner)

      ;; Corp turn 2, creds, check if supports are ticking
      (is (= 2 (get-in (refresh publics1) [:counter])))
      (is (= 0 (:agenda-point (get-corp))))
      (is (nil? (:agendapoints (refresh publics1))))
      (take-credits state :corp)

      ;; Runner turn 2, run and trash publics2
      (run-empty-server state "Server 2")
      (prompt-choice :runner "Yes") ; pay to trash
      (is (= 5 (:credit (get-runner))))
      (take-credits state :runner)

      ;; Corp turn 3, check how publics1 is doing
      (is (= 1 (get-in (refresh publics1) [:counter])))
      (is (= 0 (:agenda-point (get-corp))))
      (take-credits state :corp)

      ;; Runner turn 3, boring
      (take-credits state :runner)

      ;; Corp turn 4, check the delicious agenda points
      (let [scored-pub (get-in @state [:corp :scored 0])]
        (is (= 1 (:agenda-point (get-corp))) "Gained 1 agenda point")
        (is (= "Public Support" (:title scored-pub)))
        (is (= 1 (:agendapoints scored-pub)))))))

(deftest reality-threedee
  "Reality Threedee - Take 1 bad pub on rez; gain 1c at turn start (2c if Runner tagged)"
  (do-game
    (new-game (default-corp [(qty "Reality Threedee" 1)])
              (default-runner))
    (play-from-hand state :corp "Reality Threedee" "New remote")
    (let [r3d (get-content state :remote1 0)]
      (core/rez state :corp r3d)
      (is (= 1 (:bad-publicity (get-corp))) "Took 1 bad pub on rez")
      (take-credits state :corp)
      (take-credits state :runner)
      (is (= 8 (:credit (get-corp))) "Gained 1 credit")
      (take-credits state :corp)
      (core/gain state :runner :tag 1)
      (take-credits state :runner)
      (is (= 13 (:credit (get-corp))) "Gained 2 credits because Runner is tagged"))))

(deftest reversed-accounts
  "Reversed Accounts - Trash to make Runner lose 4 credits per advancement"
  (do-game
    (new-game (default-corp [(qty "Reversed Accounts" 1)])
              (default-runner))
    (play-from-hand state :corp "Reversed Accounts" "New remote")
    (let [rev (get-content state :remote1 0)]
      (core/advance state :corp {:card (refresh rev)})
      (core/advance state :corp {:card (refresh rev)})
      (take-credits state :corp)
      (play-from-hand state :runner "Sure Gamble")
      (play-from-hand state :runner "Sure Gamble")
      (play-from-hand state :runner "Sure Gamble")
      (take-credits state :runner)
      (is (= 18 (:credit (get-runner))))
      (core/advance state :corp {:card (refresh rev)})
      (core/advance state :corp {:card (refresh rev)})
      (is (= 4 (:advance-counter (refresh rev))))
      (core/rez state :corp (refresh rev))
      (card-ability state :corp rev 0)
      (is (= 1 (count (:discard (get-corp)))) "Reversed Accounts trashed")
      (is (= 2 (:credit (get-runner))) "Runner lost 16 credits"))))

(deftest ronald-five
  "Ronald Five - Runner loses a click every time they trash a Corp card"
  (do-game
    (new-game (default-corp [(qty "Ronald Five" 1) (qty "Melange Mining Corp." 1)])
              (default-runner))
    (play-from-hand state :corp "Ronald Five" "New remote")
    (play-from-hand state :corp "Melange Mining Corp." "New remote")
    (take-credits state :corp)
    (core/rez state :corp (get-content state :remote1 0))
    (run-empty-server state :remote2)
    (prompt-choice :runner "Yes") ; trash MMC
    (is (= 2 (:click (get-runner))) "Lost 1 click")
    (run-empty-server state :remote1)
    (prompt-choice :runner "Yes") ; trash Ronald Five
    (is (= 0 (:click (get-runner))) "Lost 1 click")))

(deftest ronin
  "Ronin - Click-trash to do 3 net damage when it has 4 or more advancements"
  (do-game
    (new-game (default-corp [(qty "Ronin" 1) (qty "Mushin No Shin" 1)])
              (default-runner))
    (play-from-hand state :corp "Mushin No Shin")
    (prompt-select :corp (find-card "Ronin" (:hand (get-corp))))
    (let [ron (get-content state :remote1 0)]
      (is (= 3 (:advance-counter (refresh ron))))
      (core/rez state :corp (refresh ron))
      (card-ability state :corp ron 0)
      (is (= 3 (count (:hand (get-runner))))
          "Ronin ability didn't fire with only 3 advancements")
      (take-credits state :corp)
      (take-credits state :runner)
      (core/advance state :corp {:card (refresh ron)})
      (is (= 4 (:advance-counter (refresh ron))))
      (card-ability state :corp ron 0)
      (is (= 3 (count (:discard (get-runner)))) "Ronin did 3 net damage")
      (is (= 2 (count (:discard (get-corp)))) "Ronin trashed"))))

(deftest sealed-vault
  "Sealed Vault - Store credits for 1c, retrieve credits by trashing or spending click"
  (do-game
    (new-game (default-corp [(qty "Sealed Vault" 1) (qty "Hedge Fund" 1)])
              (default-runner))
    (play-from-hand state :corp "Sealed Vault" "New remote")
    (play-from-hand state :corp "Hedge Fund")
    (let [sv (get-content state :remote1 0)]
      (core/rez state :corp sv)
      (card-ability state :corp sv 0)
      (prompt-choice :corp 8)
      (is (= 8 (:counter (refresh sv))) "8 credits stored on Sealed Vault")
      (is (= 0 (:credit (get-corp))))
      (card-ability state :corp sv 1)
      (prompt-choice :corp 8)
      (is (= 0 (:counter (refresh sv))) "Credits removed from Sealed Vault")
      (is (= 8 (:credit (get-corp))))
      (is (= 0 (:click (get-corp))) "Spent a click")
      (card-ability state :corp sv 0)
      (prompt-choice :corp 7)
      (is (= 7 (:counter (refresh sv))) "7 credits stored on Sealed Vault")
      (is (= 0 (:credit (get-corp))))
      (card-ability state :corp sv 2)
      (prompt-choice :corp 7)
      (is (= 7 (:credit (get-corp))))
      (is (= 2 (count (:discard (get-corp)))) "Sealed Vault trashed"))))

(deftest server-diagnostics
  "Server Diagnostics - Gain 2c when turn begins; trashed when ICE is installed"
  (do-game
    (new-game (default-corp [(qty "Server Diagnostics" 1) (qty "Pup" 1)
                             (qty "Launch Campaign" 1)])
              (default-runner))
    (play-from-hand state :corp "Server Diagnostics" "New remote")
    (core/rez state :corp (get-content state :remote1 0))
    (play-from-hand state :corp "Launch Campaign" "New remote")
    (is (= 1 (count (get-content state :remote1))) "Non-ICE install didn't trash Serv Diag")
    (take-credits state :corp)
    (take-credits state :runner)
    (is (= 5 (:credit (get-corp))) "Gained 2c at start of turn")
    (play-from-hand state :corp "Pup" "HQ")
    (is (= 1 (count (:discard (get-corp)))) "Server Diagnostics trashed by ICE install")))

(deftest snare-cant-afford
  "Snare! - Can't afford"
  (do-game
    (new-game (default-corp [(qty "Snare!" 1)])
              (default-runner [(qty "Sure Gamble" 3) (qty "Diesel" 3)]))
    (play-from-hand state :corp "Snare!" "New remote")
    (take-credits state :corp)
    (core/lose state :corp :credit 7)
    (run-empty-server state "Server 1")
    (is (= :waiting (-> @state :runner :prompt first :prompt-type))
        "Runner has prompt to wait for Snare!")
    (prompt-choice :corp "Yes")
    (is (= 0 (:tag (get-runner))) "Runner has 0 tags")
    (prompt-choice :runner "Yes")
    (is (empty? (:prompt (get-runner))) "Runner waiting prompt is cleared")
    (is (= 0 (count (:discard (get-runner)))) "Runner took no damage")))

(deftest snare-dedicated-response-team
  "Snare! - with Dedicated Response Team"
  (do-game
    (new-game (default-corp [(qty "Snare!" 1) (qty "Dedicated Response Team" 1)])
              (default-runner [(qty "Sure Gamble" 3) (qty "Diesel" 3)]))
    (play-from-hand state :corp "Snare!" "New remote")
    (play-from-hand state :corp "Dedicated Response Team" "New remote")
    (core/gain state :corp :click 1 :credit 4)
    (let [drt (get-content state :remote2 0)]
      (take-credits state :corp)
      (run-on state "Server 1")
      (core/rez state :corp drt)
      (run-successful state)
      (is (= :waiting (-> @state :runner :prompt first :prompt-type))
          "Runner has prompt to wait for Snare!")
      (prompt-choice :corp "Yes")
      (is (= 1 (:tag (get-runner))) "Runner has 1 tag")
      (prompt-choice :runner "Yes")
      (is (= 5 (count (:discard (get-runner)))) "Runner took 5 damage"))))

(deftest sundew
  "Sundew"
  (do-game
    (new-game (default-corp [(qty "Sundew" 1)])
              (default-runner))
    (play-from-hand state :corp "Sundew" "New remote")
    (let [sund (get-content state :remote1 0)]
      (core/rez state :corp sund)
      (take-credits state :corp 2)
      (is (= 5 (:credit (get-corp))) "Cost 2cr to rez")
      ;; spend a click not on a run
      (take-credits state :runner)
      (is (= 7 (:credit (get-corp))) "Corp gained 2cr from Sundew")
      (take-credits state :corp)
      (run-on state "Server 1")
      (is (= 10 (:credit (get-corp))) "Corp did not gain 2cr from run on Sundew")
      (is (= 3 (:click (get-runner))) "Runner spent 1 click to start run"))))

;(deftest sundew-dirty-laundry
;  "Sundew - Dirty Laundry"
;  (do-game
;    (new-game (default-corp [(qty "Sundew" 1)])
;              (default-runner [(qty "Dirty Laundry" 1)]))
;    (play-from-hand state :corp "Sundew" "New remote")
;    (let [sund (first (get-in @state [:corp :servers :remote1 :content]))]
;      (core/rez state :corp sund)
;      (take-credits state :corp 2)
;      (is (= 5 (:credit (get-corp))) "Cost 2cr to rez")
;      ; spend a click on a run through a card, not through click-run.
;      (play-run-event state (find-card "Dirty Laundry" (:hand (get-runner))) :remote1)
;      (is (= 5 (:credit (get-corp))) "Corp did not gain 2cr from run on Sundew"))))

(deftest team-sponsorship-hq
  "Team Sponsorship - Install from HQ"
  (do-game
    (new-game (default-corp [(qty "Domestic Sleepers" 1)
                             (qty "Team Sponsorship" 1)
                             (qty "Adonis Campaign" 1)])
              (default-runner))
    (play-from-hand state :corp "Team Sponsorship" "New remote")
    (play-from-hand state :corp "Domestic Sleepers" "New remote")
    (let [ag1 (get-content state :remote2 0)
          tsp (get-content state :remote1 0)]
      (core/rez state :corp tsp)
      (score-agenda state :corp ag1)
      (is (= (:ts-active (refresh tsp)) true) "Team Sponsorship ability enabled")
      (card-ability state :corp tsp 0)
      (prompt-select :corp (find-card "Adonis Campaign" (:hand (get-corp))))
      (prompt-choice :corp "New remote")
      (is (= "Adonis Campaign" (:title (get-content state :remote3 0)))
          "Adonis installed by Team Sponsorship")
      (is (nil? (find-card "Adonis Campaign" (:hand (get-corp)))) "No Adonis in hand")
      (is (nil? (:ts-active (refresh tsp))) "Team Sponsorship ability disabled"))))

(deftest team-sponsorship-archives
  "Team Sponsorship - Install from Archives"
  (do-game
    (new-game (default-corp [(qty "Domestic Sleepers" 1)
                             (qty "Team Sponsorship" 1)
                             (qty "Adonis Campaign" 1)])
              (default-runner))
    (play-from-hand state :corp "Team Sponsorship" "New remote")
    (play-from-hand state :corp "Domestic Sleepers" "New remote")
    (trash-from-hand state :corp "Adonis Campaign")
    (let [ag1 (get-content state :remote2 0)
          tsp (get-content state :remote1 0)]
      (core/rez state :corp tsp)
      (score-agenda state :corp ag1)
      (is (= (:ts-active (refresh tsp)) true) "Team Sponsorship ability enabled")
      (card-ability state :corp tsp 0)
      (prompt-select :corp (find-card "Adonis Campaign" (:discard (get-corp))))
      (prompt-choice :corp "New remote")
      (is (= "Adonis Campaign" (:title (get-content state :remote3 0)))
          "Adonis installed by Team Sponsorship")
      (is (nil? (find-card "Adonis Campaign" (:discard (get-corp)))) "No Adonis in discard")
      (is (nil? (:ts-active (refresh tsp))) "Team Sponsorship ability disabled"))))

(deftest team-sponsorship-multiple
  "Team Sponsorship - Multiple installed"
  (do-game
    (new-game (default-corp [(qty "Domestic Sleepers" 1)
                             (qty "Team Sponsorship" 2)
                             (qty "Adonis Campaign" 2)])
              (default-runner))
    (play-from-hand state :corp "Team Sponsorship" "New remote")
    (play-from-hand state :corp "Team Sponsorship" "New remote")
    (play-from-hand state :corp "Domestic Sleepers" "New remote")
    (trash-from-hand state :corp "Adonis Campaign")
    (let [ag1 (get-content state :remote3 0)
          tsp2 (get-content state :remote2 0)
          tsp1 (get-content state :remote1 0)]
      (core/rez state :corp tsp1)
      (core/rez state :corp tsp2)
      (score-agenda state :corp ag1)
      (is (= (:ts-active (refresh tsp1)) true) "Team Sponsorship 1 ability enabled")
      (is (= (:ts-active (refresh tsp2)) true) "Team Sponsorship 2 ability enabled")
      (card-ability state :corp tsp1 0)
      (prompt-select :corp (find-card "Adonis Campaign" (:discard (get-corp))))
      (prompt-choice :corp "New remote")
      (card-ability state :corp tsp2 0)
      (prompt-select :corp (find-card "Adonis Campaign" (:hand (get-corp))))
      (prompt-choice :corp "New remote")
      (is (= "Adonis Campaign" (:title (get-content state :remote4 0)))
          "Adonis installed by Team Sponsorship")
      (is (= "Adonis Campaign" (:title (get-content state :remote5 0)))
          "Adonis installed by Team Sponsorship")
      (is (nil? (:ts-active (refresh tsp1))) "Team Sponsorship 1 ability disabled")
      (is (nil? (:ts-active (refresh tsp2))) "Team Sponsorship 2 ability disabled"))))

(deftest the-root
  "The Root - recurring credits refill at Step 1.2"
  (do-game
    (new-game (make-deck "Blue Sun: Powering the Future" [(qty "The Root" 1)])
              (default-runner))
    (play-from-hand state :corp "The Root" "New remote")
    (core/gain state :corp :credit 6)
    (let [root (get-content state :remote1 0)]
      (core/rez state :corp root)
      (card-ability state :corp (refresh root) 0)
      (is (= 2 (:rec-counter (refresh root))) "Took 1 credit from The Root")
       (is (= 6 (:credit (get-corp))) "Corp took Root credit into credit pool")
      (take-credits state :corp)
      (take-credits state :runner)
      ; we expect Step 1.2 to have triggered because of Blue Sun
      (is (:corp-phase-12 @state) "Corp is in Step 1.2")
      (is (= 3 (:rec-counter (refresh root))) "Recurring credits were refilled before Step 1.2 window"))))

(deftest toshiyuki-sakai
  "Toshiyuki Sakai - Swap with an asset/agenda from HQ; Runner can choose to access new card or not"
  (do-game
    (new-game (default-corp [(qty "Toshiyuki Sakai" 1) (qty "Project Junebug" 1) (qty "Hedge Fund" 1)])
              (default-runner [(qty "Sure Gamble" 3) (qty "Easy Mark" 2)]))
    (play-from-hand state :corp "Toshiyuki Sakai" "New remote")
    (let [toshi (get-content state :remote1 0)]
      (core/advance state :corp {:card (refresh toshi)})
      (core/advance state :corp {:card (refresh toshi)})
      (take-credits state :corp)
      (is (= 2 (:advance-counter (refresh toshi))) "Toshiyuki has 2 advancements")
      (run-empty-server state "Server 1")
      (is (= :waiting (-> @state :runner :prompt first :prompt-type))
          "Runner has prompt to wait for Toshiyuki")
      (prompt-choice :corp "Yes") ; choose to do a swap
      (prompt-select :corp (find-card "Hedge Fund" (:hand (get-corp))))
      (is (= (refresh toshi) (get-content state :remote1 0)) "Toshiyuki still in remote; can't target an operation in hand")
      (prompt-select :corp (find-card "Project Junebug" (:hand (get-corp))))
      (let [june (get-content state :remote1 0)]
        (is (= "Project Junebug" (:title (refresh june))) "Project Junebug swapped into Server 1")
        (is (= 2 (:advance-counter (refresh june))) "Project Junebug has 2 advancements")
        (prompt-choice :runner "Yes") ; choose to access new card
        (prompt-choice :corp "Yes") ; pay 1c to fire Junebug
        (is (= 4 (count (:discard (get-runner)))) "Runner took 4 net damage")))))

(deftest turtlebacks
  "Turtlebacks - Gain 1 credit for every new server created"
  (do-game
    (new-game (default-corp [(qty "Turtlebacks" 1) (qty "PAD Campaign" 2) (qty "Wraparound" 1)])
              (default-runner))
    (core/gain state :corp :click 1)
    (play-from-hand state :corp "Turtlebacks" "New remote")
    (let [tb (get-content state :remote1 0)]
      (core/rez state :corp tb)
      (play-from-hand state :corp "PAD Campaign" "New remote")
      (is (= 4 (:credit (get-corp))) "Gained 1 credit for new server created")
      (play-from-hand state :corp "Wraparound" "Server 1")
      (is (= 4 (:credit (get-corp))) "No credit gained for install into existing server")
      (play-from-hand state :corp "PAD Campaign" "New remote")
      (is (= 5 (:credit (get-corp))) "Gained 1 credit for new server created"))))
