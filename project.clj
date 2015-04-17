(defproject imagestash "0.1.0-SNAPSHOT"
  :description "Image resizing and caching platform"
  :url "http://bitbucket.com/niktheblak/imagestash"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.imgscalr/imgscalr-lib "4.2"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ring/ring-mock "0.2.0"]]
  :jvm-opts ["-Djava.awt.headless=true"]
  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["src/test/clojure"]
  :target-path "target/%s"
  :plugins [[lein-ring "0.9.1"]]
  :ring {:handler imagestash.service/app
         :port 8080
         :init imagestash.service/start
         :destroy imagestash.service/stop})
