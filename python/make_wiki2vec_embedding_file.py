import os
import numpy as np
from typing import Dict, List
import argparse
import sys
import json
import tqdm
import gensim


def convert(wiki2vec: gensim.models.KeyedVectors, id2name: Dict[str, str]):

    id2vec = {}
    unknown = []

    for entity_id, entity_name in tqdm.tqdm(id2name.items(), total=len(id2name)):
        wiki2vec_entity = to_wiki2vec_entity(entity_name.strip())
        if in_vocab(wiki2vec_entity, wiki2vec):
            embedding = [float(x) for x in wiki2vec[wiki2vec_entity]]
            id2vec[entity_id] = embedding
        else:
            unknown.append(entity_id)

    return id2vec, unknown


def in_vocab(entity: str, model) -> bool:
    # return True if entity in model.key_to_index.keys() else False
    return True if entity in model.vocab else False


def to_wiki2vec_entity(entity: str) -> str:
    return 'ENTITY/' + entity.replace(' ', '_')


def read_tsv(file: str):
    res = {}
    with open(file, 'r') as f:
        for line in f:
            parts = line.split('\t')
            if len(parts) > 1:
                res[parts[0]] = parts[1]
    return res

def main():
    """
    Main method to run code.
    """
    parser = argparse.ArgumentParser("Map CAR EntityIds to Wikipedia2Vec embeddings.")
    parser.add_argument("--wiki2vec", help="Wiki2Vec file.", required=True)
    parser.add_argument("--id2name", help="File containing mappings from CAR EntityId to EntityName.", required=True)
    parser.add_argument("--save", help="Output directory.", required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Loading entity embeddings...')
    wiki2vec: gensim.models.KeyedVectors = gensim.models.KeyedVectors.load(args.wiki2vec)
    print('[Done].')

    print('Loading entity id to name mappings...')
    id2name: Dict[str, str] = read_tsv(args.id2name)
    print('[Done].')

    print('Mapping CAR entities to Wikipedia2Vec embeddings..')
    id2vec, unknown = convert(wiki2vec=wiki2vec, id2name=id2name)
    print('[Done].')

    out_file = os.path.join(args.save, 'car_entity_to_wiki2vec_vec.json')
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








