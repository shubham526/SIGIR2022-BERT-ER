import json
import sys
import tqdm
import os
import argparse
from os import listdir
from os.path import isfile, join
from typing import List, Dict, Set, Tuple
from collections.abc import Iterable
from itertools import islice


def read_folds(fold_file: str) -> Dict[str, Dict[str, List[str]]]:
    with open(fold_file, 'r') as f:
        return json.load(f)


def train_queries(fold_number: str, data: Dict[str, Dict[str, List[str]]]) -> List[str]:
    return data[fold_number]['training']


def test_queries(fold_number: str, data: Dict[str, Dict[str, List[str]]]) -> List[str]:
    return data[fold_number]['testing']


def read_run_file(run_file_path: str) -> Dict[str, List[str]]:
    run_dict: Dict[str, List[str]] = {}
    with open(run_file_path, 'r') as f:
        for line in f:
            query_id: str = line.split(" ")[0]
            query_run_strings_list: List[str] = run_dict[query_id] if query_id in run_dict.keys() else []
            query_run_strings_list.append(line)
            run_dict[query_id] = query_run_strings_list

    return run_dict


def write_to_file(file_path: str, data: List[List[str]]) -> None:
    with open(file_path, 'a') as f:
        for run_file_strings in data:
            for run_string in run_file_strings:
                f.write('%s' % run_string)


def create_folds_for_run(
        fold_queries: Dict[str, Dict[str, List[str]]],
        run_dict:  Dict[str, List[str]],
        save_sir: str,
        train_file: str,
        test_file: str
) -> None:

    for fold_num in range(5):

        # Get the train queries for this fold
        train_queries_for_fold: List[str] = train_queries(fold_number=str(fold_num), data=fold_queries)

        # Get the test queries for this fold
        test_queries_for_fold: List[str] = test_queries(fold_number=str(fold_num), data=fold_queries)

        # Filter the run file to have only those queries which are in the train set of particular fold
        run_fold_train_list: List[List[str]] = [
            run_dict[query_id] for query_id in run_dict.keys() if query_id in train_queries_for_fold
        ]

        # Filter the run file to have only those queries which are in the test set of particular fold
        run_fold_test_list: List[List[str]] = [
            run_dict[query_id] for query_id in run_dict.keys() if query_id in test_queries_for_fold
        ]

        train_file_path: str = save_sir + '/' + 'set-' + str(fold_num) + '/' + train_file
        test_file_path: str = save_sir + '/' + 'set-' + str(fold_num) + '/' + test_file

        write_to_file(file_path=train_file_path, data=run_fold_train_list)
        write_to_file(file_path=test_file_path, data=run_fold_test_list)


def main():
    parser = argparse.ArgumentParser("Split a run/qrels file into train/test files.")
    parser.add_argument("--folds", help='File containing the fold queries.', required=True)
    parser.add_argument("--file", help='Source file to split.', required=True)
    parser.add_argument("--save", help='Directory where data will be saved.', required=True)
    parser.add_argument("--train", help='Name of train file.', required=True)
    parser.add_argument("--test", help='Name of test file.', required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Loading fold queries..')
    fold_query_dict: Dict[str, Dict[str, List[str]]] = read_folds(args.folds)
    print('[Done].')

    print('Loading file...')
    run_dict: Dict[str, List[str]] = read_run_file(args.file)
    print('[Done].')

    print('Creating folds from file...')
    create_folds_for_run(
        fold_queries=fold_query_dict,
        run_dict=run_dict,
        save_sir=args.save,
        train_file=args.train,
        test_file=args.test
    )
    print('[Done].')


if __name__ == '__main__':
    main()


