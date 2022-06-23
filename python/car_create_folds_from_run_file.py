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


def read_folds(query_fold_dir: str) -> Dict[str, List[str]]:

    # Separate the files from the directories
    folds = [f for f in listdir(query_fold_dir) if isfile(join(query_fold_dir, f))]

    fold_query_dict: Dict[str, List[str]] = dict(
        (fold, get_queries_in_fold(join(query_fold_dir, fold)))
        for fold in folds
    )

    return fold_query_dict


def get_queries_in_fold(fold_file_path: str) -> List[str]:
    with open(fold_file_path, 'r') as f:
        queries: List[str] = [line.split('\t')[0] for line in f]
    return queries


def read_run_file(run_file_path: str) -> Dict[str, List[str]]:
    run_dict: Dict[str, List[str]] = {}
    with open(run_file_path, 'r') as f:
        for line in f:
            query_id: str = line.split(" ")[0]
            query_run_strings_list: List[str] = run_dict[query_id] if query_id in run_dict.keys() else []
            query_run_strings_list.append(line)
            run_dict[query_id] = query_run_strings_list

    return run_dict


def create_folds_for_run(
        fold_query_dict: Dict[str, List[str]],
        run_dict: Dict[str, List[str]],
        save_dir: str
) -> None:

    fold_num: int = 0
    for _, queries in fold_query_dict.items():

        # Filter the run file to have only those queries which are in a particular fold
        fold_list: List[List[str]] = [run_dict[query] for query in queries]

        fold_run_file_name: str = 'fold-' + str(fold_num) + '.run'
        fold_run_file_path: str = join(save_dir, fold_run_file_name)
        fold_num += 1

        write_to_file(fold_run_file_path, fold_list)


def write_to_file(file_path: str, data: List[List[str]]) -> None:
    with open(file_path, 'a') as f:
        for run_file_strings in data:
            for run_string in run_file_strings:
                f.write('%s' % run_string)


def main():
    parser = argparse.ArgumentParser("Create folds from a run file.")
    parser.add_argument("--fold-dir", help='Path to directory containing the fold queries.', required=True)
    parser.add_argument("--run", help='Path to run file.', required=True)
    parser.add_argument("--save-dir", help='Path to the directory where run folds will be saved.', required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    print('Loading folds from directory...')
    fold_query_dict: Dict[str, List[str]] = read_folds(args.fold_dir)
    print('[Done].')

    print('Loading run file...')
    run_dict: Dict[str, List[str]] = read_run_file(args.run)
    print('[Done].')

    print('Creating folds from run file...')
    create_folds_for_run(
        fold_query_dict=fold_query_dict,
        run_dict=run_dict,
        save_dir=args.save_dir
    )
    print('[Done].')


if __name__ == '__main__':
    main()















