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
    (swap! storage assoc (category->key category) (:params category)))
  (delete! [category]
    (swap! storage dissoc (category->key category))))

(def categories
  [{:path "/"    :params {:price {:sticky true}}}
   {:path "/car" :params {:status {:rules "required" :sticky true :values ["active" "inactive"]}
                          :condition {:rules "required" :sticky true :values ["broken" "functioning" "unknown"]}
                          :has-trailer {:sticky true}}}
   {:path "/car/Tarpan" :params {:has-abs {:sticky true}}},
   {:path "/car/Acura"  :params {:has-abs {:sticky true}}}
   {:path "/car/BMW"    :params {:has-xenons {:sticky true}}}
   {:path "/car/BMW/Serie X"    :params {:has-xenons {:sticky true :excluded true} :has-eds {}}}
   {:path "/car/BMW/Serie X/X3" :params {:has-sunroof {} :has-trailer {:excluded true}}}])

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

(fact "gathers sticky parameters for given category"
      (with-tree (create-tree categories)
        (let [params (:params (lookup "/car/BMW/Serie X/X3"))]
          (contains? params :has-sunroof) => true
          (contains? params :status) => true
          (contains? params :condition) => true
          (contains? params :price) => true)))

(fact "excludes parameters marked as excluded"
      (with-tree (create-tree categories)
        (let [params (:params (lookup "/car/BMW/Serie X"))]
          (contains? params :has-xenons) => false)))

(fact "assigns correctly sticky parameter when creating category tree"
      (with-tree (create-tree categories)
        (let [params (:params (lookup "/car/Acura"))]
          (contains? params :has-abs) => true)))

(fact "assigns correctly parameter with no sticky/excluded flags"
      (with-tree (create-tree categories)
        (let [params1 (:params (lookup "/car/BMW/Serie X"))
              params2 (:params (lookup "/car/BMW/Serie X/X3"))]
          (contains? params1 :has-eds) => true
          (contains? params2 :has-eds) => false)))

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
