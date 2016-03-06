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
;;   - uuid which identifies category
;;   - map of properties described by ```:props```
;;   - vector of subcategories described by ```:subcategories```
[Category{:path "/",
          :props {:has-alarm {:sticky true}},
          :uuid "3ee61adc-1583-4579-adfb-081ebe1ac457"
          :subcategories (Category{:path "car",
                                   :uuid "115098c5-aa7d-4ad0-8ce2-80e731a9ec2e"
                                   :props {:status  {:sticky true, :values ["active" "inactive"]},
                                           :has-led {:sticky true},
                                           :has-abs {:sticky true},
                                           :has-gps {:sticky true}},
                                   :subcategories (Category{:path "Tarpan",
                                                            :uuid "7fd972f9-86ad-42d7-a7f2-5bedd85e49a8"
                                                            :props {:has-abs {:excluded true}, :has-gps {:excluded true}},
                                                            :subcategories nil}
                                                   Category{:path "Acura",
                                                            :uuid "11cda19e-7d38-464e-8f47-c88a94c64ff0"
                                                            :props {:has-asr {:sticky true}, :has-alarm {:excluded true}},
                                                            :subcategories nil}
                                                   Category{:path "BMW",
                                                            :uuid "76a257ee-0a8e-48ce-a645-a9168dde5d99"
                                                            :props {:has-xenons {:sticky true}},
                                                            :subcategories (Category{:path "Serie X",
                                                                                     :uuid "d5c7ae9c-a28d-4c77-b2dd-c02bc9064d8c"
                                                                                     :props {:has-xenons {:sticky true, :excluded true}, :has-airbag {:is-standard true}},
                                                                                     :subcategories (Category{:path "X3",
                                                                                                              :uuid "868ddede-9293-4ac8-9c61-8354c11"
                                                                                                              :props {:has-sunroof {:is-standard true}, :has-trailer {:excluded true}},
                                                                                                              :subcategories nil})})})})}]

;; All queries have identical form: tree wrapped into ```with-tree``` macro is passed down as bound variable
;; to ```lookup``` function which traverses the tree looking for specified node and its properties.
(with-tree (create-tree categories)
  (lookup "/car"))

;; Oh, btw. ```lookup``` function may have a ```:sort-by``` option which controls the order that children are returned in. It takes an argument
;; - a function which is used against each child to deduce an order.
(with-tree (create-tree categories)
  (lookup "/car" :sort-by :path))

;; Sorting may utilize ```:props``` map as well
(with-tree (create-tree categories)
  (lookup "/car" :sort-by (comp :value :price :props)))

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

;; Last but not least, there are also 3 other functions exposed which may become helpful when doing manipulation on tree:

;; ```create-category``` allows dynamically add new category to existing tree. It takes path and props as arguments and returns in turn modified category tree.
(with-tree (create-tree categories)
  (create-category "/car/Fiat/126p" {:coolness 100}))

;; ```remove-at``` removes category at given path with all its subcategories.
(with-tree (create-tree categories)
  (remove-at "/car/Fiat"))

;; ```update-at``` updates a path and (optionally) props. If path differs from the old one - category will be moved into new location along with all its children.
;; ```:props``` will be updated only if provided one is not nil. Otherwise ```:props``` remains unaltered even though the node may have changed its location within tree.
(with-tree (create-tree categories)
  (update-at "/car/BMW/Serie X" "/car/Fiat" nil))


;; Finally, to make tree persistent it's pretty enough to extend ```Category``` type with ```Persistent``` protocol and
;; implement 2 functions: ```store!``` and ```delete!```. In-memory implementation of trivial storage may look like this.
(def storage (atom {}))
(extend-type Category
  Persistent
  (store! [category]
    (swap! storage assoc (category->key category) (:props category)))
  (delete! [category]
    (swap! storage dissoc (category->key category))))
