//! Implementation of the default `#[catch_panic]` handler.

use std::any::Any;
use std::panic::{catch_unwind, UnwindSafe};

use jni::JNIEnv;

/// The wrapper method that `#[catch_panic]` invokes to actually catch panics.
/// This is an internal implementation detail - don't call this function directly!
#[doc(hidden)]
#[inline]
pub fn __catch_panic<F, R, H>(env: JNIEnv, default: R, handler: H, f: F) -> R
where
    F: FnOnce() -> R + UnwindSafe,
    H: FnOnce(JNIEnv, Box<dyn Any + Send + 'static>),
{
    match catch_unwind(f) {
        Ok(result) => result,
        Err(err) => {
            handler(env, err);
            default
        }
    }
}

/// `#[catch_panic]`'s default panic handler. This
/// will rethrow all caught panics as java `RuntimeException`s
/// with the message passed to `panic!`.
pub fn default_handler(mut env: JNIEnv, err: Box<dyn Any + Send + 'static>) {
    let msg = match err.downcast_ref::<&'static str>() {
        Some(s) => *s,
        None => match err.downcast_ref::<String>() {
            Some(s) => &s[..],
            None => "Box<dyn Any>",
        },
    };
    env.throw_new("java/lang/RuntimeException", msg).unwrap();
}
