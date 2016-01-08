(ns mbuczko.category.tree-test
  (:require [mbuczko.category.tree :refer :all]
            [midje.sweet :refer :all]))

(def categories
  [{:path "/"
    :params {:price {:type "floating" :sticky true}}}
   {:path "/car"
    :params {:status {:type "option" :rules "required" :sticky true :values ["active" "inactive"]}
             :condition {:type "option" :rules "required" :sticky true :values ["broken" "functioning" "unknown"]}
             :has-trailer {:type "bool" :sticky true}}}
   {:path "/car/Tarpan"
    :params {:has-abs {:type "bool" :sticky true}}},
   {:path "/car/Acura"
    :params {:has-abs {:type "bool" :sticky true}}}
   {:path "/car/BMW"
    :params {:has-xenons {:type "bool" :sticky true}}}
   {:path "/car/BMW/Seria X"
    :params {:has-xenons {:type "bool" :sticky true :excluded true}
             :has-eds {:type "bool"}}}
   {:path "/car/BMW/Seria X/X30"
    :params {:has-sunroof {:type "bool" :sticky true}
             :has-trailer {:type "bool" :excluded true}}}])

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
        (let [node (lookup "/car/BMW/Seria X")]
          (:path node)) => "/car/BMW/Seria X"))

(fact "finds no category if category does not exist in tree"
      (with-tree (create-tree categories)
        (let [node (lookup "/car/BMW/Seria XXX")]
          node => nil)))

(fact "gathers sticky parameters for given category"
      (with-tree (create-tree categories)
        (let [params (:params (lookup "/car/BMW/Seria X/X30"))]
          (contains? params :has-sunroof) => true
          (contains? params :status) => true
          (contains? params :condition) => true
          (contains? params :price) => true)))

(fact "excludes parameters marked as excluded"
      (with-tree (create-tree categories)
        (let [params (:params (lookup "/car/BMW/Seria X"))]
          (contains? params :has-xenons) => false)))

(fact "assigns correctly sticky parameter when creating category tree"
      (with-tree (create-tree categories)
        (let [params (:params (lookup "/car/Acura"))]
          (contains? params :has-abs) => true)))

(fact "assigns correctly parameter with no sticky/excluded flags"
      (with-tree (create-tree categories)
        (let [params1 (:params (lookup "/car/BMW/Seria X"))
              params2 (:params (lookup "/car/BMW/Seria X/X30"))]
          (contains? params1 :has-eds) => true
          (contains? params2 :has-eds) => false)))
