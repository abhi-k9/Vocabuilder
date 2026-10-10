# Content guide

The word packs ship inside the app, in
[`core/content/src/main/assets/content`](../core/content/src/main/assets/content). Every definition
and example sentence is original, written for Vocabuilder.

## Files

```
content/
  index.json          {"packs": ["gre"]}: the packs, in order
  gre/
    pack.json         the pack's id, name and deck files, in study order
    common-01.json    one deck: its level, its number within the level and its words
    …
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

`BundledContentTest` checks all of these rules it can on every build: run
`./gradlew :core:content:testDebugUnitTest`.
