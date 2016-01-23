(ns categorizer.examples.tree
  (:require
   [mbuczko.category.tree]))

;; Category tree is a crucial structure required by all functions to work as expected.
;; To make things easy enough categorizer exposes single function ```create-tree``` which
;; produces category tree based on flat list of maps. Each map should consist of at least 2
;; keys: ```path``` and ```params```. First one describes a path in a way that operating systems
;; describe directories, ie. by nodes separated with a slash. There is also a "root" path (single slash)
;; which all subsequent nodes derive from.
;;
;; Second key - ```params``` describes map of properties assigned to given node, eg. ```{:price {:required true :sticky true}}```
;; Each property may contain one of 2 special flags: ```sticky``` and/or ```excluded``` which decide whether property gets inherited/excluded
;; down the tree.
(def categories
  [{:path "/"
    :params {:has-alarm {:sticky true}}}
   {:path "/car"
    :params {:status  {:sticky true :values ["active" "inactive"]}
             :has-led {:sticky true}
             :has-abs {:sticky true}
             :has-gps {:sticky true}}}
   {:path "/car/Tarpan"
    :params {:has-abs {:excluded true}
             :has-gps {:excluded true}}},
   {:path "/car/Acura"
    :params {:has-asr   {:sticky true}
             :has-alarm {:excluded true}}}
   {:path "/car/BMW"
    :params {:has-xenons {:sticky true}}}
   {:path "/car/BMW/Serie X"
    :params {:has-xenons {:sticky true :excluded true}
             :has-airbag {:is-standard true}}}
   {:path "/car/BMW/Serie X/X3"
    :params {:has-sunroof {:is-standard true}
             :has-trailer {:excluded true}}}])

;; Creating a tree is trivial - ```create-tree``` function takes care of details. This function expects
;; a structure as the one prepared earlier and returns a tree which can be used in further queries.
(create-tree categories)

;; All queries have identical form: tree wrapped into ```with-tree``` macro is passed down as bound variable
;; to ```lookup``` function which traverses the tree looking for specified node and its parameters.
(with-tree (create-tree categories)
  (lookup "/car"))

;; Depending on sticky/excluded flags we should get a node (if found) together with combined parameters. Look at the ```has-alarm``` parameter. It's defined
;; at the top of tree additionally marked as sticky, which means it should be inherited down the tree. Querying for "/car" shows that ```has-alarm``` indeed
;; gets added to the list of calculated parameters.
{:path "/car"
 :params {:status {:values ["active" "inactive"]},
          :has-alarm {},
          :has-led {},
          :has-abs {},
          :has-gps {}}}

;; Acura happens to have alarm missing in our tree (it's been explicite excluded).
(with-tree (create-tree categories)
  (lookup "/car/Acura"))

;; Querying for "/car/Acura" should then return no ```has-alarm``` parameter.
{:path "/car/Acura",
 :params {:status {:values ["active" "inactive"]},
          :has-led {},
          :has-abs {},
          :has-gps {},
          :has-asr {}}}
