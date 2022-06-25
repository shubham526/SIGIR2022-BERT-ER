import json
import sys
import tqdm
import os
import argparse
from os import listdir
from os.path import isfile, join
from typing import List, Dict, Set, Tuple
from trec_car import read_data

def get_queries_in_fold(outlines_file: str):
    with open(outlines_file, 'rb') as outlines:
        return [page.page_id for page in read_data.iter_outlines(outlines)]


def read_folds(data_dir: str) -> Dict[int, List[str]]:
    return dict(
        (i, get_queries_in_fold(join(data_dir, 'fold-' + str(i) + '-train.pages.cbor-outlines.cbor')))
        for i in range(5)
    )


def get_train_queries(folds: Dict[int, List[str]], i: int) -> List[str]:
    train_queries: List[str] = []
    for j in range(5):
        if j != i:
            train_queries.extend(folds[j])
    return train_queries


def create_fold_file(folds: Dict[int, List[str]]):

    # Train on everything except fold-n, then test on fold-n
    res = {}

    for i in range(5):
        test_queries: List[str] = folds[i]
        train_queries: List[str] = get_train_queries(folds, i)
        res[i] = {
            'training': train_queries,
            'testing': test_queries
        }

    return res


def main():
    parser = argparse.ArgumentParser("Create folds file containing queries for 5-fold CV from CAR data.")
    parser.add_argument("--data", help='Path to directory containing folds.', required=True)
    parser.add_argument("--save", help='Path to directory where data will be saved.', required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Loading fold queries..')
    folds: Dict[int, List[str]] = read_folds(args.data)
    print('[Done].')

    print('Creating fold file...')
    res = create_fold_file(folds)
    print('[Done].')

    print('Saving...')
    with open(args.save, 'w', encoding='utf-8') as f:
        json.dump(res, f, ensure_ascii=False, indent=4)
    print('[Done].')


if __name__ == '__main__':
    main()














