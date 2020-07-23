package net.sansa_stack.ml.spark.similarity.similarity_measures

import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, udf}

class BraunBlanquetModel extends GenericSimilarityEstimatorModel {

  protected val braun_blanquet = udf( (a: Vector, b: Vector) => {
    val feature_indices_a = a.toSparse.indices
    val feature_indices_b = b.toSparse.indices
    val f_set_a = feature_indices_a.toSet
    val f_set_b = feature_indices_b.toSet
    val braun_blanquet = (f_set_a.intersect(f_set_b).size.toDouble) / Seq(f_set_a.size.toDouble, f_set_b.size.toDouble).max
    braun_blanquet
  })

  override val estimator_name: String = "BraunBlanquetSimilarityEstimator"
  override val estimator_measure_type: String = "similarity"

  override val similarityEstimation = braun_blanquet

  override def similarityJoin(df_A: DataFrame, df_B: DataFrame, threshold: Double = -1.0, value_column: String = "braun_blanquet_similarity"): DataFrame = {

    val cross_join_df = createCrossJoinDF(df_A: DataFrame, df_B: DataFrame)

    set_similarity_estimation_column_name(value_column)

    val join_df: DataFrame = cross_join_df.withColumn(
      _similarity_estimation_column_name,
      similarityEstimation(col(_features_column_name_dfA), col(_features_column_name_dfB))
    )
    reduce_join_df(join_df, threshold)
  }

  override def nearestNeighbors(df_A: DataFrame, key: Vector, k: Int, key_uri: String = "unknown", value_column: String = "braun_blanquet_similarity", keep_key_uri_column: Boolean = false): DataFrame = {

    set_similarity_estimation_column_name(value_column)

    val nn_setup_df = createNnDF(df_A, key, key_uri)

    val nn_df = nn_setup_df
      .withColumn(
        _similarity_estimation_column_name,
        similarityEstimation(col(_features_column_name_dfB), col(_features_column_name_dfA)))

    reduce_nn_df(nn_df, k, keep_key_uri_column)
  }
}