import unittest

import numpy as np

from metrics import (
    catalog_coverage,
    evaluate,
    intra_list_diversity,
    ndcg_at_k,
    precision_at_k,
    recall_at_k,
)


class PrecisionRecallTest(unittest.TestCase):

    def test_precision_at_k_counts_hits_in_top_k(self):
        self.assertAlmostEqual(0.4, precision_at_k([1, 2, 3, 4, 5], {2, 5}, 5))
        self.assertAlmostEqual(0.5, precision_at_k([1, 2, 3, 4, 5], {2, 5}, 2))

    def test_recall_at_k_divides_by_relevant_size(self):
        self.assertAlmostEqual(1.0, recall_at_k([1, 2, 3, 4, 5], {2, 5}, 5))
        self.assertAlmostEqual(0.5, recall_at_k([1, 2, 3, 4, 5], {2, 5}, 2))

    def test_empty_relevant_set_scores_zero(self):
        self.assertAlmostEqual(0.0, recall_at_k([1, 2], set(), 2))
        self.assertAlmostEqual(0.0, ndcg_at_k([1, 2], set(), 2))

    def test_precision_divides_by_k_not_by_list_length(self):
        self.assertAlmostEqual(0.2, precision_at_k([7], {7}, 5))
        self.assertAlmostEqual(1.0, recall_at_k([7], {7}, 5))


class NdcgTest(unittest.TestCase):

    def test_perfect_ranking_scores_one(self):
        self.assertAlmostEqual(1.0, ndcg_at_k([2, 5, 1], {2, 5}, 3))

    def test_lower_rank_scores_less(self):
        high = ndcg_at_k([2, 1, 3], {2}, 3)
        low = ndcg_at_k([1, 3, 2], {2}, 3)
        self.assertGreater(high, low)
        self.assertAlmostEqual(1.0 / np.log2(4), low)

    def test_ideal_gain_is_capped_by_k(self):
        self.assertAlmostEqual(1.0, ndcg_at_k([4, 5], {4, 5, 6, 7}, 2))


class CoverageTest(unittest.TestCase):

    def test_coverage_is_distinct_items_over_catalog(self):
        self.assertAlmostEqual(0.75, catalog_coverage([[1, 2], [2, 3]], 4))

    def test_zero_catalog_scores_zero(self):
        self.assertAlmostEqual(0.0, catalog_coverage([[1]], 0))


class DiversityTest(unittest.TestCase):

    def test_orthogonal_vectors_are_fully_diverse(self):
        vectors = {1: np.array([1.0, 0.0]), 2: np.array([0.0, 1.0])}
        self.assertAlmostEqual(1.0, intra_list_diversity([[1, 2]], vectors))

    def test_identical_vectors_are_not_diverse(self):
        vectors = {1: np.array([1.0, 1.0]), 2: np.array([2.0, 2.0])}
        self.assertAlmostEqual(0.0, intra_list_diversity([[1, 2]], vectors))

    def test_single_item_list_is_skipped(self):
        vectors = {1: np.array([1.0, 0.0])}
        self.assertAlmostEqual(0.0, intra_list_diversity([[1]], vectors))


class EvaluateTest(unittest.TestCase):

    def test_averages_over_users_with_test_items(self):
        recommendations = {1: [10, 11], 2: [10, 12], 3: [20, 21]}
        test_relevant = {1: {10}, 2: {12}, 3: set()}
        vectors = {i: np.array([float(i), 1.0]) for i in (10, 11, 12, 20, 21)}

        result = evaluate(recommendations, test_relevant, vectors, 6, 2)

        self.assertEqual(2, result['evaluated_users'])
        self.assertAlmostEqual(0.5, result['precision_at_k'])
        self.assertAlmostEqual(1.0, result['recall_at_k'])
        self.assertGreater(result['ndcg_at_k'], 0.0)
        self.assertAlmostEqual(0.5, result['catalog_coverage'])

    def test_no_evaluable_user_returns_zeros(self):
        result = evaluate({1: [10]}, {1: set()}, {10: np.array([1.0])}, 3, 2)

        self.assertEqual(0, result['evaluated_users'])
        self.assertAlmostEqual(0.0, result['precision_at_k'])
        self.assertAlmostEqual(0.0, result['ndcg_at_k'])


if __name__ == '__main__':
    unittest.main()
