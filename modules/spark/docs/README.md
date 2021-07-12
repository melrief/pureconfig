# Spark module for PureConfig

Adds support for selected [Spark](http://spark.apache.org/) classes to PureConfig.

## Add pureconfig-spark to your project

In addition to [core PureConfig](https://github.com/pureconfig/pureconfig), you'll need:

```scala
libraryDependencies += "com.github.pureconfig" %% "pureconfig-spark" % "@VERSION@"
```

Also, `pureconfig-spark` depends on `spark-sql` with `provided` scope.
Spark libraries are generally added on runtime.
This module has been tested on Spark 3 but it should also work for Spark 2.4 since basic datatype APIs should stay the same.
Please note that we are only supporting Scala 2.12 for all Spark versions.

To use the Spark module you need to import:
```scala
import pureconfig.module.spark._
```

## Supported classes

* `org.apache.spark.sql.types.DataType`
* `org.apache.spark.sql.types.StructType`
* `org.apache.spark.sql.types.Metadata`
* `org.apache.spark.sql.types.StructField` (derivable)

## Example

### Custom HOCON schema to/from Spark schema
Setup custom schema case classes and converters between custom schema and Spark schema.
```scala mdoc:silent
import org.apache.spark.sql.types._
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.spark._

case class MySchema(name: String, fields: List[StructField], someOtherSetting: Option[String])

def mySchemaToSparkSchema(schema: MySchema): StructType =
  StructType(schema.fields)

def sparkSchemaToMySchema(name: String, schema: StructType): MySchema =
  MySchema(name, schema.fields.toList, None)
```

Convert custom schema to Spark and back to custom schema. Resultant string schema should match original source.
```scala mdoc
val mySchemaRes = ConfigSource.string(
  """name: Employee,
    |fields: [
    |  { name: name, data-type: string }, #types are case-insensitive and some types have variations/truncations
    |  { name: age, data-type: integer }, #also note that `nullable` and `metadata` are optional fields with Spark defaults
    |  { name: salary, data-type: "decimal(6,2)" },
    |  { name: address, data-type: "line1 string, line2 string" } #outer `struct` is optional
    |]
    |""".stripMargin).load[MySchema]

val sparkSchemaRes = mySchemaRes.map(mySchemaToSparkSchema)

val mySchemaRes2 =
  for {
    mySchema <- mySchemaRes
    sparkSchema <- sparkSchemaRes
  } yield sparkSchemaToMySchema(mySchema.name, sparkSchema)

val stringSchemaRes = mySchemaRes2.map(ConfigWriter[MySchema].to)
```

### Full schema encoded as HOCON String field to/from Spark schema
You can also read Spark schemas directly as `StructType` instead of narrowing `DataType` yourself.
```scala mdoc
case class Config(schema: StructType)
val configRes = ConfigSource.string(
  """
    |schema = "a int, b string, c struct<c1:int,c2:double>"
    |""".stripMargin).load[Config]
    
val stringSchemaRes2 = configRes.map(ConfigWriter[Config].to)
```
