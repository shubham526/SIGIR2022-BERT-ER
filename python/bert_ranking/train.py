import configparser
import os
import sys
import time
import json
import torch
import torch.nn as nn
import utils
import metrics
import warnings
import argparse
from typing import Tuple
from torch.utils.data import Dataset
from model import PointWiseBert, PairWiseBert
from datasets import BertDataset
from dataloader import BertDataLoader
from trainers import BertTrainer
from transformers import AutoTokenizer, get_linear_schedule_with_warmup


def train(model, model_type, trainer, epochs, metric, qrels, valid_loader, save_path, save, run_file, eval_every, device):
    best_valid_metric = 0.0

    for epoch in range(epochs):

        # Train
        start_time = time.time()
        print('Training on train set...')
        train_loss = trainer.train()

        # Validate
        if (epoch + 1) % eval_every == 0:
            print('Evaluating on dev set...')
            res_dict = utils.evaluate(
                model=model,
                model_type=model_type,
                data_loader=valid_loader,
                device=device
            )

            utils.save_trec(os.path.join(save_path, run_file), res_dict)
            valid_metric = metrics.get_metric(qrels, os.path.join(save_path, run_file), metric)

            if valid_metric >= best_valid_metric:
                best_valid_metric = valid_metric
                utils.save_checkpoint(os.path.join(save_path, save), model)

            end_time = time.time()
            epoch_mins, epoch_secs = utils.epoch_time(start_time, end_time)

            print(f'Epoch: {epoch + 1:02} | Epoch Time: {epoch_mins}m {epoch_secs}s')
            print(
                f'\t Train Loss: {train_loss:.3f}| Val. Metric: {valid_metric:.4f} | Best Val. Metric: {best_valid_metric:.4f}')


def main():
    parser = argparse.ArgumentParser("Script to train a model.")
    parser.add_argument('--model-type', help='Type of model (pairwise|pointwise). Default: pairwise.', type=str,
                        default='pairwise')
    parser.add_argument('--train', help='Training data.', required=True, type=str)
    parser.add_argument('--max-len', help='Maximum length for truncation/padding. Default: 512', default=512, type=int)
    parser.add_argument('--save-dir', help='Directory where model is saved.', required=True, type=str)
    parser.add_argument('--dev', help='Development data.', required=True, type=str)
    parser.add_argument('--qrels', help='Ground truth file in TREC format.', required=True, type=str)
    parser.add_argument('--save', help='Name of checkpoint to save. Default: bert.bin', default='bert.bin', type=str)
    parser.add_argument('--checkpoint', help='Name of checkpoint to load. Default: None', default=None, type=str)
    parser.add_argument('--run', help='Output run file in TREC format. Default: dev.run', default='dev.run',
                        type=str)
    parser.add_argument('--metric', help='Metric to use for evaluation. Default: map', default='map', type=str)
    parser.add_argument('--epoch', help='Number of epochs. Default: 20', type=int, default=20)
    parser.add_argument('--batch-size', help='Size of each batch. Default: 8.', type=int, default=8)
    parser.add_argument('--learning-rate', help='Learning rate. Default: 2e-5.', type=float, default=2e-5)
    parser.add_argument('--n-warmup-steps', help='Number of warmup steps for scheduling. Default: 1000.', type=int,
                        default=1000)
    parser.add_argument('--eval-every', help='Evaluate every number of epochs. Default: 1', type=int, default=1)
    parser.add_argument('--num-workers', help='Number of workers to use for DataLoader. Default: 8', type=int,
                        default=8)
    parser.add_argument('--freeze-bert', help='Whether to freeze the BERT parameters for training. Default: False.', action="store_true")
    parser.add_argument('--cuda', help='CUDA device number. Default: 0.', type=int, default=0)
    parser.add_argument('--use-cuda', help='Whether or not to use CUDA. Default: False.', action='store_true')
    args = parser.parse_args()

    cuda_device = 'cuda:' + str(args.cuda)
    print('CUDA Device: {} '.format(cuda_device))

    device = torch.device(
        cuda_device if torch.cuda.is_available() and args.use_cuda else 'cpu'
    )

    pretrain = vocab = 'bert-base-uncased'
    model_config = json.dumps({
        'Model Type': args.model_type,
        'Max Input': args.max_len,
        'Model': pretrain,
        'Metric': args.metric,
        'Epochs': args.epoch,
        'Batch Size': args.batch_size,
        'Learning Rate': args.learning_rate,
        'Warmup Steps': args.n_warmup_steps,
    })
    config_file: str = os.path.join(args.save_dir, 'config.json')
    with open(config_file, 'w') as f:
        f.write("%s\n" % model_config)

    tokenizer = AutoTokenizer.from_pretrained(vocab)
    print('Reading train data...')
    train_set =  BertDataset(
        dataset=args.train,
        tokenizer=tokenizer,
        train=True,
        max_len=args.max_len,
        model=args.model_type
    )
    print('[Done].')

    print('Reading dev data...')
    dev_set =  BertDataset(
        dataset=args.dev,
        tokenizer=tokenizer,
        train=False,
        max_len=args.max_len,
        model=args.model_type
    )
    print('[Done].')

    print('Creating data loaders...')
    print('Number of workers = ' + str(args.num_workers))
    print('Batch Size = ' + str(args.batch_size))
    train_loader = BertDataLoader(
        dataset=train_set,
        batch_size=args.batch_size,
        shuffle=True,
        num_workers=args.num_workers
    )

    dev_loader = BertDataLoader(
        dataset=dev_set,
        batch_size=args.batch_size,
        shuffle=False,
        num_workers=args.num_workers
    )
    print('[Done].')

    print('Model Type: ' + args.model_type)

    if args.model_type == 'pairwise':
        model = PairWiseBert(pretrained=pretrain)
        loss_fn = nn.MarginRankingLoss(margin=1)
    elif args.model_type == 'pointwise':
        model = PointWiseBert(pretrained=pretrain)
        loss_fn = nn.CrossEntropyLoss()
    else:
        raise ValueError('Model type must be `pairwise` or `pointwise`.')

    if args.checkpoint is not None:
        print('Loading checkpoint...')
        model.load_state_dict(torch.load(args.checkpoint))
        print('[Done].')

    if args.freeze_bert:
        warnings.warn('BERT parameters frozen for training.')
        for param in model.bert.parameters():
            param.requires_grad = False
    else:
        warnings.warn('BERT parameters not frozen for training. If you do want this, set the `--freeze-bert` flag.')

    optimizer = torch.optim.Adam(filter(lambda p: p.requires_grad, model.parameters()), lr=args.learning_rate)
    scheduler = get_linear_schedule_with_warmup(
        optimizer,
        num_warmup_steps=args.n_warmup_steps,
        num_training_steps=len(train_set) * args.epoch // args.batch_size)

    print('Using device: {}'.format(device))
    model.to(device)
    loss_fn.to(device)
    trainer = BertTrainer(
            model=model,
            optimizer=optimizer,
            criterion=loss_fn,
            scheduler=scheduler,
            metric=args.metric,
            data_loader=train_loader,
            model_type=args.model_type,
            use_cuda=args.use_cuda,
            device=device
        )

    train(
        model=model,
        model_type=args.model_type,
        trainer=trainer,
        epochs=args.epoch,
        metric=args.metric,
        qrels=args.qrels,
        valid_loader=dev_loader,
        save_path=args.save_dir,
        save=args.save,
        run_file=args.run,
        eval_every=args.eval_every,
        device=device
    )

    print('Training complete.')


if __name__ == '__main__':
    main()
