import sys
import tqdm
import os
import argparse
import json
from typing import List, Dict, Set, Tuple
from collections.abc import Iterable
from itertools import islice


def create_data(
        entity_data_file_dict: Dict[str, Dict[str, str]],
        query_id_to_name_dict: Dict[str, str],
        qrel_dict: Dict[str, Set[str]],
        mode: str,
        save_dir: str
) -> None:
    data: List[str] = []
    save: str = save_dir + "/" + mode + '.jsonl'

    for query_id, query_str in tqdm.tqdm(query_id_to_name_dict.items(), total=len(query_id_to_name_dict)):

        if query_id in qrel_dict.keys():

            entity_dict: Dict[str, str] = entity_data_file_dict[query_id]  # Data corresponding to the entity
            relevant_entities: Set[str] = qrel_dict[query_id]  # Relevant entities for the query

            for entity_id, entity_data in entity_dict.items():
                entity_text, retrieval_score = get_text_and_score(entity_data)
                label: int = 1 if entity_id in relevant_entities else 0  # Label of the data example
                # Check if strings are empty or not
                # Empty string evaluates to True
                if entity_text and retrieval_score:
                    data.append(to_data(
                        mode=mode,
                        query=query_str.strip(),
                        doc=entity_text,
                        label=str(label),
                        query_id=query_id,
                        doc_id=entity_id,
                        retrieval_score=retrieval_score
                    ))
                    data.append("\n")

        write_to_file(data, save)


def get_text_and_score(entity_data: str) -> Tuple[str, str]:
    try:
        data_dict: Dict[str, str] = json.loads(entity_data)
        return data_dict['text'], data_dict['score']
    except ValueError:  # includes simplejson.decoder.JSONDecodeError
        return "", ""


def write_to_file(data, output_file):
    file = open(output_file, "a")
    file.writelines(data)
    file.close()


def to_data(mode: str, query: str, doc: str, label: str, query_id: str, doc_id: str, retrieval_score: str) -> str:
    data: Dict[str, str] = {}
    if mode == 'dev':
        data = {
            'query': query,
            'doc': doc,
            'label': label,
            'query_id': query_id,
            'doc_id': doc_id,
            'retrieval_score': retrieval_score
        }
    elif mode == 'test':
        data = {
            'query': query,
            'doc': doc,
            'query_id': query_id,
            'doc_id': doc_id,
            'retrieval_score': retrieval_score
        }
    else:
        print('Mode should be `dev` or `test`')

    return json.dumps(data)


def read_entity_data_file(file_path: str) -> Dict[str, Dict[str, str]]:
    res: Dict[str, Dict[str, str]] = {}
    with open(file_path, 'r') as file:
        for line in file:
            line_parts = line.split("\t")
            query_id: str = line_parts[0]
            entity_id: str = line_parts[1]
            doc: str = line_parts[2]
            entity_dict: Dict[str, str] = res[query_id] if query_id in res.keys() else {}
            entity_dict[entity_id] = doc
            res[query_id] = entity_dict

    return res


def read_tsv(file_path: str) -> Dict[str, str]:
    tsv: Dict[str, str] = {}
    with open(file_path, 'r') as f:
        for line in f:
            key: str = line.split('\t')[0]
            value: str = line.split('\t')[1]
            tsv[key] = value
    return tsv


def read_qrel(qrel_file: str) -> Dict[str, Set[str]]:
    qrel_dict: Dict[str, Set[str]] = {}
    with open(qrel_file, 'r') as f:
        for line in f:
            query_id: str = line.split(' ')[0]
            entity_id: str = line.split(' ')[2]
            rel: int = int(line.split(' ')[3])
            if rel > 0:
                entity_set: Set[str] = qrel_dict[query_id] if query_id in qrel_dict.keys() else set()
                entity_set.add(entity_id)
                qrel_dict[query_id] = entity_set
    return qrel_dict


def main():
    parser = argparse.ArgumentParser("Create a dev or test file.")
    parser.add_argument("--entity-data", help='Path to entity data file.', required=True)
    parser.add_argument("--queries", help='Path to file containing query_id to query_name mappings.',
                        required=True)
    parser.add_argument("--qrel", help='Path to the entity ground truth file.', required=True)
    parser.add_argument("--mode", help='One of (dev|test).', required=True)
    parser.add_argument("--save-dir", help='Path to directory where data will be saved.', required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Loading entity data file...')
    entity_data_dict: Dict[str, Dict[str, str]] = read_entity_data_file(args.entity_data)
    print('[Done].')

    print('Loading query id to name mappings...')
    query_id_to_name_dict: Dict[str, str] = read_tsv(args.queries)
    print('[Done].')

    print('Loading ground truth data...')
    qrel_dict: Dict[str, Set[str]] = read_qrel(args.qrel)
    print('[Done].')

    create_data(
        entity_data_file_dict=entity_data_dict,
        query_id_to_name_dict=query_id_to_name_dict,
        qrel_dict=qrel_dict,
        mode=args.mode,
        save_dir=args.save_dir
    )


if __name__ == '__main__':
    main()
