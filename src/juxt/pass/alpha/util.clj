;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.util
  (:require [juxt.site.alpha.util :refer [as-hex-str random-bytes]])
  (:import
   (java.util HexFormat)
   (java.security SecureRandom)))

(defn make-nonce
  "This uses java.util.HexFormat which requires Java 17 and above. If required,
  this can be re-coded, see
  https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
  and similar. For the size parameter, try 12."
  [size]
  (as-hex-str (random-bytes size)))
