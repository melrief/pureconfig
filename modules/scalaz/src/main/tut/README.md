# Scalaz module for PureConfig

Adds support for selected [scalaz](https://github.com/scalaz/scalaz) data structures to PureConfig, provides instances of
`scalaz` type classes for `ConfigReader`, `ConfigReaderFailures`, `ConfigWriter` and `ConfigConvert` and some syntactic sugar for pureconfig
classes.

## Add pureconfig-scalaz to your project

In addition to [core pureconfig](https://github.com/pureconfig/pureconfig), you'll need:

```scala
libraryDependencies += "com.github.pureconfig" %% "pureconfig-scalaz" % "0.11.1"
```

## Example

### Reading `scalaz` data structures from a config

The following `scalaz` data structures are supported:

* `DList`, `Dequeue`, `IList`, `ISet`, `Heap`, `Maybe`, `NonEmptyList` and `==>>`
* `Order[A]` should also be in scope, when you're relying on either `ConfigReader[ISet[A]]`, `ConfigReader[Heap[A]]` or `ConfigReader[A ==>> B]`.
For example, if your `ISet` instance contains `String` values then `Order[String]` can be imported via `scalaz.std.string._`

Here is an usage example:

```tut:silent
import com.typesafe.config.ConfigFactory.parseString
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.scalaz._
import scalaz.{ ==>>, DList, Dequeue, IList, ISet, Heap, Maybe, NonEmptyList }
import scalaz.std.anyVal.intInstance
import scalaz.std.string._

case class ScalazConfig(
  numberDlist: DList[Int],
  numberDequeue: Dequeue[Int],
  numberIlist: IList[Int],
  numberHeap: Heap[Int],
  numberSet: ISet[Int],
  numberMaybe: Maybe[Int],
  numberNel: NonEmptyList[Int],
  numberMap: String ==>> Int
)
```

We can read a `ScalazConfig` like:
```tut:book
val scalazConf = parseString("""{
  number-dlist: [1,2,3],
  number-dequeue: [1,2,3],
  number-ilist: [1,2,3],
  number-heap: [1,2,3],
  number-maybe: 1,
  number-set: [1,2,3],
  number-nel: [1,2,3],
  number-map { "one": 1, "two": 2, "three": 3 }
}""")

loadConfig[ScalazConfig](scalazConf)
```

### Using `scalaz` type class instances for readers

In order to put in scope `scalaz` type classes for our readers and extend them with the extra
operations provided by `scalaz`, we need some extra imports:

```tut:silent
import pureconfig.module.scalaz.instances._
import scalaz._
import scalaz.Scalaz._
```

We are now ready to use the new syntax:

```tut:silent
case class SimpleConfig(i: Int)

// a reader that always returns SimpleConfig(42)
val constReader = SimpleConfig(42).point[ConfigReader]

// a reader that returns SimpleConfig(-1) if an error occurs
val safeReader = ConfigReader[SimpleConfig].handleError(_ => SimpleConfig(-1).point[ConfigReader])
```

And we can finally put them to use:

```tut:book
val validConf = parseString("""{ i: 1 }""")

val invalidConf = parseString("""{ s: "abc" }""")

constReader.from(validConf.root())

constReader.from(invalidConf.root())

safeReader.from(validConf.root())

safeReader.from(invalidConf.root())
```

In case there's a necessity to parse multiple configs and accumulate errors, you could leverage from `Semigroup` instance for `ConfigReaderFailures`:

```tut:book
val anotherInvalidConf = parseString("""{ i: false }""")

List(validConf, invalidConf, anotherInvalidConf).traverseU { c =>
  Validation.fromEither(implicitly[ConfigReader[SimpleConfig]].from(c.root))
}
```

### Extra syntactic sugar

We can provide some useful extension methods by importing:

```tut:silent
import pureconfig.module.scalaz.syntax._
```

For example, you can easily convert a `ConfigReaderFailures` to a `NonEmptyList[ConfigReaderFailure]`:

```tut:silent
case class MyConfig(i: Int, s: String)
```
```tut:book
val myConf = parseString("{}")

val res = loadConfig[MyConfig](myConf).left.map(_.toNel)
```

This allows `scalaz` users to easily convert a result of a `ConfigReader` into a `ValidationNel`:

```tut:silent
import scalaz.{ Validation, ValidationNel }
import pureconfig.error._
```

```tut:book
val result: ValidationNel[ConfigReaderFailure, MyConfig] =
  Validation.fromEither(res)
```

Also, you could create `ConfigReader`s using `scalaz` types:

```tut:silent
case class Tweet(msg: String)

val tweetReader: ConfigReader[Tweet] = ConfigReader.fromNonEmptyStringDisjunction { s =>
  if (s.length <= 140) Tweet(s).right
  else (new FailureReason { def description: String = "Too long to be a tweet!" }).left
}
```
