import sys
import tqdm
import os
import argparse
from typing import List, Dict, Set, Tuple
from collections.abc import Iterable
from itertools import islice


def create_file(
        pos_ent_dict: Dict[str, Set[str]],
        run_file_dict: Dict[str, Set[str]],
        top_k: int,
        out_file: str
) -> None:
    for query_id in tqdm.tqdm(run_file_dict.keys(), total=len(run_file_dict.keys())):
        if query_id in pos_ent_dict.keys():
            pos_ent: Set[str] = pos_ent_dict[query_id]
            ret_ent: Set[str] = take(top_k, run_file_dict[query_id])
            neg_ent: Set[str] = ret_ent - pos_ent
            neg_file_strings: List[str] = make_neg_file_strings(query_id, neg_ent)
            write_to_file(neg_file_strings, out_file)


def make_neg_file_strings(
        query_id: str,
        neg_ent_set: Set[str]
) -> List[str]:
    neg_file_strings: List[str] = []

    for neg_ent in neg_ent_set:
        neg_file_strings.append(query_id + ' 0 ' + neg_ent + ' 0 ')

    return neg_file_strings


def write_to_file(data: List[str], output_file: str):
    with open(output_file, 'a') as f:
        for item in data:
            f.write("%s\n" % item)


def load_file(file_path: str, file_type: str) -> Dict[str, Set[str]]:
    rankings: Dict[str, Set[str]] = {}
    with open(file_path, 'r') as file:
        for line in file:
            line_parts: List[str] = line.split(" ")
            query_id: str = line_parts[0]
            entity_id: str = line_parts[2]
            entity_set: Set[str] = rankings[query_id] if query_id in rankings.keys() else set()
            if file_type == 'qrels':
                rel: int = int(line_parts[3])
                if rel > 0:
                    entity_set.add(entity_id)
            else:
                entity_set.add(entity_id)
            rankings[query_id] = entity_set
    return rankings


def take(n: int, iterable: Set[str]) -> Set[str]:
    return set(islice(iterable, n))


def main():
    parser = argparse.ArgumentParser("Create a file with negative (non-relevant) entities.")
    parser.add_argument("--pos-ent", help='File containing positive examples (i.e., ground truth file).', required=True)
    parser.add_argument("--run", help='Run file.', required=True)
    parser.add_argument("--top-k", help='Top-K entities to take.', required=True)
    parser.add_argument("--save", help='File to save.', required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Loading run file...')
    run_file_dict: Dict[str, Set[str]] = load_file(args.run, file_type='run')
    print('[Done].')

    print('Loading ground truth file...')
    pos_ent_dict: Dict[str, Set[str]] = load_file(args.pos_ent, file_type='qrels')
    print('[Done].')

    create_file(pos_ent_dict, run_file_dict, int(args.top_k), args.save)


if __name__ == '__main__':
    main()
