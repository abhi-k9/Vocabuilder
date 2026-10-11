#!/usr/bin/env python3
"""Writes the meanings of the synonyms and antonyms that aren't words of the app, from Open English WordNet.

Looked up from a card, a synonym or antonym that is a word of the packs shows its own meanings; the others show the
meanings this script picks from Open English WordNet: the senses closest to the words that list it, first those that
WordNet links to them, then the most common of the same part of speech, at most a few. Examples are kept only when
they use the word.

It writes core/content/src/main/assets/content/dictionary/wordnet.json, leaving out the words that
dictionary/written.json defines, and lists the words WordNet doesn't have, to define there by hand.

Usage: scripts/build-dictionary.py [english-wordnet-2025.xml.gz]
Without the file, it downloads Open English WordNet 2025 and checks it against its known checksum.
"""

import collections
import gzip
import hashlib
import json
import os
import re
import sys
import unicodedata
import urllib.request
import xml.etree.ElementTree as ElementTree

WORDNET_URL = "https://github.com/globalwordnet/english-wordnet/releases/download/2025-edition/english-wordnet-2025.xml.gz"
WORDNET_SHA256 = "9ca6d1dcb75f822fdd66617f7d9da48142ace38dd544d6ad5e2feca1674ad3fe"

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(ROOT, "core", "content", "src", "main", "assets", "content")
DICTIONARY = os.path.join(CONTENT, "dictionary")

MAX_SENSES = 3
POS_NAMES = {"n": "noun", "v": "verb", "a": "adjective", "s": "adjective", "r": "adverb"}
# Relations that tie two senses' meanings closely: one step of these makes a sense relevant to a word.
SYNSET_RELATIONS = {
    "similar", "also", "hypernym", "hyponym", "instance_hypernym", "instance_hyponym", "entails", "is_entailed_by",
    "causes", "is_caused_by", "attribute", "antonym",
}
SENSE_RELATIONS = {"antonym", "derivation", "pertainym", "also", "similar", "participle"}
# Relations within a cluster of adjectives, followed for two steps: "ponderous" is similar to "heavy", whose
# antonym is "light".
CLUSTER_RELATIONS = {"similar", "antonym", "also"}


def load_packs():
    """For every synonym or antonym that isn't a word of the packs, the words listing it, with their parts of speech."""
    headwords = set()
    listed_by = collections.defaultdict(list)
    with open(os.path.join(CONTENT, "index.json")) as file:
        packs = json.load(file)["packs"]
    for pack in packs:
        with open(os.path.join(CONTENT, pack, "pack.json")) as file:
            decks = json.load(file)["decks"]
        for deck in decks:
            with open(os.path.join(CONTENT, pack, f"{deck}.json")) as file:
                for word in json.load(file)["words"]:
                    headwords.add(word["word"].lower())
                    parts_of_speech = {sense["pos"] for sense in word["senses"]}
                    for kind in ("synonyms", "antonyms"):
                        for related in word.get(kind, []):
                            listed_by[related].append((word["word"].lower(), parts_of_speech))
    return {word: listing for word, listing in listed_by.items() if word.lower() not in headwords}


class WordNet:
    """The parts of Open English WordNet needed here: entries by written form, their senses, and synsets."""

    def __init__(self, path):
        self.entries_by_form = collections.defaultdict(list)  # lowercase lemma or form -> [(entry id, is lemma)]
        self.entries = {}  # entry id -> (lemma, [sense ids])
        self.senses = {}  # sense id -> (synset id, [(relation, target sense id)])
        self.synsets = {}  # synset id -> (part of speech, definition, [examples], [(relation, target synset id)])
        with gzip.open(path) as file:
            for _, element in ElementTree.iterparse(file, events=("end",)):
                if element.tag == "LexicalEntry":
                    self._add_entry(element)
                    element.clear()
                elif element.tag == "Synset":
                    self._add_synset(element)
                    element.clear()
        self.neighbors = collections.defaultdict(set)
        self.cluster_neighbors = collections.defaultdict(set)
        self.hypernyms = collections.defaultdict(set)
        self.hyponyms = collections.defaultdict(set)
        for synset_id, (_, _, _, relations) in self.synsets.items():
            for relation, target in relations:
                self._link(synset_id, target, relation in SYNSET_RELATIONS, relation in CLUSTER_RELATIONS)
                if relation in ("hypernym", "instance_hypernym"):
                    self.hypernyms[synset_id].add(target)
                    self.hyponyms[target].add(synset_id)
        for synset_id, relations in self.senses.values():
            for relation, target in relations:
                if target in self.senses:
                    target_synset = self.senses[target][0]
                    self._link(synset_id, target_synset, relation in SENSE_RELATIONS, relation in CLUSTER_RELATIONS)

    def _add_entry(self, element):
        lemma = element.find("Lemma").get("writtenForm")
        sense_ids = []
        for sense in element.findall("Sense"):
            sense_ids.append(sense.get("id"))
            relations = [(r.get("relType"), r.get("target")) for r in sense.findall("SenseRelation")]
            self.senses[sense.get("id")] = (sense.get("synset"), relations)
        entry_id = element.get("id")
        self.entries[entry_id] = (lemma, sense_ids)
        self.entries_by_form[lemma.lower()].append((entry_id, True))
        for form in element.findall("Form"):
            self.entries_by_form[form.get("writtenForm").lower()].append((entry_id, False))

    def _add_synset(self, element):
        definition = element.find("Definition")
        examples = [example.text for example in element.findall("Example") if example.text]
        relations = [(r.get("relType"), r.get("target")) for r in element.findall("SynsetRelation")]
        self.synsets[element.get("id")] = (
            element.get("partOfSpeech"),
            definition.text if definition is not None else None,
            examples,
            relations,
        )

    def _link(self, a, b, is_close, is_cluster):
        if is_close:
            self.neighbors[a].add(b)
            self.neighbors[b].add(a)
        if is_cluster:
            self.cluster_neighbors[a].add(b)
            self.cluster_neighbors[b].add(a)

    def lemma_entries(self, word):
        """The entries whose lemma is the word, also tried without accents and with its hyphens or spaces changed, as
        WordNet writes "blase" and "buildup". Inflected forms it lists ("copied") are left out: listed as synonyms,
        they're adjectives, which WordNet doesn't have."""
        for variant in variants(word):
            lemmas = [entry_id for entry_id, is_lemma in self.entries_by_form.get(variant, []) if is_lemma]
            if lemmas:
                return lemmas
        return []

    def siblings(self, synset_id):
        """The synsets that share a broader meaning with this one, as "launch" and "instigate" share "start"."""
        return {sibling for parent in self.hypernyms[synset_id] for sibling in self.hyponyms[parent]}

    def synsets_of(self, word):
        return {self.senses[sense_id][0] for entry_id in self.lemma_entries(word) for sense_id in self.entries[entry_id][1]}


def variants(word):
    word = word.lower()
    plain = unicodedata.normalize("NFD", word).encode("ascii", "ignore").decode()
    found = []
    for base in (word, plain):
        for variant in (base, base.replace("-", ""), base.replace("-", " "), base.replace(" ", "-")):
            if variant not in found:
                found.append(variant)
    return found


def relevance(wordnet, synset_id, listed_by):
    """How close a sense is to the words listing the word, best first: 5 when WordNet puts it with one of them, 4
    when it links them, 3 or 2 when it links them through other adjectives of their clusters, 1 when they only share a
    broader meaning, else 0; then whether it has the part of speech of one of them."""
    linked, same_pos = 0, False
    pos = POS_NAMES.get(wordnet.synsets[synset_id][0])
    for headword, parts_of_speech in listed_by:
        same_pos = same_pos or pos in parts_of_speech
        own = wordnet.synsets_of(headword)
        if synset_id in own:
            linked = 5
        elif any(synset_id in wordnet.neighbors[s] for s in own):
            linked = max(linked, 4)
        else:
            near = {n for s in own for n in wordnet.cluster_neighbors[s]}
            nearer = {n for s in near for n in wordnet.cluster_neighbors[s]}
            if synset_id in nearer:
                linked = max(linked, 3)
            elif any(synset_id in wordnet.cluster_neighbors[n] for n in nearer):
                linked = max(linked, 2)
            elif any(synset_id in wordnet.siblings(s) for s in own):
                linked = max(linked, 1)
    return linked, same_pos


def uses_word(word, example):
    """Whether an example uses the word, or one of its regular forms ("subsided" for "subside")."""
    word = word.lower()
    text = example.lower()
    if not word.isalpha():
        return word in text
    forms = {word + ending for ending in ("", "s", "es", "d", "ed", "ing", "r", "er", "st", "est", "ly", "ness")}
    if word.endswith("e"):
        forms |= {word[:-1] + ending for ending in ("ed", "ing", "er", "est")}
    if word.endswith("y"):
        forms |= {word[:-1] + "i" + ending for ending in ("es", "ed", "er", "est", "ly", "ness")}
    return any(token in forms for token in re.split(r"[^a-z]+", text))


def clean_example(example):
    example = example.strip()
    if len(example) > 1 and example[0] in "\"“" and example[-1] in "\"”" and example.count('"') <= 2:
        example = example[1:-1].strip()
    return example


def choose_senses(wordnet, word, listed_by):
    candidates = []  # (relevance, rank within its entry, synset id)
    for entry_id in wordnet.lemma_entries(word):
        for rank, sense_id in enumerate(wordnet.entries[entry_id][1]):
            synset_id = wordnet.senses[sense_id][0]
            if POS_NAMES.get(wordnet.synsets[synset_id][0]) and wordnet.synsets[synset_id][1]:
                candidates.append((relevance(wordnet, synset_id, listed_by), rank, synset_id))
    if not candidates:
        return None
    # The senses WordNet links closely to the listing words if there are some; else the most common of their part of
    # speech, with any it links loosely, as those links are less reliable.
    strong = [candidate for candidate in candidates if candidate[0][0] >= 2]
    if strong:
        candidates = sorted(strong, key=lambda candidate: (-candidate[0][0], candidate[1]))
    else:
        candidates = [candidate for candidate in candidates if candidate[0][1]] or candidates
        candidates.sort(key=lambda candidate: candidate[1])
        loose = [candidate for candidate in candidates[MAX_SENSES:] if candidate[0][0]]
        if loose:
            candidates = candidates[: MAX_SENSES - 1] + loose[:1]
    senses, definitions = [], set()
    for _, _, synset_id in candidates:
        if len(senses) == MAX_SENSES:
            break
        pos, definition, examples, _ = wordnet.synsets[synset_id]
        definition = definition.strip()
        if definition in definitions:
            continue
        definitions.add(definition)
        sense = {"pos": POS_NAMES[pos], "definition": definition}
        example = next((clean_example(e) for e in examples if uses_word(word, clean_example(e))), None)
        if example:
            sense["example"] = example
        senses.append(sense)
    return senses


def wordnet_file():
    if len(sys.argv) > 1:
        return sys.argv[1]
    path = os.path.join(ROOT, "build", "english-wordnet-2025.xml.gz")
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        print(f"Downloading {WORDNET_URL}", file=sys.stderr)
        urllib.request.urlretrieve(WORDNET_URL, path)
    with open(path, "rb") as file:
        if hashlib.sha256(file.read()).hexdigest() != WORDNET_SHA256:
            sys.exit(f"{path} isn't Open English WordNet 2025: its checksum doesn't match")
    return path


def main():
    listed_by = load_packs()
    with open(os.path.join(DICTIONARY, "written.json")) as file:
        written = {entry["word"] for entry in json.load(file)["entries"]}
    wordnet = WordNet(wordnet_file())
    entries, missing = [], []
    for word in sorted(listed_by, key=lambda w: (w.lower(), w)):
        if word in written:
            continue
        senses = choose_senses(wordnet, word, listed_by[word])
        if senses:
            entries.append({"word": word, "senses": senses})
        else:
            missing.append(word)
    with open(os.path.join(DICTIONARY, "wordnet.json"), "w") as file:
        file.write('{"entries": [\n')
        file.write(",\n".join(json.dumps(entry, ensure_ascii=False) for entry in entries))
        file.write("\n]}\n")
    print(f"{len(entries)} words from WordNet, {len(written)} written, {len(missing)} missing", file=sys.stderr)
    for word in missing:
        listing = ", ".join(sorted({headword for headword, _ in listed_by[word]}))
        print(f"missing: {word} (listed by {listing})")


if __name__ == "__main__":
    main()
