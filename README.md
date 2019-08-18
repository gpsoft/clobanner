# Clobanner

[バナー画像を手軽に生成するWebアプリ](https://gpsoft.github.io/clobanner/)です。

- 画像の合成には、HTML5のキャンバスを利用
- 開発言語はClojureScript
- GUIは割愛して、バナー画像の情報は[edn(https://github.com/edn-format/edn)](https://github.com/edn-format/edn)形式で記述
- 背景画像ファイルを指定可能

![clobanner](clobanner.png)

# 開発

使うツールは:

- Clojureの[CLIツール(tools.deps)](https://clojure.org/guides/getting_started)
- [figwheel](https://github.com/bhauman/figwheel-main)
- [rebel-readline](https://github.com/bhauman/rebel-readline)
- nREPLサーバ
- Vim
- [vim-fireplace](https://github.com/tpope/vim-fireplace)
- [piggieback](https://github.com/nrepl/piggieback)

などなど。

まずはサーバ群を起動:

    $ clojure -Adev

デフォルトのブラウザで、`http://localhost:9500/index-dev.html`が開くはず(開かなければ手動で)。この時点でfigwheelとつながり、自動ビルド&リロードがONになる。

Vimで`core.cljs`を開いて、nREPLサーバと接続しCLJS REPLセッションを開始:

    :Piggieback (figwheel.main.api/repl-env "dev")

開発用のビルドオプションは、`dev.cljs.edn`に記述。

TODO:

- バナー画像情報ednのspecチェック
- 背景画像の合成を柔軟にする

# リリース

リリース用のビルドオプションは、`prod.cljs.edn`に記述。

ビルド:

    $ clojure -Aprod

あとは、`resources/public/`の下をWebサーバへデプロイ(あるいは、ブラウザで直接`index.html`を開く)。

- `index.html`
- `css/`
- `img/`
- `js/clobanner.js`
