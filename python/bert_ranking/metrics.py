from typing import List, Dict

import pytrec_eval


def get_metric(qrels: str, run: str, metric: str = 'map') -> float:

    # Read the qrel file
    with open(qrels, 'r') as f_qrel:
        qrel_dict = pytrec_eval.parse_qrel(f_qrel)

    # Read the run file
    with open(run, 'r') as f_run:
        run_dict = pytrec_eval.parse_run(f_run)

    # Evaluate
    evaluator = pytrec_eval.RelevanceEvaluator(qrel_dict, pytrec_eval.supported_measures)
    results = evaluator.evaluate(run_dict)
    mes = {}
    for _, query_measures in sorted(results.items()):
        for measure, value in sorted(query_measures.items()):
            mes[measure] = pytrec_eval.compute_aggregated_measure(measure,
                                                                  [query_measures[measure]
                                                                   for query_measures in results.values()])
    return mes[metric]


