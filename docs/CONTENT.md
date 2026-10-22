# Content guide

The word packs ship inside the app, in
[`core/content/src/main/assets/content`](../core/content/src/main/assets/content). Every definition
and example sentence in the packs is original, written for Vocabuilder; the [dictionary](#dictionary)
of their synonyms and antonyms mostly comes from Open English WordNet.

## Files

```
content/
  index.json          {"packs": ["gre"]}: the packs, in order
  gre/
    pack.json         the pack's id, name and deck files, in study order
    common-01.json    one deck: its level, its number within the level and its words
    …
  dictionary/         meanings of the synonyms and antonyms that aren't words of the packs
```

A deck file looks like this:

```json
{
  "level": "common",
  "number": 1,
  "words": [
    {
      "word": "abate",
      "senses": [
        {
          "pos": "verb",
          "definition": "to become less intense or widespread",
          "example": "By evening the storm had abated, and the ferries started running again."
        }
      ],
      "synonyms": ["subside", "wane", "ebb", "lessen"],
      "antonyms": ["intensify", "escalate"],
      "note": "An optional sentence on usage, tone or a common confusion."
    }
  ]
}
```

- `level` is `common`, `intermediate` or `advanced`; `pos` is `noun`, `verb`, `adjective` or
  `adverb`. Unknown keys are rejected, so typos fail the build.
- A word's id is the pack and the word, such as `gre:abate` (`gre:bete-noire` for "bête noire").
  Learners' progress is keyed by it, so when correcting a word's spelling, add `"id"` with the old id
  to keep their progress.
- Decks hold up to 60 words, ordered by level, then numbered from 1 within each level.

## Style

- **Definitions** start in lowercase and have no final period. They don't use the word itself.
  Qualifiers go first in parentheses: "(of a disease) not harmful".
- **Examples** are full sentences that use the word, or one of its forms ("abated" for "abate"),
  in a way that makes its meaning clear from context.
- List the most common sense first; add more senses only when they are tested or commonly met.
- **Synonyms and antonyms** are lowercase, a few of each, closest first.
- **Notes** are for what a learner could get wrong: a confusable word ("adverse" and "averse"), a
  secondary meaning, or a meaning that's the opposite of what it looks like ("enervate").

## Dictionary

Synonyms and antonyms that aren't words of the packs show meanings from the dictionary, in
`content/dictionary` (see the [README](../README.md#words) for its credit):

```
dictionary/
  wordnet.json        meanings from Open English WordNet, made by scripts/build-dictionary.py
  written.json        meanings written for Vocabuilder, of the words WordNet doesn't have
  LICENSE.txt         Open English WordNet's license, which must ship with its meanings
```

Both files list entries like this, with an optional example:

```json
{"entries": [
{"word": "subside", "senses": [{"pos": "verb", "definition": "wear off or die down", "example": "The pain subsided"}]}
]}
```

Every synonym and antonym that isn't a word of the packs needs an entry, and every entry must be
one of them. After changing the synonyms or antonyms, run `scripts/build-dictionary.py` (it
downloads Open English WordNet 2025, about 11 MB, into `build/`). It picks the senses of each word
that WordNet links to the words listing it, or else the most common of their part of speech, and
lists the words WordNet doesn't have: write those in `written.json`, in the packs' style, with an
example. The script leaves out of `wordnet.json` the words that `written.json` has, so to replace a
poor WordNet entry, write the word there and run the script again.

`BundledContentTest` checks all of these rules it can on every build: run
`./gradlew :core:content:testDebugUnitTest`.
