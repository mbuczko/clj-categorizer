(ns mbuczko.category.tree-test
  (:require [mbuczko.category.tree :refer :all]
            [midje.sweet :refer :all])
  (:import mbuczko.category.tree.Category))

(declare category->key)

;; mocking persistent storage
(def storage (atom {}))

;; extending Category to make it persistent
(extend-type Category
  PersistentCategory
  (store! [category]
    (swap! storage assoc (category->key category) (:props category)))
  (delete! [category]
    (swap! storage dissoc (category->key category))))

(def categories
  [{:path "/"    :props {:price {:sticky true}}}
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
  (get @storage path))

(fact "creates empty category tree with correct root node"
      (let [tree (create-tree [])]
        (:path tree) => "/"
        (:subcategories tree) => []))

(fact "creates 1-element category tree"
      (let [tree (create-tree [{:path "car"}])
            subcategories (:subcategories tree)]
        (count subcategories) => 1
        (get-in tree [:subcategories 0 :path]) => "/car"))

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
        (let [props (:props (lookup "/car/BMW/Serie X/X3"))]
          (contains? props :has-sunroof) => true
          (contains? props :status) => true
          (contains? props :condition) => true
          (contains? props :price) => true)))

(fact "excludes properties marked as excluded"
      (with-tree (create-tree categories)
        (let [props (:props (lookup "/car/BMW/Serie X"))]
          (contains? props :has-xenons) => false)))

(fact "assigns correctly sticky properties when creating category tree"
      (with-tree (create-tree categories)
        (let [props (:props (lookup "/car/Acura"))]
          (contains? props :has-abs) => true)))

(fact "assigns correctly properties with no sticky/excluded flags"
      (with-tree (create-tree categories)
        (let [prop1 (:props (lookup "/car/BMW/Serie X"))
              prop2 (:props (lookup "/car/BMW/Serie X/X3"))]
          (contains? prop1 :has-eds) => true
          (contains? prop2 :has-eds) => false)))

(fact "newly created tree should be persistent"
      (with-tree (create-tree categories)
        (let [bmw (get-at "category:car:BMW:Serie X")]
          (:has-xenons bmw) => truthy)))

(fact "removed node should be persistently deleted"
      (with-tree (create-tree categories)

        (lookup "/car/BMW/Serie X") => truthy
        (get-at "category:car:BMW:Serie X") => truthy

        (with-tree (remove-at "/car/BMW/Serie X")
          (lookup "/car/BMW/Serie X") => falsey
          (get-at "category:car:BMW:Serie X") => falsey)))
