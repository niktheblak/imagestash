(defproject imagestash "0.1.0-SNAPSHOT"
  :description "Image resizing and caching platform"
  :url "http://bitbucket.com/niktheblak/imagestash"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.imgscalr/imgscalr-lib "4.2"]
                 [compojure "1.2.1"]
                 [ring/ring-defaults "0.1.2"]]
  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["src/test/clojure"]
  :target-path "target/%s"
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler imagestash.service/app
         :port 8080
         :init imagestash.service/start
         :destroy imagestash.service/stop})
