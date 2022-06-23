import json
import sys
import tqdm
import os
import argparse
from typing import List, Dict, Set, Tuple
from collections.abc import Iterable
from itertools import islice


def create_data(
        task: str,
        pos_entity_data_dict: Dict[str, Dict[str, str]],
        neg_entity_data_dict: Dict[str, Dict[str, str]],
        query_id_to_name_dict: Dict[str, str],
        k: int,
        out_file: str
) -> None:
    total: int = 0
    tot_q: int = 0
    for query_id, query_str in tqdm.tqdm(query_id_to_name_dict.items(), total=len(query_id_to_name_dict.keys())):
        if query_id in query_id in pos_entity_data_dict.keys() and query_id in neg_entity_data_dict.keys():

            tot_q += 1

            if task == 'ranking':
                pos_ent_dict: Dict[str, str] = pos_entity_data_dict[query_id] \
                    if k > len(pos_entity_data_dict[query_id]) \
                    else dict(take(k, pos_entity_data_dict[query_id].items()))

                neg_ent_dict: Dict[str, str] = neg_entity_data_dict[query_id] \
                    if k > len(neg_entity_data_dict[query_id]) \
                    else dict(take(k, neg_entity_data_dict[query_id].items()))

                data_dict: Set[Tuple[str, str]] = get_data_dict(pos_ent_dict=pos_ent_dict, neg_ent_dict=neg_ent_dict)
                data: List[str] = to_pairwise_data(query=query_str.rstrip(), doc_dict=data_dict)
                total += len(data) / 2  # Because data contains \n characters
            else:
                data: List[str] = to_classification_data(query=query_str.rstrip(),
                                                         pos_entity_dict=pos_entity_data_dict[query_id],
                                                         neg_entity_dict=neg_entity_data_dict[query_id])
                total += len(data) / 2  # Because data contains \n characters

            write_to_file(data, out_file)
    print('Total training examples created = ' + str(total))
    print('Total queries used = ' + str(tot_q))


def get_data_dict(
        pos_ent_dict: Dict[str, str],
        neg_ent_dict: Dict[str, str]
) -> Set[Tuple[str, str]]:
    entity_pairs: List[List[str]] = [[a, b] for a in pos_ent_dict.keys() for b in neg_ent_dict.keys() if a != b]

    data_dict: Set[Tuple[str, str]] = {
        (
            get_text(pos_ent_dict[pair[0]]),
            get_text(neg_ent_dict[pair[1]])
        )
        for pair in entity_pairs
    }

    return data_dict


def get_text(entity_data: str) -> str:
    try:
        data_dict: Dict[str, str] = json.loads(entity_data)
        return data_dict['text']
    except ValueError:  # includes simplejson.decoder.JSONDecodeError
        # print('Decoding JSON has failed. Returning empty string.')
        return ""


def to_pairwise_data(query: str, doc_dict: Set[Tuple[str, str]]) -> List[str]:
    data: List[str] = []
    for doc_pos, doc_neg in doc_dict:
        data_dict = {
            'query': query,
            'doc_pos': doc_pos.rstrip(),
            'doc_neg': doc_neg.rstrip()
        }
        data.append(json.dumps(data_dict))
        data.append("\n")
    return data


def to_classification_data(
        query: str,
        pos_entity_dict: Dict[str, str],
        neg_entity_dict: Dict[str, str]
) -> List[str]:
    data: List[str] = []

    query_pos_text: List[str] = list(
        filter(None, [get_text(entity_data) for _, entity_data in pos_entity_dict.items()])
    )
    query_neg_text: List[str] = list(
        filter(None, [get_text(entity_data) for _, entity_data in neg_entity_dict.items()])
    )

    for doc_pos in query_pos_text:
        data.append(json.dumps({
            'query': query,
            'doc': doc_pos.rstrip(),
            'label': 1
        }))
        data.append("\n")

    for doc_neg in query_neg_text:
        data.append(json.dumps({
            'query': query,
            'doc': doc_neg.rstrip(),
            'label': 0
        }))
        data.append("\n")

    return data


def write_to_file(data, output_file):
    file = open(output_file, "a")
    file.writelines(data)
    file.close()


def read_entity_id_to_text_file(file_path: str) -> Dict[str, Dict[str, str]]:
    res: Dict[str, Dict[str, str]] = {}
    with open(file_path, 'r') as file:
        for line in file:
            line_parts = line.split("\t")
            query_id: str = line_parts[0]
            entity_id: str = line_parts[1]
            if query_id not in res:
                res[query_id] = {}
            doc: str = line_parts[2]
            res[query_id][entity_id] = doc

    return res


def read_tsv(file_path: str) -> Dict[str, str]:
    tsv: Dict[str, str] = {}
    with open(file_path, 'r') as f:
        for line in f:
            key: str = line.split('\t')[0]
            value: str = line.split('\t')[1]
            tsv[key] = value
    return tsv


def take(n, iterable):
    return list(islice(iterable, n))


def balance(
        pos_ent_id_to_text: Dict[str, Dict[str, str]],
        neg_ent_id_to_text: Dict[str, Dict[str, str]]
) -> Dict[str, Dict[str, str]]:
    res: Dict[str, Dict[str, str]] = {}
    for query_id in pos_ent_id_to_text:
        pos_entity_map: Dict[str, str] = pos_ent_id_to_text[query_id]
        if query_id in neg_ent_id_to_text:
            neg_ent_map: Dict[str, str] = neg_ent_id_to_text[query_id]
            neg_ent_map = dict(take(len(pos_entity_map), neg_ent_map.items()))
            res[query_id] = neg_ent_map
    return res


def main():
    parser = argparse.ArgumentParser("Create a training file.")
    parser.add_argument("--task", help='One of (ranking|classification).', required=True)
    parser.add_argument("--pos-ent-data", help='File containing data of positive examples.', required=True)
    parser.add_argument("--neg-ent-data", help='File containing data of negative examples.', required=True)
    parser.add_argument("--k", help='Number of positive and negative entities to consider while making the data.',
                        required=True)
    parser.add_argument("--queries", help='File containing queries.', required=True)
    parser.add_argument("--save-dir", help='Directory where to save.', required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    if args.task == 'ranking':
        print('Creating ranking data.')
    elif args.task == 'classification':
        print('Creating classification data.')
    else:
        raise ValueError('Task must be `ranking` or `classification`.')

    print('Reading positive entity data....')
    pos_ent_data_dict: Dict[str, Dict[str, str]] = read_entity_id_to_text_file(args.pos_ent_data)
    # pos_ent_data_dict = dict(take(int(args.k), pos_ent_data_dict.items()))
    print('[Done].')

    print('Reading negative entity data...')
    neg_ent_data_dict: Dict[str, Dict[str, str]] = read_entity_id_to_text_file(args.neg_ent_data)
    # neg_ent_data_dict = dict(take(int(args.k), neg_ent_data_dict.items()))
    neg_ent_data_dict: Dict[str, Dict[str, str]] = balance(pos_ent_data_dict, neg_ent_data_dict)
    print('[Done].')

    print('Reading queries...')
    queries: Dict[str, str] = read_tsv(args.queries)
    print('[Done].')

    save: str = args.save_dir + '/' + 'train.' + args.task + '.jsonl'

    create_data(
        task=args.task,
        pos_entity_data_dict=pos_ent_data_dict,
        neg_entity_data_dict=neg_ent_data_dict,
        query_id_to_name_dict=queries,
        k=int(args.k),
        out_file=save
    )


if __name__ == '__main__':
    main()
