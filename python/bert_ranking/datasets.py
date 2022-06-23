from typing import List, Tuple, Dict, Any
import json
import torch
from torch.utils.data import Dataset
from transformers import AutoTokenizer

class BertDataset(Dataset):
    def __init__(
            self,
            dataset,
            tokenizer: AutoTokenizer,
            train: bool,
            max_len: int,
            model: str
    ) -> None:

        self._dataset = dataset
        self._tokenizer = tokenizer
        self._train = train
        self._max_len = max_len
        self._model = model

        self._read_data()

        self._count = len(self._examples)

    def _read_data(self):
        with open(self._dataset, 'r') as f:
            self._examples = [json.loads(line) for i, line in enumerate(f)]


    def _create_bert_input(self, query, document):
        # Tokenize all of the sentences and map the tokens to their word IDs.
        encoded_dict = self._tokenizer.encode_plus(
            text=query,
            text_pair=document,
            add_special_tokens=True,  # Add '[CLS]' and '[SEP]'
            max_length=self._max_len,  # Pad & truncate all sentences.
            padding='max_length',
            truncation=True,
            return_attention_mask=True,  # Construct attn. masks.
            return_token_type_ids=True  # Construct token type ids
        )

        return encoded_dict['input_ids'], encoded_dict['attention_mask'], encoded_dict['token_type_ids']

    def __len__(self) -> int:
        return self._count

    def collate(self, batch):
        if self._train:
            if self._model == 'pairwise':
                input_ids_pos = torch.tensor([item['input_ids_pos'] for item in batch])
                token_type_ids_pos = torch.tensor([item['token_type_ids_pos'] for item in batch])
                attention_mask_pos = torch.tensor([item['attention_mask_pos'] for item in batch])
                input_ids_neg = torch.tensor([item['input_ids_neg'] for item in batch])
                token_type_ids_neg = torch.tensor([item['token_type_ids_neg'] for item in batch])
                attention_mask_neg = torch.tensor([item['attention_mask_neg'] for item in batch])
                return {
                    'input_ids_pos': input_ids_pos,
                    'token_type_ids_pos': token_type_ids_pos,
                    'attention_mask_pos': attention_mask_pos,
                    'input_ids_neg': input_ids_neg,
                    'token_type_ids_neg': token_type_ids_neg,
                    'attention_mask_neg': attention_mask_neg
                }
            elif self._model == 'pointwise':
                input_ids = torch.tensor([item['input_ids'] for item in batch])
                token_type_ids = torch.tensor([item['token_type_ids'] for item in batch])
                attention_mask = torch.tensor([item['attention_mask'] for item in batch])
                label = torch.tensor([item['label'] for item in batch])
                return {
                    'input_ids': input_ids,
                    'token_type_ids': token_type_ids,
                    'attention_mask': attention_mask,
                    'label': label
                }
            else:
                raise ValueError('Model type must be `pairwise` or `pointwise`.')
        else:
            query_id = [item['query_id'] for item in batch]
            doc_id = [item['doc_id'] for item in batch]
            label = [item['label'] for item in batch]
            input_ids = torch.tensor([item['input_ids'] for item in batch])
            token_type_ids = torch.tensor([item['token_type_ids'] for item in batch])
            attention_mask = torch.tensor([item['attention_mask'] for item in batch])
            return {
                'query_id': query_id,
                'doc_id': doc_id,
                'label': label,
                'input_ids': input_ids,
                'token_type_ids': token_type_ids,
                'attention_mask': attention_mask
            }

    def __getitem__(self, index: int) -> Dict[str, Any]:
        example = self._examples[index]
        if self._train:
            if self._model == 'pairwise':
                input_ids_pos, attention_mask_pos, token_type_ids_pos = self._create_bert_input(example['query'],
                                                                                                example['doc_pos'])
                input_ids_neg, attention_mask_neg, token_type_ids_neg = self._create_bert_input(example['query'],
                                                                                                example['doc_neg'])
                return {
                    'input_ids_pos': input_ids_pos,
                    'token_type_ids_pos': token_type_ids_pos,
                    'attention_mask_pos': attention_mask_pos,
                    'input_ids_neg': input_ids_neg, 'token_type_ids_neg': token_type_ids_neg,
                    'attention_mask_neg': attention_mask_neg
                }
            elif self._model == 'pointwise':
                input_ids, attention_mask, token_type_ids = self._create_bert_input(example['query'], example['doc'])
                return {
                    'input_ids': input_ids,
                    'token_type_ids': token_type_ids,
                    'attention_mask': attention_mask,
                    'label': example['label']
                }
            else:
                raise ValueError('Model type must be `pairwise` or `pointwise`.')
        else:
            input_ids, attention_mask, token_type_ids = self._create_bert_input(example['query'], example['doc'])
            return {
                'query_id': example['query_id'],
                'doc_id': example['doc_id'],
                'label': example['label'],
                'input_ids': input_ids,
                'attention_mask': attention_mask,
                'token_type_ids': token_type_ids
            }