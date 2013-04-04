(ns evelyn.core
  (:require [clojure.tools.logging :as log]))

(def ^{:dynamic true} swarm-size 30)

;(def swarm (atom nil))

(defrecord Dimension [tag minimum maximum])
(defrecord Particle [velocity current-value current-position best-value best-position])
(defrecord Swarm [fitness-fn dimensions particles best-particle])

(defn- v+ [& vs] (apply mapv + vs))

(defn- v- [& vs] (apply mapv - vs))

(defn- fv* [f v] (vec (map (partial * f) v)))

(defn- rand-bounded [dimension]
  (* (rand) (- (:maximum dimension) (:minimum dimension))))

(defn- get-value [fitness-fn position]
  (apply fitness-fn position))

(defn- best [particles]
  (reduce
    (fn [bp p]
      (if (or (nil? bp)
              (< 0 (:best-value p) (:best-value bp)))
        p
        bp))
    nil
    particles))

(let [acceleration-coef 0.999
      particle-position-coef 1.5
      swarm-position-coef 1.5]

  (defn- update-velocity [swarm particle]
    (let [particle-position-coef* (* (rand) particle-position-coef)
          swarm-position-coef*    (* (rand) swarm-position-coef)
          v1 (fv* acceleration-coef
                  (:velocity particle))
          v2 (fv* particle-position-coef*
                  (v- (:best-position particle)
                      (:current-position particle)))
          v3 (fv* swarm-position-coef*
                  (v- (:best-position (:best-particle swarm))
                      (:current-position particle)))]
      (v+ v1 v2 v3))))

(defn- constrain-position [dimensions position]
  (->> position
      (mapv min (map :maximum dimensions) ,,,)
      (mapv max (map :minimum dimensions) ,,,)))

(defn- update-particle [swarm particle]
  (let [new-velocity (update-velocity swarm particle)
        new-position (constrain-position
                       (:dimensions swarm)
                       (v+ (:current-position particle)
                           new-velocity))
        ;; TODO: Make sure position doesn't exceed bounds for each dimension
        new-value (get-value (:fitness-fn swarm) new-position)
        [best-value best-position] (if (< 0 new-value (:best-value particle))
                                     [new-value new-position]
                                     [(:best-value particle) (:best-position particle)])]
    ;(->Particle new-velocity new-value new-position best-value best-position)))
    (assoc particle
           :velocity new-velocity
           :current-value new-value
           :current-position new-position
           :best-value best-value
           :best-position best-position)))

(defn- particle-factory [fitness-fn dimensions]
  (let [velocity (vec (repeat (count dimensions) 0.0))
        current-position (map rand-bounded dimensions)
        current-value (get-value fitness-fn current-position)
        best-position current-position
        best-value current-value]
  (->Particle velocity current-value current-position best-value best-position)))

(defn swarm-factory [fitness-fn size dimensions]
  (let [particles (repeatedly size (partial particle-factory fitness-fn dimensions))
        best-particle (best particles)]
    (log/info "Added " (count particles) " particle(s) to swarm...")
    (->Swarm fitness-fn dimensions particles best-particle)))

(defn- update-swarm [swarm]
  (let [particles (map (partial update-particle swarm) (:particles swarm))
        best-particle (best particles)]
    (->Swarm (:fitness-fn swarm) (:dimensions swarm) particles best-particle)))
    ;(assoc swarm :particles particles :best-particle best-particle)))

(defn learn [swarm generations]
  (let [swarm* (update-swarm swarm)]
    ;(log/info swarm*)
    (log/info generations (:best-particle swarm*))
    (if (zero? generations)
      (:best-particle swarm*)
      (recur swarm* (dec generations)))))
