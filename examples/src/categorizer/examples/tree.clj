(ns categorizer.examples.tree
  (:require
   [mbuczko.category.tree]))

;; Category tree is a crucial structure required by all functions to work as expected.
;; To make things easy enough categorizer exposes single function ```create-tree``` which
;; produces category tree based on flat list of maps. Each map should consist of at least 2
;; keys: ```path``` and ```props```. First one describes a path in a way that operating systems
;; describe directories, ie. by nodes separated with a slash. There is also a "root" path (single slash)
;; which all subsequent nodes derive from.
;;
;; Second key - ```props``` describes map of properties assigned to given node, eg. ```{:price {:required true :sticky true}}```
;; Each property may contain one of 2 special flags: ```sticky``` and/or ```excluded``` which decide whether property gets inherited/excluded
;; down the tree.
(def categories
  [{:path "/" :props {:has-alarm {:sticky true}}}
   {:path "/car"
    :props {:status  {:sticky true :values ["active" "inactive"]}
            :has-led {:sticky true}
            :has-abs {:sticky true}
            :has-gps {:sticky true}}}
   {:path "/car/Tarpan"
    :props {:has-abs {:excluded true}
            :has-gps {:excluded true}}},
   {:path "/car/Acura"
    :props {:has-asr   {:sticky true}
            :has-alarm {:excluded true}}}
   {:path "/car/BMW"
    :props {:has-xenons {:sticky true}}}
   {:path "/car/BMW/Serie X"
    :props {:has-xenons {:sticky true :excluded true}
            :has-airbag {:is-standard true}}}
   {:path "/car/BMW/Serie X/X3"
    :props {:has-sunroof {:is-standard true}
            :has-trailer {:excluded true}}}])

;; Creating a tree is trivial - ```create-tree``` function takes care of details. This function expects
;; a structure as the one prepared earlier and returns a tree which can be used in further queries.
(create-tree categories)

;; Internally each node of category tree is represented by ```Category``` type which encapsulates 3 important things:
;;
;;   - category path described by ```:path``` value
;;   - map of properties described by ```:props```
;;   - vector of subcategories described by ```:subcategories```
[Category{:path "/",
          :props {:has-alarm {:sticky true}},
          :subcategories (Category{:path "/car",
                                   :props {:status  {:sticky true, :values ["active" "inactive"]},
                                           :has-led {:sticky true},
                                           :has-abs {:sticky true},
                                           :has-gps {:sticky true}},
                                   :subcategories (Category{:path "/car/Tarpan",
                                                            :props {:has-abs {:excluded true}, :has-gps {:excluded true}},
                                                            :subcategories nil}
                                                   Category{:path "/car/Acura",
                                                            :props {:has-asr {:sticky true}, :has-alarm {:excluded true}},
                                                            :subcategories nil}
                                                   Category{:path "/car/BMW",
                                                            :props {:has-xenons {:sticky true}},
                                                            :subcategories (Category{:path "/car/BMW/Serie X",
                                                                                     :props {:has-xenons {:sticky true, :excluded true}, :has-airbag {:is-standard true}},
                                                                                     :subcategories (Category{:path "/car/BMW/Serie X/X3",
                                                                                                              :props {:has-sunroof {:is-standard true}, :has-trailer {:excluded true}},
                                                                                                              :subcategories nil})})})})}]

;; All queries have identical form: tree wrapped into ```with-tree``` macro is passed down as bound variable
;; to ```lookup``` function which traverses the tree looking for specified node and its properties.
(with-tree (create-tree categories)
  (lookup "/car"))

;; Depending on sticky/excluded flags we should get a node (if found) together with combined properties. Look at the ```has-alarm```property. It's defined
;; at the top of tree additionally marked as sticky, which means it should be inherited down the tree. Querying for "/car" shows that ```has-alarm``` indeed
;; gets added to the list of calculated properties.
{:path "/car"
 :status {:values ["active" "inactive"]},
 :has-alarm {},
 :has-led {},
 :has-abs {},
 :has-gps {}}

;; Acura happens to have alarm missing in our tree (it's been explicite excluded).
(with-tree (create-tree categories)
  (lookup "/car/Acura"))

;; Querying for "/car/Acura" should then return no ```has-alarm``` property.
{:path "/car/Acura",
 :status {:values ["active" "inactive"]},
 :has-led {},
 :has-abs {},
 :has-gps {},
 :has-asr {}}

;; Last but not least, there are also two other functions exposed which may become helpful when doing manipulation on tree:

;; ```create-category``` allows dynamically add new category to existing tree. It takes ```Category``` instance as parameter and returns in turn modified category tree.
(with-tree (create-tree categories)
  (create-category (Category. "/car/Fiat/126p" {:coolness 100} nil)))

;; ```remove-at``` removes category at given path with all its subcategories.
(with-tree (create-tree categories)
  (remove-at "/car/Fiat"))

;; Finally, to make tree persistent it's pretty enough to extend ```Category``` type with ```Persistent``` protocol and
;; implement 2 functions: ```store!``` and ```delete!```. In-memory implementation of trivial storage may look like this.
(def storage (atom {}))
(extend-type Category
  Persistent
  (store! [category]
    (swap! storage assoc (category->key category) (:props category)))
  (delete! [category]
    (swap! storage dissoc (category->key category))))
