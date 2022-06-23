from typing import Tuple
import torch
import torch.nn as nn
from transformers import AutoConfig, AutoModel, DistilBertModel
import warnings
class Bert(nn.Module):
    def __init__(self, pretrained: str) -> None:

        super(Bert, self).__init__()
        self._pretrained = pretrained
        self._config = AutoConfig.from_pretrained(self._pretrained)
        self.bert = AutoModel.from_pretrained(self._pretrained, config=self._config)


    def forward(
            self,
            input_ids: torch.Tensor,
            attention_mask: torch.Tensor = None,
            token_type_ids: torch.Tensor = None
    ):


        output = self.bert(input_ids=input_ids, attention_mask=attention_mask, token_type_ids=token_type_ids)
        last_hidden_state = output.last_hidden_state
        logits = last_hidden_state[:, 0, :]
        score = self.classifier(logits).squeeze(-1)
        return score, logits


class PointWiseBert(Bert):
    def __init__(self, pretrained: str) -> None:

        Bert.__init__(self, pretrained)
        self.classifier = nn.Linear(self._config.hidden_size, 2)


class PairWiseBert(Bert):
    def __init__(self, pretrained: str) -> None:

        Bert.__init__(self, pretrained)
        self.classifier = nn.Linear(self._config.hidden_size, 1)
