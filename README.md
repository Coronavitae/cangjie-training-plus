# Fork

This app is a fork of the [original cangjie-training app](https://github.com/JustForFun119/cangjie-training) with additional features.

[Try this version here!](https://coronavitae.github.io/cangjie-training-plus/)

# What is Cangjie?

Cangjie is an alternative typing method for Chinese characters developed in the 80s and previously taught in Hong Kong schools. It has the benefits of enabling touch-typing: each character has a single Cangjie code, no need to choose characters from a list. It also forces the user to learn characters to type, allowing character practice during typing. Historically, its most important feature was the ability to type any Chinese dialect regardless of pronounciation.

Cangjie input methods are included on every major mobile and desktop operating system.

# How can I learn Cangjie?

There are three things you have to learn to use Cangjie:

1. The keyboard layout. This is a lot easier if you have a keyboard with Cangjie keys, but you can also learn it from diagrams like [this one](https://dylansung.tripod.com/methods/cangjie_utf8.htm), [this one](http://www.chinaknowledge.de/Literature/Script/cangjie.html), or in this app!

Each letter on the keyboard corresponds to one primary radical. Example: v=女. Typing "[v][space]" will produce "女".

2. Learn the radical mapping. There are only 24 radical keys, but there are more than 100 radicals, so each key represents multiple radical (or strokes or parts of radicals). A few of these are simple, like 一 being used for 工, or 水 usually being used for 又, but most are a bit weirder.

See [here ("summary of bases")](https://dylansung.tripod.com/methods/cangjie_utf8.htm) or [here](https://leanpub.com/cangjiedictionary/read) for a visual guide to the radical mapping. I prefer the longer explanation [here](http://www.robos.org/sections/chinese/cangjie.html).

3. Learn the rules for abbreviation. Cangjie has ~ three rules for abbreviation that are key. Since every character can be typed in 5 keys, but some characters have MANY more than 5 radicals, you only type some of the keys for each character. For basic characters, for example, you imagine the whole list of keys for the entire character, and then only type the 1st, 2nd, 3rd, and last key. This is the fundamental idea that lets Cangjie type every character with 5 keys.

4. Learn the exceptions and oddities. To actually work, some radicals have more complex rules, which can usually be understood by looking up a character on [hkcards.com](hkcards.com) and google translating the result.

For example, if the 女 key is used for 又 (when it's a radical)， and 一 is used for 工 (when it's a radical), then how do you type the characters 又 and 工? The explanation is complex, and eventualy you just have to memorize that 工＝一中一 and 又=弓大.

Fortunately, the original creator of this app built in quick links to HKCards.com for every character, so you can click any character being tested to open an explanation for its Cangjie code!

# how to use this app

1. Practice the key mappings: a new feature of this expanded app is "practice mode." Press "\" to toggle practice mode, where you are told exactly which radical codes to press for each character. This is to practice the muscle memory of pressing each key, without having to worry about the complex Cangjie code system. In Practice Mode, your practice will not affect "character mastery", which determines how often characters appear.

2. Once you feel confident about the key mappings, try turning off practice mode for normal practice. Fundamentally, the app is built on a spaced-repetition-learning model, but it's built for very slow learning (about 10 characters per day). For quicker practice, you can press 1 to add 10 new characters to the pool of learned characters, or press 2 to add up to 20 characters to the review stack. Newly added characters are taken from a list of the most commonly used Chinese characters.

Your character reviews are scored based on both accuracy AND speed. Complete each character perefectly within 1 second for a perfect score. Failed characters are repeated at the end of the practice. With sufficient reviews, a character's "mastery" is improved. In the list of characters, sun icons indicate mastery, with three suns representing full mastery.

Have fun! I hope the changes I made make this a faster way to learn and practice Cangjie.

Following text is from main branch:

# cangjie-training

Train Cangjie keyboard input, increase typing fluency (Input 🔁 Memorise)

訓練倉頡鍵盤指法，提升取碼流暢度 (輸入 🔁 記憶)

[Try it 試下](https://justforfun119.github.io/cangjie-training/public/index.html)

# Thanks

- 倉頡新星 https://gholk.github.io/cjns/index.html
  - exercise webpage https://gholk.github.io/cjns/keyExercise.html
- https://www.hkcards.com/
- Cangjie character-to-radicals dictionary from https://github.com/rime/rime-cangjie (https://raw.githubusercontent.com/rime/rime-cangjie/master/cangjie5.dict.yaml)
- Popular usage of Chinese character from https://humanum.arts.cuhk.edu.hk (https://humanum.arts.cuhk.edu.hk/Lexis/lexi-can/faq.php?s=1)
- 倉頡之友 https://www.chinesecj.com for Cangjie dictionary
- Workbox for PWA support (https://developer.chrome.com/docs/workbox/modules/workbox-cli/)

# Build app

## Development

Build app scripts: `npx shadow-cljs watch app` or `npx shadow-cljs compile app`

## Release

1. Build app scripts: `npx shadow-cljs release app`
2. Update workbox config for PWA (offline etc.): `npx workbox-cli generateSW workbox-config.js`

# User Data

Stored on user's browser local storage only

# Why

To practise/learn 倉頡 (Cangjie) input 🀄
1. Practise Cangjie **radical input** using keyboard
  - Memorise radical key mapping 記熟倉頡字母鍵位 (鍵盤指法)
    - ❔ I don't know which keyboard key to press for entering some Cangjie radical
    - 👨‍💻 practise typing with on-screen keyboard showing keys and Cangjie radicals
    - 倉頡字母 Cangjie radicals
      - 日月金木水火土 ABCDEFG
      - 竹戈十大中一弓 HIJKLMN (斜點交叉縱橫鉤)
      - 人心手口 OPQR
      - 尸廿山女田卜 STUVXY (側並仰紐方卜)
      - 難 X
  - Train to radical extraction fluency 訓練以取碼流暢度
    - ❔ I don't know/I am slow at breaking down a Chinese character into Cangjie radicals
    - 👨‍💻 practise by example: write Chinese character with/without radical hints
    - example: to write 2 characters "香港"
      - character 香 = radicals 竹 木 日 = keyboard keys "H" "D" "A"
      - 港 = 水 廿 金 山 = "E" "T" "C" "U"
2. Apply **spaced repetition learning** method for Cangjie training
  - [SM-2](https://www.supermemo.com/en/archives1990-2015/english/ol/sm2)
  - [modified SM-2](https://www.blueraja.com/blog/477/a-better-spaced-repetition-learning-algorithm-sm2) ([implemented here](src/main/cangjie_training/learner.cljs))
3. Practical web app written in Clojure/script!
