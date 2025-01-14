; Copyright 2024 Nubank NA
;
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
; which can be found in the file epl-v10.html at the root of this distribution.
;
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
;
; You must not remove this notice, or any other, from this software.

(ns io.pedestal.test-common
  (:require [clj-commons.ansi :as ansi]
            [clojure.core.async :as async]))

(defn no-ansi-fixture
  [f]
  (binding [ansi/*color-enabled* false]
    (f)))

(defn <!!?
  "<!! with a timeout to keep tests from hanging."
  ([ch]
   (<!!? ch 1000))
  ([ch timeout]
   (async/alt!!
     ch ([val _] val)
     (async/timeout timeout) ::timeout)))