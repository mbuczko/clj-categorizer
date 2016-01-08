(set-env!
 :source-paths   #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0-RC4" :scope "provided"]
                 [org.clojure/tools.logging "0.3.1"]
                 [boot/core "2.5.5"]
                 [adzerk/bootlaces "0.1.13"]
                 [zilti/boot-midje "0.2.1-SNAPSHOT" :scope "test"]
                 [midje "1.8.3" :scope "test"]
                 [cheshire "5.5.0"]])

(require '[adzerk.bootlaces :refer :all]
         '[zilti.boot-midje :refer [midje]])

(def +version+ "0.1.0")

(bootlaces! +version+)

(task-options!
 midje {:test-paths #{"test"}}
 pom   {:project 'mbuczko/categorizer
        :version +version+
        :description "Scores incoming data based on predefined matchers"
        :url "https://github.com/mbuczko/categorizer"
        :scm {:url "https://github.com/mbuczko/categorizer"}
        :license {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})
