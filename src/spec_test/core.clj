(ns spec-test.core
  (:refer-clojure :exclude [inc])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))

;; stage1

;; inc 引数xに1を足した値を返す
(defn inc
  [x]
  (+ x 1))

;; fdef-inc inc関数の仕様
;; args: 偶数かつ整数の引数x一つを持つ
;; ret: 奇数かつ偶数の値を返す
(s/fdef inc
        :args (s/cat :x (s/and int? even?))
        :ret (s/and int? odd?))

;; inc関数を呼び出す
(inc 1)

;; 値2は偶数且つ整数か？
(s/valid? (s/and int? even?) 2)

;; inc関数のテスト結果を10個表示する
(s/exercise-fn `inc 10)

;; stage2
;; 仕様を定義する

;; 仕様を定義
(s/def ::id (s/and integer? pos?)) ;; 42
(s/def ::first-name (s/and string? #(< (count %) 20))) ;; suzukaze
(s/def ::last-name (s/and string? #(< (count %) 20))) ;; aoba
(s/def ::email-address (s/and string? #(< (count %) 30))) ;; hoge@gmail.com

;;  定義された仕様からユーザーエンティティを示す仕様を定義
(s/def ::user-entity (s/keys :req-un [::id ::first-name ::last-name ::email-address]))

;; Success!
(s/valid? ::user-entity
          {:id 1
           :first-name "suzukaze"
           :last-name "aoba"
           :email-address "aoba.suzukaze@eaglejump.com"})

;; Failed...
(s/valid? ::user-entity
          {:id 1
           :first-name "suzukaze"
           :last-name "aoba"
           :email-address "aobaaobaaobaaobaaobaaoba.suzukazesuzukazesuzukazesuzukazesuzukazesuzukaze@eaglejump.com"})

;; 失敗した際の詳細を表示する
(s/explain ::user-entity
           {:id 1
            :first-name "suzukaze"
            :last-name "aoba"
            :email-address "aobaaobaaobaaobaaobaaoba.suzukazesuzukazesuzukazesuzukazesuzukazesuzukaze@eaglejump.com"})

;; stage3
;; リクエストハンドラのテストを行う

;; POST http://localhost/api/v1/user
;; リクエスト
(s/def ::post-user-request (s/keys :req-un [::first-name ::last-name ::email-address]))

;; レスポンス
(s/def ::post-user-response (s/keys :req-un [::id ::first-name ::last-name ::email-address]))

;; シーケンシャルならIDを生成する関数
(def get-id!
  (let [id (atom 0)]
    (fn []
      (swap! id inc)
      @id)))

;; リクエストハンドラ
(defn post-user-handler
  [req]
  (assoc req :id (get-id!))) ;; :id を付与し返却

;; リクエストハンドラの仕様
(s/fdef post-user-handler
        :args (s/cat :req ::post-user-request)
        :ret ::post-user-response
        :fn #(= (-> % :ret (dissoc :id)) (-> :args :req)))

(s/conform ::post-user-response
           (post-user-handler {:first-name "hoge" :last-name "fuga" :email-address "hoge@gmail.com"}))

(gen/sample (s/gen ::post-user-request))

(s/exercise ::post-user-request 5)

(s/exercise-fn `post-user-handler)

;; stage4
;; 複雑な条件のテスト

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")

;; emailアドレス, 日時の仕様をカスタムジェネレータによって定義する
(s/def ::email-address (s/with-gen (s/and string? #(re-matches email-regex %))
                         #(gen/fmap (fn [[s1 s2 s3]] (str s1 "@" s2 "." s3))
                          (gen/tuple (gen/string-alphanumeric) (gen/string-alphanumeric) (gen/string-alphanumeric)))))

(s/exercise-fn `post-user-handler 10)
