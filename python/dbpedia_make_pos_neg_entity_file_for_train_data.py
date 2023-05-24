import sys
import os
import argparse
from typing import List, Dict, Set, Tuple

'''
The DBpedia dataset contains graded relevance judgements. So both positive and negative entities are present in the
qrels file. We separate this qrels file into two files: one containing the positive entities and the other containing
the negative entities.
'''


def create_file(in_file: str, pos_out_file: str, neg_out_file: str) -> None:
    pos_ent_dict, neg_ent_dict = get_pos_neg_ent_dict(in_file=in_file)
    pos_run_file_strings: List[str] = make_file_strings(data_dict=pos_ent_dict)
    neg_run_file_strings: List[str] = make_file_strings(data_dict=neg_ent_dict)
    write_to_file(data=pos_run_file_strings, output_file=pos_out_file)
    write_to_file(data=neg_run_file_strings, output_file=neg_out_file)

    print('Positive entities file written to: {}'.format(pos_out_file))
    print('Negative entities file written to: {}'.format(neg_out_file))


def get_pos_neg_ent_dict(in_file: str) -> Tuple[Dict[str, Set[str]], Dict[str, Set[str]]]:
    pos_ent_dict: Dict[str, Set[str]] = {}
    neg_ent_dict: Dict[str, Set[str]] = {}
    with open(in_file, 'r') as file:
        for line in file:
            line_parts: List[str] = line.split(" ")
            query_id: str = line_parts[0]
            entity_id: str = line_parts[2]
            pos_entity_set: Set[str] = pos_ent_dict[query_id] if query_id in pos_ent_dict.keys() else set()
            neg_entity_set: Set[str] = neg_ent_dict[query_id] if query_id in neg_ent_dict.keys() else set()
            rel: int = int(line_parts[3])
            if rel > 0:
                pos_entity_set.add(entity_id)
            else:
                neg_entity_set.add(entity_id)
            pos_ent_dict[query_id] = pos_entity_set
            neg_ent_dict[query_id] = neg_entity_set
    return pos_ent_dict, neg_ent_dict


def write_to_file(data: List[str], output_file: str) -> None:
    with open(output_file, 'a') as f:
        for item in data:
            f.write("%s\n" % item)


def make_file_strings(data_dict: Dict[str, Set[str]]) -> List[str]:
    file_strings: List[str] = []

    for query_id, entity_set in data_dict.items():
        for entity_id in entity_set:
            file_strings.append(query_id + ' 0 ' + entity_id)

    return file_strings


def main():
    parser = argparse.ArgumentParser("Create two files, one with positive entities and another with negative entities.")
    parser.add_argument("--qrels", help='Graded ground truth judgements', required=True)
    parser.add_argument("--save", help='Directory where files will be saved.', required=True)
    args = parser.parse_args(args=None if sys.argv[1:] else ['--help'])

    pos_out_file: str = args.save + '/train.pos_ent.txt'
    neg_out_file: str = args.save + '/train.neg_ent.txt'
    create_file(in_file=args.qrels, pos_out_file=pos_out_file, neg_out_file=neg_out_file)


if __name__ == '__main__':
    main()
