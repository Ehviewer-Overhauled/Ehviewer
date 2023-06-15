//! # #\[catch_panic\]
//!
//! A helper macro for safe Java-Rust interop that "catches" Rust panics
//! and rethrows them as Java exceptions.
//!
//! ## Usage
//!
//! Attach `#[catch_panic]` to a JNI callback to have panics converted into `RuntimeException`s:
//!
//! ```
//! # use jni::JNIEnv;
//! # use catch_panic::catch_panic;
//! #
//! #[no_mangle]
//! #[catch_panic]
//! pub extern "C" fn Java_com_example_Example_panic(_env: JNIEnv) {
//!     panic!("everything is not fine");
//! }
//!
//! # catch_panic::test::check_callback(|env| Java_com_example_Example_panic(env), true);
//! ```
//!
//! Since `#[catch_panic]` internally relies on Rust's [catch_unwind] function,
//! you **must** build your library with [`panic = "unwind"`] for panics to be caught.
//!
//! [catch_unwind]: std::panic::catch_unwind
//! [`panic = "unwind"`]: https://doc.rust-lang.org/cargo/reference/profiles.html#panic
//!
//! ### Dummy return values
//!
//! Even if an exception is thrown, the native callback must return some value to the JVM.
//! By default, `#[catch_panic]` will attempt to use the [Default][std::default::Default]
//! value of a type as the dummy value to return. This however does not work for `jobject`s
//! and related types, so an explicit default must be specified:
//!
//! ```
//! # use jni::JNIEnv;
//! # use jni::sys::jobject;
//! # use catch_panic::catch_panic;
//! #
//! #[no_mangle]
//! #[catch_panic(default = "std::ptr::null_mut()")]
//! pub extern "C" fn Java_com_example_Example_gimmeAnObject(mut env: JNIEnv) -> jobject {
//!     env.alloc_object("java/lang/Object").unwrap().into_inner()
//! }
//!
//! # catch_panic::test::check_callback(|env| Java_com_example_Example_gimmeAnObject(env), false);
//! ```
//!
//! Any valid expression can be used for the default value.
//!
//! ### Custom panic payload handlers
//!
//! For throwing custom exception types or processing the panic payload,
//! you can use your own panic payload handler:
//!
//! ```
//! # use std::any::Any;
//! # use jni::JNIEnv;
//! # use catch_panic::catch_panic;
//! #
//! pub fn enterprise_certified_handler(mut env: JNIEnv, err: Box<dyn Any + Send + 'static>) {
//!     let msg = match err.downcast_ref::<&'static str>() {
//!         Some(s) => *s,
//!         None => match err.downcast_ref::<String>() {
//!             Some(s) => &s[..],
//!             None => "this is a certified `std::panic::panic_any` moment",
//!         },
//!     };
//!     env.throw_new("com/example/ExampleEnterpriseException", msg).unwrap();
//! }
//!
//! #[no_mangle]
//! #[catch_panic(handler = "enterprise_certified_handler")]
//! pub extern "C" fn Java_com_example_Example_makeFactoryNoises(env: JNIEnv) {
//!     panic!("<insert factory noises>");
//! }
//!
//! # catch_panic::test::check_callback_with_setup(
//! #     |env| {
//! #         let src = std::fs::read("testlib/ExampleEnterpriseException.class")
//! #             .expect("ExampleEnterpriseException test class file missing!");
//! #
//! #         let object_class = env.find_class("java/lang/Object").unwrap();
//! #         let object_class_loader = env
//! #             .call_method(object_class, "getClassLoader", "()Ljava/lang/ClassLoader;", &[])
//! #             .unwrap();
//! #
//! #         env.define_class(
//! #             "com/example/ExampleEnterpriseException",
//! #             object_class_loader.try_into().unwrap(),
//! #             &src,
//! #         )
//! #         .unwrap();
//! #     },
//! #     |env| Java_com_example_Example_makeFactoryNoises(env),
//! #     true,
//! # );
//! ```

pub mod handler;
pub use catch_panic_macros::catch_panic;
