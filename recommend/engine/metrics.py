import numpy as np


def precision_at_k(recommended, relevant, k):
    if k <= 0:
        return 0.0
    top = list(recommended)[:k]
    hits = sum(1 for pid in top if pid in relevant)
    return hits / k


def recall_at_k(recommended, relevant, k):
    if not relevant:
        return 0.0
    top = list(recommended)[:k]
    hits = sum(1 for pid in top if pid in relevant)
    return hits / len(relevant)


def ndcg_at_k(recommended, relevant, k):
    if not relevant:
        return 0.0
    dcg = 0.0
    for rank, pid in enumerate(list(recommended)[:k], start=1):
        if pid in relevant:
            dcg += 1.0 / np.log2(rank + 1)
    ideal_hits = min(len(relevant), k)
    idcg = sum(1.0 / np.log2(rank + 1) for rank in range(1, ideal_hits + 1))
    if idcg == 0.0:
        return 0.0
    return dcg / idcg


def catalog_coverage(recommended_lists, catalog_size):
    if catalog_size <= 0:
        return 0.0
    distinct = set()
    for items in recommended_lists:
        distinct.update(items)
    return len(distinct) / catalog_size


def intra_list_diversity(recommended_lists, item_vectors):
    per_user = []
    for items in recommended_lists:
        vectors = [item_vectors[pid] for pid in items if pid in item_vectors]
        if len(vectors) < 2:
            continue
        similarities = []
        for i in range(len(vectors)):
            for j in range(i + 1, len(vectors)):
                similarities.append(cosine_similarity(vectors[i], vectors[j]))
        per_user.append(1.0 - float(np.mean(similarities)))
    if not per_user:
        return 0.0
    return float(np.mean(per_user))


def cosine_similarity(a, b):
    denominator = float(np.linalg.norm(a) * np.linalg.norm(b))
    if denominator == 0.0:
        return 0.0
    return float(np.dot(a, b) / denominator)


def evaluate(recommendations, test_relevant, item_vectors, catalog_size, k):
    evaluated_users = [
        uid for uid in test_relevant
        if len(test_relevant[uid]) > 0 and uid in recommendations
    ]
    lists = [recommendations[uid] for uid in evaluated_users]
    if evaluated_users:
        precision = float(np.mean([precision_at_k(recommendations[uid], test_relevant[uid], k) for uid in evaluated_users]))
        recall = float(np.mean([recall_at_k(recommendations[uid], test_relevant[uid], k) for uid in evaluated_users]))
        ndcg = float(np.mean([ndcg_at_k(recommendations[uid], test_relevant[uid], k) for uid in evaluated_users]))
    else:
        precision = recall = ndcg = 0.0
    return {
        'k': k,
        'evaluated_users': len(evaluated_users),
        'precision_at_k': precision,
        'recall_at_k': recall,
        'ndcg_at_k': ndcg,
        'catalog_coverage': catalog_coverage(lists, catalog_size),
        'intra_list_diversity': intra_list_diversity(lists, item_vectors),
    }
