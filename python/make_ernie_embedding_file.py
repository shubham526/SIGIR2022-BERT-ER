import os
import numpy as np
from typing import Dict, List
import argparse
import sys
import json
import tqdm


def convert(
        entity2id: Dict[str, str],
        entity2vec: List[List[float]],
        entity_map: Dict[str, str],
        car_entities: Dict[str, str]
):

    id2vec = {}
    unknown = []

    for entity_id, entity_name in car_entities.items():
        if entity_name.strip() in entity_map.keys():
            wikidata_id = entity_map[entity_name.strip()]
            line_id = entity2id[wikidata_id.strip()]
            embedding = entity2vec[int(line_id.strip())]
            id2vec[entity_id] = embedding
        else:
            unknown.append(entity_id)

    return id2vec, unknown


def read_tsv(file: str):
    res = {}
    with open(file, 'r') as f:
        for line in f:
            parts = line.split('\t')
            if len(parts) > 1:
                res[parts[0]] = parts[1]
    return res

def main():
     parser = argparse.ArgumentParser("Map CAR EntityIds to ERNIE embeddings.")
    parser.add_argument("--entity2id", help="ERNIE entity2id.txt file.", required=True)
    parser.add_argument("--entity2vec", help="ERNIE entity2vec.vec file.", required=True)
    parser.add_argument("--entity-map", help="ERNIE entity_map.txt file.", required=True)
    parser.add_argument("--car-entities", help="Mappings from CAR EntityId to EntityName.", required=True)
    parser.add_argument("--save", help="Directory to save.", required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Reading entity2id.txt..')
    entity2id = read_tsv(args.entity2id)
    print('[Done].')

    print('Reading entity2vec.vec ...')
    entity2vec = []
    with open(args.entity2vec, 'r') as fin:
        for line in fin:
            vec = line.strip().split('\t')
            vec = [float(x) for x in vec]
            entity2vec.append(vec)
    print('[Done].')

    print('Reading entity_map.txt ...')
    entity_map = read_tsv(args.entity_map)
    print('[Done].')

    print('Reading CAR entities...')
    car_entities = read_tsv(args.car_entities)
    print('[Done].')

    print('Mapping CAR entities to ERNIE embeddings..')
    id2vec, unknown = convert(entity2id=entity2id, entity2vec=entity2vec, entity_map=entity_map, car_entities=car_entities)
    print('[Done].')

    out_file = os.path.join(args.save, 'car_entity_to_ernie_vec.json')
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







