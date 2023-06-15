//! JNI-compatible method signature generator for Rust libraries.
//!
//! This crate was designed for use with the [`jni`](https://crates.io/crates/jni) crate, which
//! exposes JNI-compatible type bindings. Although it's possible to use `jni` without `jni_fn`, the
//! procedural macro defined here will make it easier to write the method signatures correctly.
//!
//! See the `jni_fn` attribute macro documentation below for more info and usage examples.

#![deny(missing_docs)]
#![deny(unsafe_code)]

use proc_macro2::TokenStream;
use quote::ToTokens;
use syn::spanned::Spanned;

/// Annotate a function with this procedural macro attribute to expose it over the JNI.
///
/// This attribute takes a single string literal as an argument, specifying the package namespace
/// this function should be placed under.
///
/// ```
/// use jni::{ JNIEnv, objects::{ JClass, JString }, sys::jstring };
/// use jni_fn::jni_fn;
///
/// #[jni_fn("com.example.RustBindings")]
/// pub fn sayHello(env: JNIEnv, _: JClass, name: JString) -> jstring {
///     let name_javastr = env.get_string(name).unwrap();
///     let name = name_javastr.to_str().unwrap();
///
///     env.new_string(format!("Hello, {}!", name))
///         .expect("Couldn't create java string!")
///         .into_inner()
/// }
/// ```
///
/// The `sayHello` function will automatically be expanded to have the correct ABI specification
/// and the appropriate JNI-compatible name, i.e. in this case -
/// `Java_com_example_RustBindings_sayHello`.
///
/// Then it can be accessed by, for example, Kotlin code as follows:
/// ```kotlin
/// package com.example.RustBindings
///
/// class RustBindings {
///     private external fun sayHello(name: String): String
///
///     fun greetWorld() {
///         println(sayHello("world"))
///     }
/// }
/// ```
#[proc_macro_attribute]
pub fn jni_fn(
    attr: proc_macro::TokenStream,
    item: proc_macro::TokenStream,
) -> proc_macro::TokenStream {
    jni_fn2(attr.into(), item.into()).into()
}

/// Deals exclusively with `proc_macro2::TokenStream` instead of `proc_macro::TokenStream`,
/// allowing it and all interior functionality to be unit tested.
fn jni_fn2(attr: TokenStream, item: TokenStream) -> TokenStream {
    let attr_span = attr.span();
    let item_span = item.span();

    let mut function: syn::ItemFn = match syn::parse2(item) {
        Ok(f) => f,
        Err(_e) => {
            return syn::Error::new(
                item_span,
                "The `jni_fn` attribute can only be applied to `fn` items",
            )
            .to_compile_error()
        }
    };

    let namespace = match syn::parse2::<syn::LitStr>(attr) {
        Ok(n) => n,
        Err(_e) => return syn::Error::new(attr_span, "The `jni_fn` attribute must have a single string literal supplied to specify the namespace").to_compile_error(),
    }.value();

    if !valid_namespace(&namespace) {
        return syn::Error::new(
            attr_span,
            "Invalid package namespace supplied to `jni_fn` attribute",
        )
        .to_compile_error();
    }

    let orig_fn_name = function.sig.ident.to_string();

    function.sig.ident = syn::Ident::new(
        &create_jni_fn_name(&namespace, &orig_fn_name),
        function.sig.ident.span(),
    );

    function.attrs.push(syn::Attribute {
        pound_token: Default::default(),
        style: syn::AttrStyle::Outer,
        bracket_token: Default::default(),
        path: syn::parse_str("no_mangle").unwrap(),
        tokens: TokenStream::new(),
    });
    function.attrs.push(syn::Attribute {
        pound_token: Default::default(),
        style: syn::AttrStyle::Outer,
        bracket_token: Default::default(),
        path: syn::parse_str("allow").unwrap(),
        tokens: quote::quote! { (non_snake_case) },
    });

    if function.sig.abi.is_some() {
        return syn::Error::new(function.sig.abi.span(), "Don't specify an ABI for `jni_fn` attributed functions - the correct ABI will be added automatically").to_compile_error();
    }
    function.sig.abi = Some(syn::Abi {
        extern_token: Default::default(),
        name: Some(syn::LitStr::new("system", function.sig.ident.span())),
    });

    if !matches!(function.vis, syn::Visibility::Public(_)) {
        return syn::Error::new(
            function.vis.span(),
            "`jni_fn` attributed functions must have public visibility (`pub`)",
        )
        .to_compile_error();
    }

    function.into_token_stream()
}

/// Ensures that `namespace` appears roughly like a valid package name.
///
/// A package name is a '.'-separated identifier list.
///
/// Identifiers are described in section 3.8 of the Java language specification, although some
/// JVM-compatible languages have slightly different restrictions on what is considered a valid
/// identifier. This function attempts to catch obviously incorrect strings.
///
/// Please submit an issue report or patch to make this more permissive if it's required for
/// valid JVM code! Otherwise, making it more restrictive is appreciated as long as it's confirmed
/// to work with multiple JVM-compatible languages.
fn valid_namespace(namespace: &str) -> bool {
    /// These shouldn't occur _anywhere_ in the package name.
    const FORBIDDEN_CHARS: &[char] = &[
        ' ', ',', ':', ';', '|', '\\', '/', '!', '@', '#', '%', '^', '&', '*', '(', ')', '{', '}',
        '[', ']', '-', '`', '~', '\t', '\n', '\r',
    ];

    for c in FORBIDDEN_CHARS {
        if namespace.contains(*c) {
            return false;
        }
    }

    fn is_valid_ident(ident: &str) -> bool {
        /// These shouldn't occur as the first character of an identifier.
        const FORBIDDEN_START_CHARS: &[char] = &['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];

        if ident.is_empty() {
            return false;
        }

        for c in FORBIDDEN_START_CHARS {
            if ident.starts_with(*c) {
                return false;
            }
        }

        true
    }

    for ident in namespace.split('.') {
        if !is_valid_ident(ident) {
            return false;
        }
    }

    true
}

/// Creates a JNI-compatible function name from the given namespace and function name.
/// This does _not_ transform the provided function name into `snakeCase` if it's not already; but
/// `#[allow(non_snake_case)]` should be added to prevent errors.
///
/// Importantly, any underscores in the original namespace or function name need to be replaced by
/// "_1", and then dot separators need to be turned into underscores.
fn create_jni_fn_name(namespace: &str, fn_name: &str) -> String {
    let namespace_underscored = namespace.replace('_', "_1").replace('.', "_");
    let fn_name_underscored = fn_name.replace('_', "_1");
    format!("Java_{}_{}", namespace_underscored, fn_name_underscored)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_create_jni_fn_name() {
        assert_eq!(
            create_jni_fn_name("com.example.Foo", "init"),
            "Java_com_example_Foo_init"
        );
        assert_eq!(
            create_jni_fn_name("com.example.Bar", "closeIt"),
            "Java_com_example_Bar_closeIt"
        );
        assert_eq!(
            create_jni_fn_name("com.example.Bar", "close_it"),
            "Java_com_example_Bar_close_1it"
        );
        assert_eq!(
            create_jni_fn_name(
                "org.signal.client.internal.Native",
                "IdentityKeyPair_Deserialize"
            ),
            "Java_org_signal_client_internal_Native_IdentityKeyPair_1Deserialize"
        );
    }

    #[test]
    fn test_valid_namespace() {
        assert!(valid_namespace("com.example.Foo"));
        assert!(valid_namespace("com.antonok.kb"));
        assert!(valid_namespace("org.signal.client.internal.Native"));
        assert!(valid_namespace("net.under_score"));
        assert!(!valid_namespace("com example Foo"));
        assert!(!valid_namespace(" com.example.Foo"));
        assert!(!valid_namespace("com.example.Foo "));
        assert!(!valid_namespace("com.example.1Foo"));
    }

    #[test]
    fn test_code_generation() {
        let attr = quote::quote! {
            "com.example.Bar"
        };
        let source = quote::quote! {
            pub fn close_it(env: JNIEnv, _: JClass, filename: JString) -> jboolean {
                unimplemented!()
            }
        };

        let expanded = jni_fn2(attr.into(), source.into());

        assert_eq!(
            format!("{}", expanded),
            format!(
                "{}",
                quote::quote! {
                    #[no_mangle]
                    #[allow(non_snake_case)]
                    pub extern "system" fn Java_com_example_Bar_close_1it (env: JNIEnv, _: JClass, filename: JString) -> jboolean {
                        unimplemented!()
                    }
                }
            )
        );
    }

    #[test]
    fn test_unsafe_fn() {
        let attr = quote::quote! {
            "com.example.Bar"
        };
        let source = quote::quote! {
            pub unsafe fn close_it(env: JNIEnv, _: JClass, filename: JString) -> jboolean {
                unimplemented!()
            }
        };

        let expanded = jni_fn2(attr.into(), source.into());

        assert_eq!(
            format!("{}", expanded),
            format!(
                "{}",
                quote::quote! {
                    #[no_mangle]
                    #[allow(non_snake_case)]
                    pub unsafe extern "system" fn Java_com_example_Bar_close_1it (env: JNIEnv, _: JClass, filename: JString) -> jboolean {
                        unimplemented!()
                    }
                }
            )
        );
    }

    #[test]
    fn test_non_function() {
        let attr = quote::quote! { "com.example.Foo" };
        let source = quote::quote! {
            enum NotAFunction {
                Variant1,
                Variant2(u8),
            }
        };

        let expanded = jni_fn2(attr.into(), source.into());

        assert_eq!(
            format!("{}", expanded),
            format!(
                "{}",
                quote::quote! {
                    compile_error! { "The `jni_fn` attribute can only be applied to `fn` items" }
                }
            )
        );
    }

    #[test]
    fn test_empty_attribute() {
        let attr = quote::quote! {};
        let source = quote::quote! {
            pub fn close_it(env: JNIEnv, _: JClass, filename: JString) -> jboolean {
                unimplemented!()
            }
        };

        let expanded = jni_fn2(attr.into(), source.into());

        assert_eq!(
            format!("{}", expanded),
            format!(
                "{}",
                quote::quote! {
                    compile_error! { "The `jni_fn` attribute must have a single string literal supplied to specify the namespace" }
                }
            )
        );
    }

    #[test]
    fn test_invalid_namespace() {
        let attr = quote::quote! { "." };
        let source = quote::quote! {
            pub fn close_it(env: JNIEnv, _: JClass, filename: JString) -> jboolean {
                unimplemented!()
            }
        };

        let expanded = jni_fn2(attr.into(), source.into());

        assert_eq!(
            format!("{}", expanded),
            format!(
                "{}",
                quote::quote! {
                    compile_error! { "Invalid package namespace supplied to `jni_fn` attribute" }
                }
            )
        );
    }

    #[test]
    fn test_specified_abi() {
        let attr = quote::quote! { "com.example.Foo" };
        let source = quote::quote! {
            pub extern "C" fn close_it(env: JNIEnv, _: JClass, filename: JString) -> jboolean {
                unimplemented!()
            }
        };

        let expanded = jni_fn2(attr.into(), source.into());

        assert_eq!(
            format!("{}", expanded),
            format!(
                "{}",
                quote::quote! {
                    compile_error! { "Don't specify an ABI for `jni_fn` attributed functions - the correct ABI will be added automatically" }
                }
            )
        );
    }

    #[test]
    fn test_wrong_visibility() {
        let attr = quote::quote! { "com.example.Foo" };
        let source = quote::quote! {
            fn close_it(env: JNIEnv, _: JClass, filename: JString) -> jboolean {
                unimplemented!()
            }
        };

        let expanded = jni_fn2(attr.into(), source.into());

        assert_eq!(
            format!("{}", expanded),
            format!(
                "{}",
                quote::quote! {
                    compile_error! { "`jni_fn` attributed functions must have public visibility (`pub`)" }
                }
            )
        );
    }
}
