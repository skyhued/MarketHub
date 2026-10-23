import unittest
from datetime import datetime, timedelta

import numpy as np

import train
from train import build_matrix, evaluate_on_test, generate_similar_products, generate_user_recommends, split_by_time, top_k_recommends


class SplitByTimeTest(unittest.TestCase):

    def setUp(self):
        self.base = datetime(2026, 1, 1)
        self.rows = [(1, 100 + i, 1.0, self.base + timedelta(days=i)) for i in range(10)]

    def test_test_split_holds_out_the_newest_rows(self):
        train_rows, test_rows = split_by_time(self.rows, 0.2)

        self.assertEqual(8, len(train_rows))
        self.assertEqual(2, len(test_rows))
        self.assertEqual(self.rows[-1][3], test_rows[-1][3])
        self.assertLessEqual(max(r[3] for r in train_rows), min(r[3] for r in test_rows))

    def test_single_row_is_not_split(self):
        train_rows, test_rows = split_by_time(self.rows[:1], 0.2)

        self.assertEqual(1, len(train_rows))
        self.assertEqual([], test_rows)


class BuildMatrixTest(unittest.TestCase):

    def test_matrix_places_weights_and_leaves_gaps_at_zero(self):
        rows = [(1, 100, 0.5, None), (1, 101, 2.0, None), (2, 101, 1.0, None)]

        matrix, user_ids, prod_ids, user_map, prod_map = build_matrix(rows)

        self.assertEqual([1, 2], user_ids)
        self.assertEqual([100, 101], prod_ids)
        self.assertEqual((2, 2), matrix.shape)
        self.assertAlmostEqual(0.5, matrix[user_map[1], prod_map[100]])
        self.assertAlmostEqual(2.0, matrix[user_map[1], prod_map[101]])
        self.assertAlmostEqual(0.0, matrix[user_map[2], prod_map[100]])

    def test_no_rows_gives_empty_matrix(self):
        matrix, user_ids, prod_ids, user_map, prod_map = build_matrix([])

        self.assertEqual(0, matrix.size)
        self.assertEqual([], user_ids)
        self.assertEqual({}, prod_map)


class TopKRecommendsTest(unittest.TestCase):

    def test_seen_items_are_excluded_and_order_is_by_score(self):
        X = np.array([[1.0, 0.0]])
        Y = np.array([[3.0, 0.0], [2.0, 0.0], [1.0, 0.0]])

        result = top_k_recommends(X, Y, [7], [100, 101, 102], 2, {7: {100}})

        self.assertEqual([101, 102], result[7])

    def test_k_limits_list_length(self):
        X = np.array([[1.0, 0.0]])
        Y = np.array([[3.0, 0.0], [2.0, 0.0], [1.0, 0.0]])

        result = top_k_recommends(X, Y, [7], [100, 101, 102], 1, {})

        self.assertEqual(1, len(result[7]))


class EvaluateOnTestTest(unittest.TestCase):

    def test_metrics_are_computed_from_train_split_only(self):
        np.random.seed(0)
        rows = [
            (1, 100, 2.0, datetime(2026, 1, 1)),
            (1, 101, 1.0, datetime(2026, 1, 2)),
            (2, 101, 2.0, datetime(2026, 1, 1)),
            (2, 102, 1.0, datetime(2026, 1, 2)),
            (1, 102, 1.0, datetime(2026, 1, 20)),
            (2, 100, 1.0, datetime(2026, 1, 20)),
        ]
        train_rows, test_rows = split_by_time(rows, 0.2)

        metrics = evaluate_on_test(train_rows, test_rows, 2)

        self.assertEqual(2, metrics['k'])
        self.assertEqual(2, metrics['evaluated_users'])
        for key in ('precision_at_k', 'recall_at_k', 'ndcg_at_k', 'catalog_coverage', 'intra_list_diversity'):
            self.assertGreaterEqual(metrics[key], 0.0)
            self.assertLessEqual(metrics[key], 1.0)

    def test_empty_train_split_returns_zeros(self):
        metrics = evaluate_on_test([], [], 2)

        self.assertEqual(0, metrics['evaluated_users'])
        self.assertEqual(0.0, metrics['precision_at_k'])


class ServingOutputTest(unittest.TestCase):

    def test_recommends_are_capped_at_top_n(self):
        np.random.seed(1)
        X = np.ones((1, 2))
        Y = np.random.rand(30, 2)
        prod_ids = list(range(1000, 1030))

        recs = generate_user_recommends(X, Y, [7], prod_ids)

        self.assertEqual(train.TOP_N, len(recs))
        self.assertTrue(all(r[0] == 7 and r[3] == 'ALS+BPR协同过滤' for r in recs))

    def test_similar_products_skip_self_and_use_minus_one_user(self):
        Y = np.array([[1.0, 0.0], [0.9, 0.1], [0.0, 1.0]])

        sim = generate_similar_products(Y, [100, 101, 102])

        self.assertEqual(6, len(sim))
        self.assertTrue(all(r[0] == -1 for r in sim))
        self.assertTrue(all(r[1] != ref for r in sim for ref in [int(r[3].split('=')[1])]))


if __name__ == '__main__':
    unittest.main()
