import argparse
import torch
import torch.nn as nn
import os
import tqdm
from transformers import AutoTokenizer
import utils
from bert_model import PointWiseBert, PairWiseBert
from torch.utils.data import Dataset


def test(model, model_type, data_loader, device):
    rst_dict = {}
    model.eval()

    num_batch = len(data_loader)

    with torch.no_grad():
        for _, batch in tqdm.tqdm(enumerate(data_loader), total=num_batch):
            query_id, doc_id = batch['query_id'], batch['doc_id']
            batch_score, _ = model(
                batch['input_ids'].to(device),
                batch['attention_mask'].to(device),
                batch['token_type_ids'].to(device)
            )

            if model_type == 'pointwise':
                batch_score = batch_score.softmax(dim=-1)[:, 1].squeeze(-1)

            batch_score = batch_score.detach().cpu().tolist()
            for (q_id, d_id, b_s) in zip(query_id, doc_id, batch_score):
                if q_id not in rst_dict:
                    rst_dict[q_id] = {}

                if d_id not in rst_dict[q_id] or b_s > rst_dict[q_id][d_id][0]:
                    rst_dict[q_id][d_id] = [b_s]

    return rst_dict


def main():
    parser = argparse.ArgumentParser("Script to run inference on a fine-tuned model.")
    parser.add_argument('--model-type', help='Type of model (pairwise|pointwise). Default: pairwise.', type=str,
                        default='pairwise')
    parser.add_argument('--test', help='Test data.', required=True, type=str)
    parser.add_argument('--max-len', help='Maximum length for truncation/padding. Default: 512', default=512, type=int)
    parser.add_argument('--run', help='Output run file.', required=True, type=str)
    parser.add_argument('--eval-run', help='Whether or not to evaluate the run file. Default: False.',
                        action='store_true')
    parser.add_argument('--qrels', help='Ground truth file for evaluation.', type=str)
    parser.add_argument('--checkpoint', help='Name of checkpoint to load.', type=str, default=None)
    parser.add_argument('--mode', help='Mode (cls|pooling). Default: cls.', type=str, default='cls')
    parser.add_argument('--batch-size', help='Size of each batch. Default: 8.', type=int, default=8)
    parser.add_argument('--num-workers', help='Number of workers for DataLoader. Default: 8', type=int, default=8)
    parser.add_argument('--use-cuda', help='Whether to use CUDA. Default: False', action='store_true')
    parser.add_argument('--cuda', help='CUDA device number. Default: 0.', type=int, default=0)

    args = parser.parse_args()

    cuda_device = 'cuda:' + str(args.cuda)
    print('CUDA Device: {} '.format(cuda_device))

    device = torch.device(
        cuda_device if torch.cuda.is_available() and args.use_cuda else 'cpu'
    )

    pretrain = vocab = 'bert-base-uncased'
    tokenizer = AutoTokenizer.from_pretrained(vocab)

    print('Reading test data....')
    test_set = utils.read_data(
        data=args.test,
        train=False,
        tokenizer=tokenizer,
        max_len=args.max_len,
        model_type=args.model_type
    )
    print('[Done].')

    print('Creating data loader...')
    test_loader = utils.create_data_loaders(
        data_set=test_set,
        shuffle=False,
        batch_size=args.batch_size,
        num_workers=args.num_workers
    )

    print('[Done].')

    if args.model_type == 'pairwise':
        model = PairWiseBert(
            pretrained=pretrain,
            mode=args.mode
        )
    elif args.model_type == 'pointwise':
        model = PointWiseBert(
            pretrained=pretrain,
            mode=args.mode
        )
    else:
        raise ValueError('Model type must be `pairwise` or `pointwise`.')

    if args.checkpoint is not None:
        print('Loading checkpoint...')
        model.load_state_dict(torch.load(args.checkpoint, map_location=device))
        print('[Done].')

    model.to(device)

    print('Running inference...')

    res_dict = test(
        model=model,
        model_type=args.model_type,
        data_loader=test_loader,
        device=device
    )
    print('Writing run file...')
    utils.save_trec(args.run, res_dict)
    print('[Done].')

    if args.eval_run:
        test_metric = metrics.get_metric(qrels=args.qrels, run=args.run, metric=args.metric)
        print('{} = {:.4f}'.format(args.metric, test_metric))

    print('[Done].')
    print('Run file saved to ==> {}'.format(args.run))


if __name__ == "__main__":
    main()
