from wikipedia2vec import Wikipedia2Vec, Dictionary
import os
import numpy as np
from typing import Dict, List
import argparse
import sys
import json
import tqdm

class LinearMapper:
    def __init__(self, path):
        if not path.endswith(".npy"):
            path += ".npy"

        if not os.path.exists(path):
            raise Exception("File not found.", path)

        self.model = np.load(path)

    def apply(self, x):
        return x.dot(self.model)

class Embedding:
    def __getitem__(self, word_or_words):
        if isinstance(word_or_words, str):
            if not word_or_words in self:
                raise Exception("Embedding does not contain", word_or_words)
            return self.get_vector(word_or_words)

        for word in word_or_words:
            if not word in self:
                raise Exception("Embedding does not contain", word)

        return self.get_vectors(word_or_words)

    @property
    def vocab(self):
        return self.get_vocab()

    @property
    def all_embeddings(self):
        return self[self.vocab]


class Wikipedia2VecEmbedding(Embedding):
    def __init__(self, path, prefix="ENTITY/", do_cache_dict=True, do_lower_case=False):
        from wikipedia2vec import Wikipedia2Vec, Dictionary
        if os.path.exists(path):
            self.model = Wikipedia2Vec.load(path)
        else:
            raise FileNotFoundError()

        self.dict_cache = None
        if do_cache_dict:
            self.dict_cache = {}

        self.prefix = prefix
        self.do_lower_case = do_lower_case

    def _preprocess_word(self, word):
        if word.startswith(self.prefix):
            word = ' '.join(word[len(self.prefix):].split('_'))
        if self.do_lower_case:
            word = word.lower()
        return word

    def index(self, word):
        preprocessed_word = self._preprocess_word(word)

        if (not self.dict_cache is None) and preprocessed_word in self.dict_cache:
            return self.dict_cache[preprocessed_word]

        if word.startswith(self.prefix):
            ret = self.model.dictionary.get_entity(preprocessed_word)
        else:
            ret = self.model.dictionary.get_word(preprocessed_word)

        if not self.dict_cache is None:
            self.dict_cache[preprocessed_word] = ret

        return ret

    def __contains__(self, word):
        return self.index(word) is not None

    def get_vector(self, word):
        if word.startswith(self.prefix):
            return self.model.get_vector(self.index(word))
        return self.model.get_vector(self.index(word))

    def get_vectors(self, words):
        return np.stack([self.get_vector(word) for word in words], 0)

    @property
    def all_special_tokens(self):
        return []


class EBertEmbedding(Embedding):
    def __init__(self, embedding: Wikipedia2VecEmbedding, mapper: LinearMapper):
        self.embedding = embedding
        self.mapper = mapper

    def __getitem__(self, word_or_words):
        embedding = self.embedding[word_or_words]
        return self.mapper.apply(embedding)

    def __contains__(self, word):
        return word in self.embedding

    def index(self, word):
        return self.embedding.index(word)

    @property
    def all_special_tokens(self):
        return self.embedding.all_special_tokens


def convert(ebert: EBertEmbedding, id2name: Dict[str, str]):

    id2vec = {}
    unknown = []

    for entity_id, entity_name in tqdm.tqdm(id2name.items(), total=len(id2name)):
        wiki2vec_entity = to_wiki2vec_entity(entity_name.strip())
        if in_vocab(wiki2vec_entity, ebert):
            embedding = [float(x) for x in ebert[wiki2vec_entity]]
            id2vec[entity_id] = embedding
        else:
            unknown.append(entity_id)

    return id2vec, unknown



def load_embeddings(wiki2vec_file: str, mapping_file: str) -> EBertEmbedding:
    w2v = Wikipedia2VecEmbedding(path=wiki2vec_file)
    mapper = LinearMapper(path=mapping_file)
    return EBertEmbedding(embedding=w2v, mapper=mapper)


def in_vocab(entity: str, model: EBertEmbedding) -> bool:
    return True if entity in model else False


def to_wiki2vec_entity(entity: str) -> str:
    return 'ENTITY/' + entity.replace(' ', '_')


def read_tsv(file: str) -> Dict[str, str]:
    res = {}
    with open(file, 'r') as f:
        for line in f:
            parts = line.split('\t')
            key = parts[0]
            value = parts[1]
            res[key] = value
    return res


def main():
    """
    Main method to run code.
    """
    parser = argparse.ArgumentParser("Map CAR EntityIds to E-BERT embeddings.")
    parser.add_argument("--wiki2vec", help="Wiki2Vec file.", required=True)
    parser.add_argument("--mapper", help="Mapping file for E-BERT (.npy file).", required=True)
    parser.add_argument("--id2name", help="Mappings from CAR EntityId to EntityName.", required=True)
    parser.add_argument("--save", help="Output directory.", required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Loading entity embeddings...')
    ebert: EBertEmbedding = load_embeddings(wiki2vec_file=args.wiki2vec, mapping_file=args.mapper)
    print('[Done].')

    print('Loading entity id to name mappings...')
    id2name: Dict[str, str] = read_tsv(args.id2name)
    print('[Done].')

    print('Mapping CAR entities to E-BERT embeddings..')
    id2vec, unknown = convert(ebert=ebert, id2name=id2name)
    print('[Done].')

    out_file = os.path.join(args.save, 'car_entity_to_ebert_vec.json')
    stats_file = os.path.join(args.save, 'unknown_car_entities.txt')

    print('Writing to file..')
    with open(out_file, 'w') as f:
        json.dump(id2vec, f)

    with open(stats_file, 'w') as f:
        f.write("%s\n" % len(unknown))
        for item in unknown:
            f.write("%s\n" % item)
    print('[Done].')




if __name__ == '__main__':
    main()
