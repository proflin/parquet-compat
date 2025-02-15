package com.databricks.parquet.schema

import scala.collection.JavaConverters._

import com.databricks.parquet.ParquetSuite
import com.databricks.parquet.avro.AvroArrayOfOptionalInts
import com.databricks.parquet.dsl._
import org.apache.avro.generic.GenericRecord

class ListCompatibilitySuite extends ParquetSuite {
  test("PARQUET-364: read a Parquet record containing an Avro \"array<array<int>>\"") {
    withTempHadoopPath { path =>
      val schema =
        """message root {
          |  required group f (LIST) {
          |    repeated group array (LIST) {
          |      repeated int32 array;
          |    }
          |  }
          |}
        """.stripMargin

      write.directly(path, schema) { implicit writer =>
        import write._

        message { implicit consumer =>
          field(0, "f") {
            group {
              field(0, "array") {
                group {
                  field(0, "array") {
                    int(0)
                    int(1)
                  }
                }

                group {
                  field(0, "array") {
                    int(2)
                    int(3)
                  }
                }
              }
            }
          }
        }
      }

      expectException[ClassCastException] {
        withAvroParquetReader[GenericRecord](path) { reader =>
          val expected = Seq(
            Seq(0: Integer, 1: Integer).asJava,
            Seq(2: Integer, 3: Integer).asJava
          ).asJava

          assert(reader.read().get("f") === expected)
        }
      }
    }
  }

  test("PARQUET-364: read a Parquet record containing a Thrift \"list<list<i32>>\"") {
    withTempHadoopPath { path =>
      val schema =
        """message root {
          |  required group f (LIST) {
          |    repeated group f_tuple (LIST) {
          |      repeated int32 f_tuple_tuple;
          |    }
          |  }
          |}
        """.stripMargin

      write.directly(path, schema) { implicit writer =>
        import write._

        message { implicit consumer =>
          field(0, "f") {
            group {
              field(0, "f_tuple") {
                group {
                  field(0, "f_tuple_tuple") {
                    int(0)
                    int(1)
                  }
                }

                group {
                  field(0, "f_tuple_tuple") {
                    int(2)
                    int(3)
                  }
                }
              }
            }
          }
        }
      }

      expectException[ClassCastException] {
        withAvroParquetReader[GenericRecord](path) { reader =>
          val expected = Seq(
            Seq(0: Integer, 1: Integer).asJava,
            Seq(2: Integer, 3: Integer).asJava
          ).asJava

          assert(reader.read().get("f") === expected)
        }
      }
    }
  }

  test("write an Avro array of optional integers") {
    withTempHadoopPath { path =>
      val schema = AvroArrayOfOptionalInts.getClassSchema
      withAvroParquetWriter[AvroArrayOfOptionalInts](path, schema) { writer =>
        val arrayOfOptionalInts = Seq(1: Integer, null, 2: Integer, null).asJava
        expectException[NullPointerException] {
          writer.write(AvroArrayOfOptionalInts.newBuilder().setF(arrayOfOptionalInts).build())
        }
      }
    }
  }
}
