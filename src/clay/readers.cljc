(ns clay.readers
  (:require
   #?(:cljs ["react-plotly.js" :default Plot])
   #?(:clj [clojure.core :refer [print-method]])))

(comment
  (str (Plotly.)))

(deftype Plotly []
  Object
  (toString [_]
    "#nrepl-ws/plotly PlotlyComponent"))

#?(:clj
   (defmethod print-method Plotly
     [v w]
     (.write w (str v))))

(defn plotly [_]
  #?(:cljs Plot
     :clj (Plotly.)))