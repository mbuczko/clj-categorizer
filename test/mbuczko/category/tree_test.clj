(ns mbuczko.category.tree-test
  (:require [mbuczko.category.tree :refer :all]
            [midje.sweet :refer :all]
            [clojure.tools.logging :as log])
  (:import mbuczko.category.tree.Category))

(declare category->key)

;; mocking persistent storage
(def storage (atom {}))

;; extending Category to make it persistent
(extend-type Category
  Persistent
  (store! [category]
    (swap! storage assoc (category->key category) true))
  (delete! [category]
    (swap! storage dissoc (category->key category))))

(def categories
  [{:path "/"    :props {:price {:sticky true :value 1}}}
   {:path "/car" :props {:status {:rules "required" :sticky true :values ["active" "inactive"]}
                          :condition {:rules "required" :sticky true :values ["broken" "functioning" "unknown"]}
                          :has-trailer {:sticky true}}}
   {:path "/car/Tarpan" :props {:has-abs {:sticky true} :price {:value 300}}},
   {:path "/car/Acura"  :props {:has-abs {:sticky true} :price {:value 200}}}
   {:path "/car/BMW"    :props {:has-xenons {:sticky true} :price {:value 500}}}
   {:path "/car/BMW/Serie X"    :props {:has-xenons {:sticky true :excluded true} :has-eds {}}}
   {:path "/car/BMW/Serie X/X3" :props {:has-sunroof {} :has-trailer {:excluded true}}}])

(defn- category->key [category]
  (str "category" (.replaceAll (:path category) "/" ":")))

(defn- get-at [path]
  "Returns node at given path stored in atom-based storage."
  (get @storage path))

(fact "creates empty category tree with correct root node"
      (let [tree (create-tree [])]
        (:path tree) => "/"
        (:subcategories tree) => falsey))

(fact "creates 1-element category tree"
      (let [tree (create-tree [{:path "car"}])
            subcategories (:subcategories tree)]
        (count subcategories) => 1
        (-> tree :subcategories (first) :path) => "car"))

(fact "creates nested category tree"
      (let [tree (create-tree [{:path "car"} {:path "/car/BMW"} {:path "/car/BMW/Serie X"}])
            subcategories (:subcategories tree)
            car (first subcategories)
            bmw (first (:subcategories car))
            serie (first (:subcategories bmw))]
        (:path car)   => "car"
        (:path bmw)   => "BMW"
        (:path serie) => "Serie X"))

(fact "finds category basing on provided path"
      (with-tree (create-tree categories)
        (let [serie "/car/BMW/Serie X"]
          (:path (lookup serie)) => serie)))

(fact "finds root path"
      (with-tree (create-tree categories)
        (let [node (lookup "/")]
          (:path node)) => "/"))

(fact "finds no category if category does not exist in tree"
      (with-tree (create-tree categories)
        (lookup "/car/BMW/Serie XXX") => nil))

(fact "gathers sticky properties for given category"
      (with-tree (create-tree categories)
        (let [props (lookup "/car/BMW/Serie X/X3")]
          (contains? props :has-sunroof) => true
          (contains? props :status) => true
          (contains? props :condition) => true
          (contains? props :price) => true)))

(fact "excludes properties marked as excluded"
      (with-tree (create-tree categories)
        (let [props (lookup "/car/BMW/Serie X")]
          (contains? props :has-xenons) => false)))

(fact "assigns correctly sticky properties when creating category tree"
      (with-tree (create-tree categories)
        (let [props (lookup "/car/Acura")]
          (contains? props :has-abs) => true)))

(fact "assigns correctly properties with no sticky/excluded flags"
      (with-tree (create-tree categories)
        (let [prop1 (lookup "/car/BMW/Serie X")
              prop2 (lookup "/car/BMW/Serie X/X3")]
          (contains? prop1 :has-eds) => true
          (contains? prop2 :has-eds) => false)))

(fact "creates category dynamicaly"
      (with-tree (create-tree categories)
        (let [honker "/car/Tarpan/Honker"]
          (with-tree (create-category honker {:abs {}})
            (let [node (lookup honker)]
              (:path node) => honker
              (:abs node)  => truthy
              (:eds node)  => falsey)))))

(fact "moves path between nodes"
      (with-tree (create-tree categories)
        (let [bmw "/car/BMW/Serie X", tarpan "/car/Tarpan/Serie X"]
          (with-tree (update-at bmw tarpan {:coolness {:value 1000}})
            (let [node (lookup tarpan), missing (lookup bmw)]
              (:path node) => tarpan
              (:coolness node) => {:value 1000}
              (:path missing => falsey))))))

(fact "edits node without touching its props"
      (with-tree (create-tree categories)
        (let [serie-x "/car/BMW/Serie X", serie-xxx "/car/BMW/Serie XXX", node (lookup serie-x)]
          (:has-eds node) => truthy

          (with-tree (update-at serie-x serie-xxx nil)
            (let [renamed (lookup serie-xxx)]
              (:has-eds renamed) => truthy)))))

(fact "removes node at given path"
      (with-tree (create-tree categories)
        (let [serie "/car/BMW/Serie X"]
          (lookup serie) => truthy

          (with-tree (remove-at serie)
            (lookup serie) => falsey))))

(fact "newly created tree should be persistent"
      (with-tree (create-tree categories)
        (create-category "/car/BMW/Serie 5" nil)
        (get-at "category:car:BMW:Serie 5") => truthy))

(fact "removed node should be persistently deleted"
      (with-tree (create-tree categories)
        (get-at "category:car:BMW:Serie 5") => truthy

        (with-tree (create-category "/car/BMW/Serie 5" nil)
          (remove-at "/car/BMW/Serie 5")
          (get-at "category:car:BMW:Serie 5") => falsey)))
