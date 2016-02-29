(ns mbuczko.category.tree-test
  (:require [mbuczko.category.tree :refer :all]
            [midje.sweet :refer :all])
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
   {:path "/car/Tarpan" :props {:has-abs {:sticky true}}},
   {:path "/car/Acura"  :props {:has-abs {:sticky true}}}
   {:path "/car/BMW"    :props {:has-xenons {:sticky true}}}
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
        (-> tree :subcategories (first) :path) => "/car"))

(fact "finds correct category basing on provided path"
      (with-tree (create-tree categories)
        (let [node (lookup "/car/BMW/Serie X")]
          (:path node)) => "/car/BMW/Serie X"))

(fact "finds no category if category does not exist in tree"
      (with-tree (create-tree categories)
        (let [node (lookup "/car/BMW/Serie XXX")]
          node => nil)))

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

(fact "newly created tree should be persistent"
      (with-tree (create-tree categories)
        (create-category (map->Category {:path "/car/BMW/Serie 5"}))
        (get-at "category:car:BMW:Serie 5") => truthy))

(fact "removed node should be persistently deleted"
      (with-tree (create-tree categories)
        (let [bmw5 (map->Category {:path "/car/BMW/Serie 5"})
              tree (create-category bmw5)]

          (get-at "category:car:BMW:Serie 5") => truthy

          (with-tree tree
            (remove-at "/car/BMW/Serie 5")
            (get-at "category:car:BMW:Serie 5") => falsey))))
