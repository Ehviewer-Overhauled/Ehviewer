mod implementations;

use crate::JnixEnv;

/// Conversion from a Java type into its Rust equivalent.
///
/// A type that implements this trait has an equivalent Java representation.
pub trait FromJava<'env, JavaType> {
    /// The [JNI signature] of the Java type.
    ///
    /// [JNI signature]: https://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/types.html#wp16432
    const JNI_SIGNATURE: &'static str;

    /// Performs the conversion.
    fn from_java(env: &JnixEnv<'env>, source: JavaType) -> Self;
}
