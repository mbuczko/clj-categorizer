(ns mbuczko.category.tree
  (:require [cheshire.core         :as json]
            [clojure.tools.logging :as log]
            [clojure.zip           :as zip]))

(defprotocol TreeNode
  (branch? [node] "Is it possible for node to have children?")
  (node-children [node] "Returns children of this node.")
  (make-node [node children] "Makes new node from existing node and new children."))

(defrecord Category [path params subcategories]
  TreeNode
  (branch? [node] true)
  (node-children [node] (:subcategories node))
  (make-node [node children] (Category. (:path node) (:params node) (vec children))))

(def ^:dynamic *categories-tree*
  (atom (map->Category {:path "/" :params {} :subcategories []})))

(defn- tree-zip
  "Makes a zipper out of a tree."
  [root]
  (zip/zipper branch? node-children make-node root))

(defn- find-child-node
  "Looks for a node described by given path among direct children of loc."
  [loc path]
  (loop [node (zip/down loc)]
    (if-not (nil? node)
      (if (= path (:path (zip/node node))) node (recur (zip/right node))))))

(defn- create-child-node
  "Inserts a node with given path as a rightmost child at loc.
  Moves location to newly inserted child."
  [loc path]
  (let [node (Category. path {} [])]
    (zip/rightmost (zip/down (zip/append-child loc node)))))

(defn- find-or-create-node
  "Recursively looks for a node with a given path beginning at loc.
  When node was not found and create? is true whole the subtree (node and its children) are immediately created.
  Returns subtree with found (or eventually created) node as a root."
  [loc path create?]
  (if (= path (:path (first loc))) ;; short-circut
    loc
    (loop [node loc
           curr ""
           [head & rest] (drop-while empty? (.split path "/"))]
      (let [cpath (str curr "/" head)
            child (or (find-child-node node cpath)
                      (when create? (create-child-node node cpath)))]
        (if (and rest child)
          (recur child cpath rest) child)))))

(defn- trail-at
  "Returns list of parents of node at given loc with node itsef included at first pos,
  its parent at second pos and so on."
  [loc]
  (map zip/node (take-while (complement nil?) (iterate zip/up loc))))

(defn sticky?
  "Is parameter inherited down the category tree?"
  [prop]
  (:sticky prop))

(defn excluded?
  "Is parameter inherited down the category tree?"
  [prop]
  (:excluded prop))

(defn sticky-merge
  "Joins map m with [k v] only if v is sticky & not excluded.
  If v is excluded (and sticky) removes existing k key from m.
  Otherwise returns m."
  [m [k v]]
  (if (sticky? v)
    (if (excluded? v)
      (dissoc m k)
      (assoc m k (dissoc v :sticky)))
    m))

(defn stickify-props
  "Makes each property in m sticky by adding :sticky true"
  [m]
  (reduce-kv #(assoc %1 %2 (assoc %3 :sticky true)) {} m))

(defn- collect-params
  "Calculates list of parameters for given loc in category tree."
  [loc]
  (let [params (-> (mapv :params (trail-at loc))
                   (update-in [0] stickify-props))]
    (reduce #(reduce sticky-merge %1 %2) {} (rseq params))))

(defn lookup
  "Traverses a tree looking for a category of given path and
  recalculates params to reflect parameters inheritance."
  [path]
  (let [tree @*categories-tree*]
    (when-let [node (find-or-create-node (tree-zip tree) path false)]
      (-> (zip/node node)
          (select-keys [:path])
          (assoc :params (collect-params node))))))

(defn create-category
  "Adds new category described by path. Returns altred tree."
  [tree path params]
  (-> tree
      (tree-zip)
      (find-or-create-node path true)
      (zip/edit assoc :params params)
      (zip/root)))

(defn create-tree
  "Creates category tree basing on provided collection of category paths."
  [coll]
  (let [root (tree-zip (Category. "/" {} []))]
    (loop [node (zip/node root) [c & rest] coll]
      (if c (recur (create-category node (:path c) (:params c)) rest) node))))

(defn from-file
  "Loads tree definition from external json-formatted file.
  Definition consists of an array of following map:

  {path: 'category', params: {'has_xenons': {type: 'bool', sticky: true}}}

  where:
  * category is path-like string category/subcategory/subsubcategory/...
  * params is map of category specific parameters.

  Each parameter is described by type (eg. 'bool') and sticky/excluded flags
  used to perform inheritance calculations."
  ([path]
   (when-let [reader (clojure.java.io/reader path)]
     (log/info "Loading categories from" path)
     (when-let [tree (create-tree (json/parse-stream reader true))]
       (reset! *categories-tree* tree)))))

(defmacro with-tree
  "Changes default binding to categories tree"
  [tree & body]
  `(binding [*categories-tree* (atom ~tree)]
     ~@body))
