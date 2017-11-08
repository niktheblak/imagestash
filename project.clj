(defproject imagestash "0.1.0-SNAPSHOT"
  :description "Image resizing and caching platform"
  :url "http://bitbucket.com/niktheblak/imagestash"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.imgscalr/imgscalr-lib "4.2"]
                 [metosin/compojure-api "1.1.11"]
                 [metosin/ring-http-response "0.9.0"]
                 [clj-time "0.14.0"]
                 [org.clojure/tools.logging "0.4.0"]]
  :jvm-opts ["-Djava.awt.headless=true"]
  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["src/test/clojure"]
  :target-path "target/%s"
  :plugins [[lein-ring "0.12.1"]]
  :uberjar-name "server.jar"
  :profiles {:uberjar {:resource-paths ["swagger-ui"]
                       :aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.compiler.elide-meta=[:doc :file :line :added]"]}}
  :ring {:handler imagestash.service/app
         :port 8080
         :init imagestash.service/start
         :destroy imagestash.service/stop})
