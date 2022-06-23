import os
import numpy as np
from typing import Dict, List
import argparse
import sys
import json
import tqdm
import gensim
from scipy import spatial
import operator


def load_run_file(file_path: str) -> Dict[str, List[str]]:
    rankings: Dict[str, List[str]] = {}
    with open(file_path, 'r') as file:
        for line in file:
            line_parts = line.split(" ")
            query_id = line_parts[0]
            entity_id = line_parts[2]
            entity_list: List[str] = rankings[query_id] if query_id in rankings.keys() else []
            entity_list.append(entity_id)
            rankings[query_id] = entity_list
    return rankings

def get_query_annotations(query_annotations: str) -> Dict[str, float]:
    annotations = json.loads(query_annotations)
    res: Dict[str, float] = {}
    for ann in annotations:
        a = json.loads(ann)
        res[a['entity_name']] = a['score']
    return res

def cosine_distance(
        query_entity: str,
        target_entity: str,
        embeddings: Dict[str, List[float]],
        name2id: Dict[str, str],
) -> float:

    if query_entity in name2id and name2id[query_entity].strip() in embeddings and target_entity in embeddings:
        emb_e1 = np.array(embeddings[name2id[query_entity].strip()])
        emb_e2 = np.array(embeddings[target_entity])
        return 1 - spatial.distance.cosine(emb_e1, emb_e2)
    else:
        return 0.0

def entity_score(
        query_annotations: Dict[str, float],
        target_entity: str,
        embeddings: Dict[str, List[float]],
        name2id: Dict[str, str],
) -> float:
    score = 0
    for query_entity, conf in query_annotations.items():
        distance = cosine_distance(query_entity, target_entity, embeddings, name2id)
        score += distance * conf
    return score


def re_rank(
        run_dict: Dict[str, List[str]],
        query_annotations: Dict[str, str],
        embeddings: Dict[str, List[float]],
        embedding_method: str,
        name2id: Dict[str, str],
        k: int,
        out_file: str
) -> None:

    print('Re-ranking top-{} entities from the run file.'.format(k))
    for query_id, query_entities in tqdm.tqdm(run_dict.items(), total=len(run_dict)):
        query_entities = query_entities[:k]
        ranked_entities: Dict[str, float] = rank_entities_for_query(
            entity_list=query_entities,
            query_annotations=get_query_annotations(query_annotations[query_id]),
            embeddings=embeddings,
            name2id=name2id
        )
        if not ranked_entities:
            print('Empty ranking for query: {}'.format(query))
        else:
            run_file_strings: List[str] = to_run_file_strings(query_id, ranked_entities, embedding_method)
            write_to_file(run_file_strings, out_file)


def rank_entities_for_query(
        entity_list: List[str],
        query_annotations: Dict[str, float],
        embeddings: Dict[str, List[float]],
        name2id: Dict[str, str],
) -> Dict[str, float]:
    ranking: Dict[str, float] = dict(
        (entity, entity_score(
            query_annotations=query_annotations,
            target_entity=entity,
            embeddings=embeddings,
            name2id=name2id
        ))
        for entity in entity_list
    )

    return dict(sorted(ranking.items(), key=operator.itemgetter(1), reverse=True))


def to_run_file_strings(query: str, entity_ranking: Dict[str, float], embedding_method: str) -> List[str]:
    embedding_method = embedding_method + '-ReRank'
    run_file_strings: List[str] = []
    rank: int = 1
    for entity, score in entity_ranking.items():
        if score > 0.0:
            run_file_string: str = query + ' Q0 ' + entity + ' ' + str(rank) + ' ' + str(score) + ' ' + embedding_method
            run_file_strings.append(run_file_string)
            rank += 1

    return run_file_strings


def write_to_file(run_file_strings: List[str], run_file: str) -> None:
    with open(run_file, 'a') as f:
        for item in run_file_strings:
            f.write("%s\n" % item)


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
    parser = argparse.ArgumentParser("Re-implementation of entity re-ranking using entity embeddings from Gerritse et al., 2020.")
    parser.add_argument("--run", help="Entity run file to re-rank.", required=True)
    parser.add_argument("--annotations", help="File containing TagMe annotations for queries.", required=True)
    parser.add_argument("--embeddings", help="Entity embedding file", required=True)
    parser.add_argument("--embedding-method", help="Entity embedding method (Wiki2Vec|ERNIE|E-BERT).", required=True)
    parser.add_argument("--name2id", help="EntityName to EntityId mappings.", required=True)
    parser.add_argument("--k", help="Top-K entities to re-rank from run file.", required=True, type=int)
    parser.add_argument("--save", help="Output run file (re-ranked).", required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Loading run file...')
    run_dict: Dict[str, List[str]] = load_run_file(args.run)
    print('[Done].')

    print('Loading query annotations...')
    query_annotations: Dict[str, str] = read_tsv(args.annotations)
    print('[Done].')

    print('Loading entity embeddings...')
    with open(args.embeddings, 'r') as f:
        embeddings: Dict[str, List[float]] = json.load(f)
    print('[Done].')

    print('Loading name2id file...')
    name2id = read_tsv(args.name2id)
    print('[Done].')

    print("Re-Ranking run...")
    re_rank(
        run_dict=run_dict,
        query_annotations=query_annotations,
        embeddings=embeddings,
        embedding_method=args.embedding_method,
        name2id=name2id,
        k=args.k,
        out_file=args.save
    )
    print('[Done].')

    print('New run file written to {}'.format(args.save))


if __name__ == '__main__':
    main()
