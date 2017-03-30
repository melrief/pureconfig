### 0.7.0 (undefined)

- New features
  - Add `ConfigFactoryWrapper` to control exceptions from typesafe `ConfigFactory`
  - Modify the message of `ConfigReaderException` to group errors by keys in the configuration, instead of by type of error
  - Add a path (`Option[String]`) to `ConfigReaderFailure`, in order to expose more information (if available) about the key in the configuration whose value raised the failure

- Breaking changes
  - `loadConfigFromFiles` works on `Path` instead of `File` for consistency
  - `ConfigValueLocation` now uses `URL` instead of `Path` to encode locations of `ConfigValue`s

- Bug fixes
  - `pureconfig.load*` methods don't throw exceptions on malformed configuration anymore
     and wrap errors in `ConfigReaderFailures` [[#148](https://github.com/melrief/pureconfig/issues/148)]

### 0.6.0 (Feb 14, 2017)

- New features
  - New  `ProductHint` trait allowing customization of the derived `ConfigConvert` for case classes, superseeding
    `ConfigFieldMapping` ([docs](https://github.com/melrief/pureconfig#override-behaviour-for-case-classes)). In
    addition to defining field name mappings, `ProductHint` instances control:
    - Whether default field values should be used when
      fields are missing in the config ([docs](https://github.com/melrief/pureconfig#default-field-values));
    - Whether unknown keys are ignored or cause pureconfig to return a `Failure`
      ([docs](https://github.com/melrief/pureconfig#unknown-keys)).
  - Support for reading and writing [`java.util.UUID`](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html)s;
  - Support for reading and writing [`java.nio.file.Path`](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html)s;
  - Support for reading and writing [`java.net.URI`](https://docs.oracle.com/javase/8/docs/api/java/net/URI.html)s;
  - Support multiple failures, e.g. when multiple fields of a class fail to convert;
  - Add `ConfigReaderFailure` ADT to model failures and `ConfigReaderFailures` to represent a non empty list of errors;
  - Add `ConfigValueLocation`, which is the physical location of a ConfigValue represented by a file name and a line number;
  - Add `loadConfigOrThrow` methods to the API;
  - Add helpers to create `ConfigConvert`:
    - `ConfigConvert.fromStringConvert` that requires a function `String => Either[ConfigReaderFailure, T]`
    - `ConfigConvert.fromStringConvertTry` that requires a function `String => Try[T]`
    - `ConfigConvert.fromStringConvertOpt` that requires a function `String => Option[T]`
  - Add `ConfigConvert.catchReadError` to convert a function that can throw exception into a safe function that returns
    a `Either[CannotConvert, T]`;

- Breaking changes
  - `ConfigConvert.from` now returns a value of type `Either[ConfigReaderFailures, T]` instead of `Try[T]`;
  - `CoproductHint` has been changed to adapt to the new `ConfigConvert`:
    - `CoproductHint.from` now returns a value of type `Either[ConfigReaderFailures, Option[ConfigValue]]`
    - `CoproductHint.to` now returns a value of type `Either[ConfigReaderFailures, Option[ConfigValue]]`
  - The default field mapping changed from camel case config keys (e.g. `exampleKey`) to kebab case keys (e.g.
    `example-key`). Case class fields are still expected to be camel case. The old behavior can be retained by putting
    in scope an `implicit def productHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))`;
  - `ConfigFieldMapping` has no type parameters now;
  - `ConfigFieldMapping` was replaced by `ProductHint` as the type of object to put in scope in order to customize
    the derivation of `ConfigConvert` for case class. Old `ConfigFieldMapping` implicit instances in scope have no
    effect now. The migration can be done by replacing code like
    `implicit def mapping: ConfigFieldMapping[T] = <mapping>` with
    `implicit def productHint: ProductHint[T] = ProductHint(<mapping>)`;
  - `ConfigConvert.fromString`, `ConfigConvert.fromNonEmptyString`, `ConfigConvert.vstringConvert`,
    `ConfigConvert.nonEmptyStringConvert` are now deprecated and the new helpers should be used instead.

### 0.5.1 (Jan 20, 2017)

- New features
  - More consistent handling of missing keys: if a config key is missing pureconfig always throws a
    `KeyNotFoundException` now, unless the `ConfigConvert` extends the new `AllowMissingKey` trait.
  - Add support for the `java.time` package. Converters types which support different string formats, such as `LocalDate`,
    must be configured before they can be used. See the [README](https://github.com/melrief/pureconfig#configurable-converters)
    for more details.
  - Add support for converting objects with numeric keys into lists. This is a functionallity also supported
    by typesafe config since version [1.0.1](https://github.com/typesafehub/config/blob/f6680a5dad51d992139d45a84fad734f1778bf50/NEWS.md#101-may-19-2013)
    and discussed in the following [issue](https://github.com/typesafehub/config/issues/69).

### 0.5.0 (Jan 3, 2017)

- New features
  - Sealed families are now converted to and from configs unambiguously by using an extra `type` field (customizable) in
    their config representation; 
  - New `CoproductHint` trait which allows customization of the derived `ConfigConvert` for sealed families;
- Breaking changes
  - The default config representation for sealed families has changed:
    - By default pureconfig now expects to find a `type` field containing the lowercase simple class name of the type to
      be read. For example, for a family including `DogConf` and `CatConf`, pureconfig expects to find a
      `type: "dogconf"` field in the config file;
    - The old behavior can be restored by putting an implicit instance of `FirstSuccessCoproductHint` in scope (the
      migration to the new format is strongly recommended though, as the previous one may lead to ambiguous behavior);
    - More information about the default representation and on how to customize it can be seen in the
      [README](https://github.com/melrief/pureconfig#override-behaviour-for-sealed-families).
- Bug fixes
  - `0` is accepted again as a valid `Duration` in configs.
