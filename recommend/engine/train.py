"""
基于交替最小二乘法(ALS) + 贝叶斯个性化排序(BPR)的隐式反馈推荐引擎
参考 implicit 论文思想，numpy 从零实现，未复制任何 implicit 源码
"""
import os
import numpy as np
import pymysql
from datetime import datetime

from metrics import evaluate

def db_config():
    return {
        'host': os.environ.get('MYSQL_HOST', 'localhost'),
        'port': int(os.environ.get('MYSQL_PORT', '3306')),
        'user': os.environ.get('MYSQL_USER', 'markethub_user'),
        'password': os.environ.get('MYSQL_PASSWORD', ''),
        'database': os.environ.get('MYSQL_DATABASE', 'market_hub'),
        'charset': 'utf8mb4'
    }

FACTORS = 20           # 隐向量维度
ALS_ITERS = 15         # ALS 迭代次数
BPR_ITERS = 30         # BPR 迭代次数
REG = 0.1              # 正则化系数
ALPHA = 40             # 隐式反馈置信度
BPR_LR = 0.01          # BPR 学习率
TOP_N = 20             # 每人推荐商品数
SIMILAR_COUNT = 5      # 每个商品找几个相似商品
TEST_RATIO = 0.2       # 按时间切分时留作测试集的比例
EVAL_K = 5             # 离线指标的 K
RANDOM_SEED = 42       # 随机种子

def load_behaviors():
    conn = pymysql.connect(**db_config())
    cursor = conn.cursor()
    cursor.execute(
        "SELECT user_id, product_id, SUM(weight) AS w, MAX(create_time) AS t "
        "FROM user_behavior GROUP BY user_id, product_id"
    )
    rows = cursor.fetchall()
    conn.close()
    return [(r[0], r[1], float(r[2]), r[3]) for r in rows]

def build_matrix(rows):
    user_ids = sorted({r[0] for r in rows})
    prod_ids = sorted({r[1] for r in rows})
    if not user_ids or not prod_ids:
        return np.zeros((0, 0)), user_ids, prod_ids, {}, {}

    user_map = {uid: i for i, uid in enumerate(user_ids)}
    prod_map = {pid: i for i, pid in enumerate(prod_ids)}

    matrix = np.zeros((len(user_ids), len(prod_ids)))
    for uid, pid, w, _ in rows:
        matrix[user_map[uid], prod_map[pid]] = w

    return matrix, user_ids, prod_ids, user_map, prod_map

def split_by_time(rows, test_ratio):
    if len(rows) < 2:
        return rows, []
    ordered = sorted(rows, key=lambda r: r[3])
    cut = int(len(ordered) * (1.0 - test_ratio))
    cut = min(max(cut, 1), len(ordered) - 1)
    return ordered[:cut], ordered[cut:]

def als_train(matrix, factors, iters, reg, alpha):
    """交替最小二乘法：利用置信度对角特性，向量化求解"""
    np.random.seed(RANDOM_SEED)
    Cu = 1.0 + alpha * matrix          # 置信度
    P  = (matrix > 0).astype(np.float64)  # 偏好
    n_users, n_items = Cu.shape

    X = np.random.randn(n_users, factors) * 0.01
    Y = np.random.randn(n_items, factors) * 0.01

    for it in range(iters):
        YtY = Y.T @ Y
        for u in range(n_users):
            cu = Cu[u]
            A = (Y.T * cu) @ Y + reg * np.eye(factors)
            b = (Y.T * cu) @ P[u]
            X[u] = np.linalg.solve(A, b)

        XtX = X.T @ X
        for i in range(n_items):
            ci = Cu[:, i]
            A = (X.T * ci) @ X + reg * np.eye(factors)
            b = (X.T * ci) @ P[:, i]
            Y[i] = np.linalg.solve(A, b)

        if (it + 1) % 5 == 0:
            print(f"  ALS iter {it + 1}/{iters}")

    return X, Y

def bpr_finetune(X, Y, matrix, iters, lr, reg):
    """
    贝叶斯个性化排序：微调隐向量，优化 pairwise 排序
    核心思路：已交互商品 (i) 的预测分应高于未交互商品 (j)
    loss = -ln(σ(x_u·(y_i - y_j))) + λ||θ||²
    随机梯度更新 x_u, y_i, y_j
    """
    n_users, n_items = matrix.shape
    interacted = [set(np.where(row > 0)[0]) for row in matrix]
    all_items = set(range(n_items))

    for it in range(iters):
        loss_sum = 0
        count = 0
        for u in range(n_users):
            pos_set = interacted[u]
            if not pos_set:
                continue

            i = np.random.choice(list(pos_set))
            neg_candidates = list(all_items - pos_set - {i})
            if not neg_candidates:
                continue
            j = np.random.choice(neg_candidates)

            xu = X[u]
            yi, yj = Y[i], Y[j]
            diff = xu @ (yi - yj)
            sigmoid = 1.0 / (1.0 + np.exp(diff))

            grad = sigmoid
            X[u] += lr * (grad * (yi - yj) - reg * xu)
            Y[i] += lr * (grad * xu - reg * yi)
            Y[j] += lr * (-grad * xu - reg * yj)

            loss_sum += np.log(sigmoid + 1e-10)
            count += 1

        if (it + 1) % 10 == 0:
            avg_loss = -loss_sum / max(count, 1)
            print(f"  BPR iter {it + 1}/{iters}, loss={avg_loss:.4f}")

    return X, Y

def top_k_recommends(X, Y, user_ids, prod_ids, k, exclude):
    scores = X @ Y.T
    results = {}
    for u_idx, uid in enumerate(user_ids):
        seen = exclude.get(uid, set())
        order = np.argsort(-scores[u_idx])
        results[uid] = [prod_ids[i] for i in order if prod_ids[i] not in seen][:k]
    return results

def evaluate_on_test(train_rows, test_rows, k):
    matrix, user_ids, prod_ids, user_map, prod_map = build_matrix(train_rows)
    if matrix.size == 0:
        return evaluate({}, {}, {}, 0, k)

    X, Y = als_train(matrix, FACTORS, ALS_ITERS, REG, ALPHA)
    X, Y = bpr_finetune(X, Y, matrix, BPR_ITERS, BPR_LR, REG)

    interacted = {}
    for uid, pid, w, t in train_rows:
        interacted.setdefault(uid, set()).add(pid)

    recommendations = top_k_recommends(X, Y, user_ids, prod_ids, k, interacted)

    test_relevant = {}
    for uid, pid, w, t in test_rows:
        test_relevant.setdefault(uid, set()).add(pid)

    item_vectors = {pid: Y[i] for i, pid in enumerate(prod_ids)}
    return evaluate(recommendations, test_relevant, item_vectors, len(prod_ids), k)

def generate_user_recommends(X, Y, user_ids, prod_ids):
    scores = X @ Y.T
    results = []
    for u_idx, uid in enumerate(user_ids):
        top = np.argsort(-scores[u_idx])
        count = 0
        for idx in top:
            if scores[u_idx, idx] <= 0:
                break
            results.append((
                uid, prod_ids[idx],
                round(float(scores[u_idx, idx]), 4),
                'ALS+BPR协同过滤'
            ))
            count += 1
            if count >= TOP_N:
                break
    return results

def generate_similar_products(Y, prod_ids):
    norms = np.linalg.norm(Y, axis=1, keepdims=True) + 1e-10
    Y_norm = Y / norms
    sim = Y_norm @ Y_norm.T
    np.fill_diagonal(sim, -1.0)

    results = []
    for i, pid in enumerate(prod_ids):
        top = np.argsort(-sim[i])[:SIMILAR_COUNT]
        for j in top:
            if j == i: continue
            results.append((
                -1, prod_ids[j],
                round(float(sim[i, j]), 4),
                f'相似|ref={pid}'
            ))
    return results

def get_hot_products(cursor):
    cursor.execute(
        "SELECT id FROM product "
        "WHERE status=1 AND audit_status=1 "
        "ORDER BY (COALESCE(view_count,0)+COALESCE(sales_count,0)) DESC LIMIT %s",
        (TOP_N,)
    )
    return [r[0] for r in cursor.fetchall()]

def save_results(recs, similar, hot_pids):
    conn = pymysql.connect(**db_config())
    cursor = conn.cursor()
    cursor.execute("DELETE FROM recommendation_cache")

    for uid, pid, score, reason in recs:
            cursor.execute(
                "INSERT IGNORE INTO recommendation_cache (user_id,product_id,score,reason) "
                "VALUES (%s,%s,%s,%s)", (uid, pid, score, reason)
            )
    for uid, pid, score, reason in similar:
        cursor.execute(
            "INSERT IGNORE INTO recommendation_cache (user_id,product_id,score,reason) "
            "VALUES (%s,%s,%s,%s)", (uid, pid, score, reason)
        )
    for i, pid in enumerate(hot_pids):
        cursor.execute(
            "INSERT IGNORE INTO recommendation_cache (user_id,product_id,score,reason) "
            "VALUES (%s,%s,%s,%s)", (0, pid, float(TOP_N - i), '热门商品')
        )
    conn.commit()
    conn.close()

def save_eval_log(status, note, user_count, item_count, behavior_count, test_count,
                  metrics, elapsed_seconds):
    conn = pymysql.connect(**db_config())
    cursor = conn.cursor()
    cursor.execute(
        "INSERT INTO recommend_eval_log (status, note, user_count, item_count, behavior_count, "
        "test_behavior_count, k_value, precision_at_k, recall_at_k, ndcg_at_k, catalog_coverage, "
        "intra_list_diversity, elapsed_seconds) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
        (
            status, note, user_count, item_count, behavior_count, test_count,
            metrics.get('k'),
            metrics.get('precision_at_k'),
            metrics.get('recall_at_k'),
            metrics.get('ndcg_at_k'),
            metrics.get('catalog_coverage'),
            metrics.get('intra_list_diversity'),
            round(elapsed_seconds, 2),
        )
    )
    conn.commit()
    conn.close()

def print_metrics(metrics):
    print(f"   评估用户数: {metrics['evaluated_users']}, K={metrics['k']}")
    print(f"   Precision@{metrics['k']}: {metrics['precision_at_k']:.4f}")
    print(f"   Recall@{metrics['k']}: {metrics['recall_at_k']:.4f}")
    print(f"   NDCG@{metrics['k']}: {metrics['ndcg_at_k']:.4f}")
    print(f"   Catalog Coverage: {metrics['catalog_coverage']:.4f}")
    print(f"   Intra-list Diversity: {metrics['intra_list_diversity']:.4f}")

def main():
    started = datetime.now()
    print(f"[{started}] 开始训练推荐模型...")

    print("1/6 加载行为数据...")
    rows = load_behaviors()
    user_ids = sorted({r[0] for r in rows})
    prod_ids = sorted({r[1] for r in rows})
    print(f"   行为 {len(rows)} 条，用户 {len(user_ids)} 个，商品 {len(prod_ids)} 个")

    if len(user_ids) < 2:
        print("   行为数据不足，仅写入热门")
        conn = pymysql.connect(**db_config())
        c = conn.cursor()
        hot = get_hot_products(c)
        conn.close()
        save_results([], [], hot)
        save_eval_log('SKIPPED', '用户数不足 2，仅写入热门', len(user_ids), len(prod_ids),
                      len(rows), 0, {}, (datetime.now() - started).total_seconds())
        print(f"[{datetime.now()}] 完成（仅热门）")
        return

    print("2/6 按时间切分训练集/测试集...")
    train_rows, test_rows = split_by_time(rows, TEST_RATIO)
    print(f"   训练 {len(train_rows)} 条，测试 {len(test_rows)} 条"
          f"（测试集为时间上最近的 {TEST_RATIO:.0%}）")

    print("3/6 离线评估（只用训练集训练）...")
    metrics = evaluate_on_test(train_rows, test_rows, EVAL_K)
    print_metrics(metrics)

    print("4/6 ALS + BPR 在全量数据上重训...")
    matrix, user_ids, prod_ids, user_map, prod_map = build_matrix(rows)
    X, Y = als_train(matrix, FACTORS, ALS_ITERS, REG, ALPHA)
    X, Y = bpr_finetune(X, Y, matrix, BPR_ITERS, BPR_LR, REG)

    print("5/6 生成推荐...")
    recs = generate_user_recommends(X, Y, user_ids, prod_ids)
    sim = generate_similar_products(Y, prod_ids)

    print("6/6 写入数据库...")
    conn = pymysql.connect(**db_config())
    c = conn.cursor()
    hot = get_hot_products(c)
    conn.close()
    save_results(recs, sim, hot)
    save_eval_log('SUCCESS', None, len(user_ids), len(prod_ids), len(rows), len(test_rows),
                  metrics, (datetime.now() - started).total_seconds())

    print(f"[{datetime.now()}] 训练完成！推荐 {len(recs)} 条，相似 {len(sim)} 条")

if __name__ == '__main__':
    try:
        main()
    except Exception as exc:
        print(f"[{datetime.now()}] 训练失败: {exc}")
        try:
            save_eval_log('FAILED', str(exc)[:255], 0, 0, 0, 0, {}, 0.0)
        except Exception as log_exc:
            print(f"评估日志写入失败: {log_exc}")
        raise
