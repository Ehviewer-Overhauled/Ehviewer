mod implementations;

use crate::{AsJValue, JnixEnv};

/// Conversion from a type into its Java representation.
///
/// A type that implements this trait has an equivalent Java representation.
pub trait IntoJava<'borrow, 'env: 'borrow> {
    /// The [JNI signature] of the Java type.
    ///
    /// [JNI signature]: https://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/types.html#wp16432
    const JNI_SIGNATURE: &'static str;

    /// The Rust type that wraps a reference to the Java type.
    type JavaType: AsJValue<'env>;

    /// Performs the conversion.
    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType;

    /// Returns the JNI signature of the Java type.
    ///
    /// This is a helper method to obtain the `JNI_SIGNATURE` from a value without knowing its
    /// exact type name.
    fn jni_signature(&self) -> &'static str {
        Self::JNI_SIGNATURE
    }
}
